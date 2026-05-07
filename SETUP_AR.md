# دليل تشغيل وليف 🎙️

## المتطلبات

### 1. تثبيت مكتبات بايثون
```
pip install -r requirements.txt
```

### 2. تنزيل موديل Vosk العربي
- من الرابط: https://alphacephei.com/vosk/models
- اختر موديل `vosk-model-ar-*` (نسخة عربية)
- فك الضغط وضع المحتويات في مجلد اسمه `model` بجانب الملف `waleef.py`

### 3. تنزيل صوت Piper العربي (لنطق الردود)
1. أنشئ مجلد اسمه `voices` بجانب الملف `waleef.py`
2. نزّل ملفين من Hugging Face:
   - الموديل: https://huggingface.co/rhasspy/piper-voices/resolve/main/ar/ar_JL/kareem/medium/ar_JL-kareem-medium.onnx
   - ملف الإعدادات: https://huggingface.co/rhasspy/piper-voices/resolve/main/ar/ar_JL/kareem/medium/ar_JL-kareem-medium.onnx.json
3. ضع الملفين داخل مجلد `voices`

### 4. تأكد من أن Ollama شغّال
```
ollama serve
ollama list
```
لازم يظهر الموديل `gemma4:e2b` في القائمة.

---

## شجرة الملفات النهائية
```
waleef-app/
├── waleef.py
├── requirements.txt
├── Arial.ttf
├── model/                          ← موديل Vosk العربي
│   ├── am/
│   ├── conf/
│   └── ...
└── voices/                         ← أصوات Piper
    ├── ar_JL-kareem-medium.onnx
    └── ar_JL-kareem-medium.onnx.json
```

---

## التشغيل
```
python waleef.py
```

---

## الإصلاحات اللي تمت في هذا الإصدار

| المشكلة | الحل |
|---|---|
| الانهيار المفاجئ بدون رد صوتي | استبدال `pyttsx3` بـ Piper TTS (محلي 100%، صوت عربي طبيعي، لا يعتمد على COM) |
| بطء الرد الأول | إحماء موديل Ollama عند بدء التطبيق |
| بطء استقبال الردود الطويلة | تفعيل Streaming من Ollama والنطق جملة جملة فور وصولها |
| تسريب الميكروفون عند الأخطاء | استخدام `try/finally` لتنظيف موارد PyAudio دائماً |
| التسجيل المفتوح للأبد | حد أقصى 30 ثانية للتسجيل |
| تداخل الأصوات | عامل نطق واحد (Worker) مع طابور Queue |
| لا يوجد Fallback عند فشل التحميل | الزر معطّل حتى تجهز الموديلات + رسائل خطأ واضحة |
| الانتظار قبل أول رد | تحميل الموديلات في الخلفية، الواجهة تظهر فوراً |

---

## ملاحظات مهمة

- **عند الضغط على "ابدأ الكلام" أثناء النطق**: يتم إيقاف النطق فوراً وتفريغ طابور الكلام.
- **النطق جملة جملة**: تحس وليف يرد عليك مباشرة بدون انتظار الرد الكامل.
- **العيون الصفراء = يفكر، الزرقاء = جاهز**.

## بدائل صوت Piper عربية (لو حبيت تجرب)
- `ar_JL-kareem-low.onnx` - حجم أصغر، جودة أقل
- يمكن تعديل المسار من `PIPER_VOICE_PATH` في أعلى `waleef.py`
