import os
import re
import time
import json
import wave
import queue
import audioop
import tempfile
import threading
import winsound

import ollama
import pyaudio
import arabic_reshaper
from bidi.algorithm import get_display
from vosk import Model, KaldiRecognizer

try:
    from piper import PiperVoice
    PIPER_AVAILABLE = True
except ImportError:
    PIPER_AVAILABLE = False
    print("[تحذير] مكتبة piper-tts غير مثبتة. نفّذ: pip install piper-tts", flush=True)

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.widget import Widget
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.clock import Clock
from kivy.graphics import Color, Ellipse, Line
from kivy.properties import NumericProperty, ListProperty, BooleanProperty
from kivy.core.text import LabelBase


# ============== الإعدادات ==============
LabelBase.register(name='ArabicFont', fn_regular=r'C:\waleef-app\Arial.ttf')

MODEL_PATH = "model"
PIPER_VOICE_PATH = r"voices\ar_JL-kareem-medium.onnx"
OLLAMA_MODEL = "gemma4:e2b"
MAX_RECORDING_SECONDS = 30
SAMPLE_RATE = 16000

# تكبير صوت الميكروفون (1.0 = طبيعي، 2.0 = ضعفين، 3.0 = ثلاثة أضعاف)
# ارفعه لو وليف ما يسمعك جيدًا، أو نزّله لو يلتقط ضوضاء.
MIC_GAIN = 2.5

# عدد الرسائل اللي يحتفظ بها وليف من المحادثة (ذاكرة قصيرة المدى)
HISTORY_LIMIT = 8

# شخصية وليف - عدّل النص هذا لتغيير أسلوبه
SYSTEM_PROMPT = """أنت "وليف"، مساعد شخصي ذكي عربي ودود ومرح بشخصية مميزة.

قواعدك في الرد:
- رد دائماً بالعربية الفصحى المبسطة (ليس عامية، وليس فصحى ثقيلة).
- اعطِ إجابات مباشرة ومفيدة فوراً، ولا تطلب توضيحات إذا كان السؤال واضحاً.
- إذا طلب المستخدم فكرة، اعطه فكرة محددة فعلاً، لا تطلب منه أن يحدد.
- إذا طلب اقتراحاً تسويقياً، اعطه اقتراحاً ملموساً مع تفاصيل قصيرة.
- إذا سألك "كيف حالك"، رد رد طبيعي قصير، ثم اسأله سؤالاً واحداً مفيداً.
- اجعل ردودك قصيرة (2-4 جمل عادة)، فالمستخدم يسمع ردك بصوت لا يقرأه.
- لا تستخدم رموز markdown أو نقاط أو علامات تنسيق، الرد سيُقرأ بصوت عالٍ.
- كن مبدعاً ولا تكرر نفس الأسلوب في كل رد.
- لا تذكر أنك ذكاء اصطناعي إلا إذا سُئلت مباشرة."""


# ============== أصوات تفاعلية ==============
def _safe_beep(freq, dur):
    try:
        winsound.Beep(freq, dur)
    except Exception:
        pass

def play_start_sound(): _safe_beep(1000, 150)
def play_stop_sound():  _safe_beep(700, 150)
def play_think_sound():
    _safe_beep(1300, 80)
    _safe_beep(1600, 80)


# ============== أدوات النص ==============
def fix_arabic(text):
    if not text:
        return ""
    return get_display(arabic_reshaper.reshape(text))


_SENTENCE_BOUNDARY = re.compile(r'(?<=[\.!\?؟\n])\s+')

def split_into_sentences(text):
    """تقسيم النص لجمل لنطقها متتابعة."""
    if not text:
        return []
    parts = _SENTENCE_BOUNDARY.split(text)
    return [p.strip() for p in parts if p.strip()]


def extract_chunk_content(chunk):
    """استخراج المحتوى من chunk بغض النظر عن نسخة ollama-python.
    النسخ الجديدة (>=0.4) ترجع Pydantic ChatResponse، القديمة dict."""
    try:
        if isinstance(chunk, dict):
            msg = chunk.get('message') or {}
            return msg.get('content', '') or ''
        # Pydantic أو أي object فيه message.content
        msg = getattr(chunk, 'message', None)
        if msg is None:
            return ''
        return getattr(msg, 'content', '') or ''
    except Exception:
        return ''


# ============== عيون النيون ==============
class ProceduralNeonEyes(Widget):
    blink_state = NumericProperty(1)
    eye_color = ListProperty([0, 0.8, 1, 1])

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.bind(pos=self.update_canvas, size=self.update_canvas,
                  blink_state=self.update_canvas, eye_color=self.update_canvas)
        Clock.schedule_interval(self.blink_animation, 4)

    def update_canvas(self, *args):
        self.canvas.clear()
        with self.canvas:
            Color(*self.eye_color, mode='rgba')
            eye_size = self.width * 0.15
            center_y = self.center_y - 60
            left_x = self.center_x - (self.width * 0.25)
            right_x = self.center_x + (self.width * 0.05)
            Ellipse(pos=(left_x, center_y), size=(eye_size, eye_size * self.blink_state))
            Ellipse(pos=(right_x, center_y), size=(eye_size, eye_size * self.blink_state))
            Color(0, 0.6, 1, 0.2)
            Line(circle=(left_x + eye_size / 2, center_y + (eye_size * self.blink_state) / 2, eye_size / 1.4), width=1.5)
            Line(circle=(right_x + eye_size / 2, center_y + (eye_size * self.blink_state) / 2, eye_size / 1.4), width=1.5)

    def blink_animation(self, dt):
        Clock.schedule_once(lambda d: setattr(self, 'blink_state', 0.1), 0.1)
        Clock.schedule_once(lambda d: setattr(self, 'blink_state', 1.0), 0.2)


# ============== التطبيق ==============
class WaleefApp(App):
    is_listening = BooleanProperty(False)

    def build(self):
        self.title = "وليف - نظام التفاعل الكامل"
        layout = BoxLayout(orientation='vertical', padding=[40, 100, 40, 40], spacing=25)

        self.eyes = ProceduralNeonEyes(size_hint=(1, 0.5))
        layout.add_widget(self.eyes)

        self.status_label = Label(
            text=fix_arabic("جاري التحضير.."),
            font_name='ArabicFont', font_size='24sp', size_hint=(1, 0.2)
        )
        layout.add_widget(self.status_label)

        self.mic_btn = Button(
            text=fix_arabic("ابدأ الكلام"),
            font_name='ArabicFont', size_hint=(0.4, 0.15),
            pos_hint={'center_x': 0.5}, background_color=(0, 0.7, 1, 1),
            disabled=True
        )
        self.mic_btn.bind(on_press=self.handle_button)
        layout.add_widget(self.mic_btn)

        # حالة داخلية
        self.vosk_model = None
        self.piper_voice = None
        self.tts_queue = queue.Queue()
        self.tts_stop_flag = threading.Event()
        # ذاكرة المحادثة - يحتفظ بآخر HISTORY_LIMIT رسالة
        self.conversation_history = []

        # عامل النطق (Worker واحد فقط - يمنع تداخل الأصوات)
        threading.Thread(target=self._tts_worker, daemon=True).start()

        # تحميل الموديلات + إحماء Ollama في الخلفية
        threading.Thread(target=self._load_models, daemon=True).start()

        return layout

    # ---------- تحميل الموديلات ----------
    def _load_models(self):
        # Vosk
        if os.path.exists(MODEL_PATH):
            try:
                self.vosk_model = Model(MODEL_PATH)
                print(">>> Vosk جاهز", flush=True)
            except Exception as e:
                print(f"[خطأ Vosk] {e}", flush=True)
        else:
            print(f"[خطأ] مجلد موديل Vosk مفقود: {MODEL_PATH}", flush=True)

        # Piper
        if PIPER_AVAILABLE and os.path.exists(PIPER_VOICE_PATH):
            try:
                self.piper_voice = PiperVoice.load(PIPER_VOICE_PATH)
                print(">>> Piper جاهز", flush=True)
            except Exception as e:
                print(f"[خطأ Piper] {e}", flush=True)
        else:
            print(f"[تحذير] موديل Piper غير موجود في: {PIPER_VOICE_PATH}", flush=True)

        # إحماء Ollama (تحميل الموديل في الذاكرة) عشان أول رد ما يكون بطيء
        try:
            ollama.chat(
                model=OLLAMA_MODEL,
                messages=[{'role': 'user', 'content': 'مرحبا'}],
                options={'num_predict': 1},
            )
            print(">>> Ollama جاهز", flush=True)
        except Exception as e:
            print(f"[تحذير Ollama] {e}", flush=True)

        # تحضير الواجهة
        Clock.schedule_once(lambda dt: self._on_models_ready())

    def _on_models_ready(self):
        if self.vosk_model:
            self.status_label.text = fix_arabic("جاهز للعمل.. اضغط للتحدث")
            self.mic_btn.disabled = False
        else:
            self.status_label.text = fix_arabic("خطأ: موديل Vosk غير موجود!")

    # ---------- الزر ----------
    def handle_button(self, instance):
        if not self.is_listening:
            self.start_voice_interaction()
        else:
            self.stop_voice_interaction()

    def start_voice_interaction(self):
        if not self.vosk_model:
            self.status_label.text = fix_arabic("خطأ: موديل Vosk غير محمّل!")
            return
        # إيقاف أي نطق جاري
        self._clear_tts_queue()
        self.is_listening = True
        play_start_sound()
        self.mic_btn.text = fix_arabic("إيقاف ومعالجة")
        self.mic_btn.background_color = (1, 0.2, 0.2, 1)
        self.status_label.text = fix_arabic("أنا أسمعك الآن..")
        print(">>> بدء التسجيل الصوتي", flush=True)
        threading.Thread(target=self.recording_thread, daemon=True).start()

    def stop_voice_interaction(self):
        self.is_listening = False
        play_stop_sound()
        self.status_label.text = fix_arabic("جاري استخراج الكلام...")
        self.mic_btn.text = fix_arabic("انتظر...")

    # ---------- التسجيل الصوتي ----------
    def recording_thread(self):
        p = None
        stream = None
        try:
            p = pyaudio.PyAudio()
            stream = p.open(format=pyaudio.paInt16, channels=1, rate=SAMPLE_RATE,
                            input=True, frames_per_buffer=4000)
            rec = KaldiRecognizer(self.vosk_model, SAMPLE_RATE)

            start_time = time.time()
            last_partial = ""
            while self.is_listening:
                if time.time() - start_time > MAX_RECORDING_SECONDS:
                    print(">>> انتهى الحد الأقصى للتسجيل (30 ثانية)", flush=True)
                    break
                try:
                    data = stream.read(2000, exception_on_overflow=False)
                    # تكبير الصوت لرفع حساسية الميكروفون
                    if MIC_GAIN != 1.0:
                        try:
                            data = audioop.mul(data, 2, MIC_GAIN)
                        except audioop.error:
                            pass  # تجاوز قيم تسببت في clipping
                    rec.AcceptWaveform(data)

                    # إظهار النص الجزئي حتى يتأكد المستخدم من السماع
                    try:
                        partial = json.loads(rec.PartialResult()).get("partial", "").strip()
                        if partial and partial != last_partial:
                            last_partial = partial
                            Clock.schedule_once(
                                lambda dt, t=partial: self._update_partial(t)
                            )
                    except Exception:
                        pass
                except Exception as e:
                    print(f"[خطأ قراءة] {e}", flush=True)
                    break

            final_data = json.loads(rec.FinalResult())
            query = final_data.get("text", "").strip()

            if query:
                print(f">>> النص المفهوم: {query}", flush=True)
                Clock.schedule_once(lambda dt: self.ask_ollama(query))
            else:
                print(">>> لم يُسمع نص واضح", flush=True)
                Clock.schedule_once(lambda dt: self.reset_ui("لم أسمع نصاً واضحاً.. حاول مجدداً"))
        except Exception as e:
            print(f"[خطأ ميكروفون] {e}", flush=True)
            Clock.schedule_once(lambda dt: self.reset_ui("خطأ في الميكروفون.. حاول مجدداً"))
        finally:
            self.is_listening = False
            # تنظيف الموارد دائماً
            try:
                if stream is not None:
                    stream.stop_stream()
                    stream.close()
            except Exception:
                pass
            try:
                if p is not None:
                    p.terminate()
            except Exception:
                pass

    # ---------- Ollama (Streaming) ----------
    def _build_messages(self, prompt):
        """يبني قائمة الرسائل: system prompt + ذاكرة + السؤال الجديد."""
        msgs = [{'role': 'system', 'content': SYSTEM_PROMPT}]
        msgs.extend(self.conversation_history)
        msgs.append({'role': 'user', 'content': prompt})
        return msgs

    def _remember(self, user_text, assistant_text):
        """يخزن الرسالتين في الذاكرة ويقصها عند الحد."""
        self.conversation_history.append({'role': 'user', 'content': user_text})
        self.conversation_history.append({'role': 'assistant', 'content': assistant_text})
        if len(self.conversation_history) > HISTORY_LIMIT * 2:
            # نحتفظ بأحدث HISTORY_LIMIT تبادل (user + assistant)
            self.conversation_history = self.conversation_history[-HISTORY_LIMIT * 2:]

    def ask_ollama(self, prompt):
        self.status_label.text = fix_arabic("وليف يفكر الآن...")
        play_think_sound()
        self.eyes.eye_color = [1, 0.8, 0, 1]  # أصفر = تفكير

        def run():
            full_answer = ""
            buf = ""
            messages = self._build_messages(prompt)
            chat_options = {
                'temperature': 0.8,
                'top_p': 0.9,
                'repeat_penalty': 1.15,
            }
            try:
                stream = ollama.chat(
                    model=OLLAMA_MODEL,
                    messages=messages,
                    stream=True,
                    options=chat_options,
                )
                for chunk in stream:
                    piece = extract_chunk_content(chunk)
                    if not piece:
                        continue
                    full_answer += piece
                    buf += piece

                    # كل ما تكتمل جملة أرسلها للنطق فوراً
                    sentences = split_into_sentences(buf)
                    if len(sentences) > 1:
                        for s in sentences[:-1]:
                            self.tts_queue.put(s)
                        buf = sentences[-1]

                    # تحديث الواجهة بآخر كلام
                    Clock.schedule_once(
                        lambda dt, ans=full_answer: self._update_status(ans)
                    )

                # نطق ما تبقى من الكلام
                tail = buf.strip()
                if tail:
                    self.tts_queue.put(tail)

                # خطة بديلة: لو الـ stream ما رجّع شي، جرّب بدون streaming
                if not full_answer.strip():
                    print("[تحذير] streaming رجع فاضي، تجربة بدون streaming...", flush=True)
                    try:
                        resp = ollama.chat(
                            model=OLLAMA_MODEL,
                            messages=messages,
                            options=chat_options,
                        )
                        if isinstance(resp, dict):
                            full_answer = (resp.get('message') or {}).get('content', '') or ''
                        else:
                            msg = getattr(resp, 'message', None)
                            full_answer = getattr(msg, 'content', '') or '' if msg else ''
                        if full_answer.strip():
                            for s in split_into_sentences(full_answer):
                                self.tts_queue.put(s)
                    except Exception as e2:
                        print(f"[فشلت الخطة البديلة] {e2}", flush=True)

                if full_answer.strip():
                    self._remember(prompt, full_answer.strip())
                    Clock.schedule_once(lambda dt: self._on_response_done(full_answer))
                else:
                    Clock.schedule_once(lambda dt: self.reset_ui("ما وصلني رد من العقل.. جرّب مجدداً"))
            except Exception as e:
                print(f"[خطأ Ollama] {e}", flush=True)
                Clock.schedule_once(lambda dt: self.reset_ui("فشل الرد.. تأكد من تشغيل Ollama"))

        threading.Thread(target=run, daemon=True).start()

    def _update_status(self, text):
        # نعرض آخر 200 حرف فقط لمنع تجاوز حدود الـ Label
        display = text if len(text) <= 200 else "..." + text[-200:]
        self.status_label.text = fix_arabic(display)

    def _update_partial(self, text):
        """يعرض النص الجزئي أثناء التسجيل ليتأكد المستخدم من السماع."""
        if not self.is_listening:
            return
        display = text if len(text) <= 80 else "..." + text[-80:]
        self.status_label.text = fix_arabic("🎙️ " + display)

    def _on_response_done(self, answer):
        self.eyes.eye_color = [0, 0.8, 1, 1]
        print(f">>> الرد الكامل: {answer}", flush=True)
        self.reset_ui()

    def reset_ui(self, msg=None):
        if msg:
            self.status_label.text = fix_arabic(msg)
        self.mic_btn.text = fix_arabic("ابدأ الكلام")
        self.mic_btn.background_color = (0, 0.7, 1, 1)
        self.is_listening = False

    # ---------- نظام النطق (TTS) ----------
    def _clear_tts_queue(self):
        try:
            while True:
                self.tts_queue.get_nowait()
        except queue.Empty:
            pass

    def _tts_worker(self):
        """عامل واحد ينطق الجمل بالترتيب من الطابور."""
        while True:
            text = self.tts_queue.get()
            if text is None:
                break
            try:
                self._speak_one(text)
            except Exception as e:
                print(f"[خطأ نطق] {e}", flush=True)

    def _synthesize_to_wav(self, text, wav_path):
        """يدعم نسختين من Piper API."""
        with wave.open(wav_path, "wb") as wav_f:
            if hasattr(self.piper_voice, 'synthesize_wav'):
                self.piper_voice.synthesize_wav(text, wav_f)
            else:
                self.piper_voice.synthesize(text, wav_f)

    def _speak_one(self, text):
        text = text.strip()
        if not text:
            return
        if self.piper_voice is None:
            print(f"[لا يوجد صوت متاح] {text}", flush=True)
            return

        tmp_path = None
        try:
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
                tmp_path = tmp.name
            self._synthesize_to_wav(text, tmp_path)
            winsound.PlaySound(tmp_path, winsound.SND_FILENAME)
        finally:
            if tmp_path:
                try:
                    os.remove(tmp_path)
                except Exception:
                    pass


if __name__ == '__main__':
    WaleefApp().run()
