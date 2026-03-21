# قرين (Qarin) - Wear OS App Architecture

## Overview
تطبيق Wear OS + Android متكامل لإدارة المهام، المهارات، التذكيرات، والملاحظات مع تشفير محلي ومزامنة ساعة-هاتف.

## Modules

```
qarin/
├── shared/          # النماذج المشتركة والتشفير
│   └── src/main/java/com/qarin/shared/
│       ├── models/  # QarinTask, QarinSkill, QarinReminder, QarinNote
│       └── crypto/  # CryptoManager (AES-256 + Android Keystore)
│
├── wear/            # تطبيق الساعة (Wear OS 2+)
│   └── src/main/java/com/qarin/wear/
│       ├── data/db/        # Room + SQLCipher (مشفر)
│       ├── data/sync/      # WearSyncManager (DataLayer API)
│       ├── service/        # DataLayer, Reminder, Tile services
│       └── ui/             # Wear Compose UI
│
└── mobile/          # تطبيق الهاتف المرافق
    └── src/main/java/com/qarin/mobile/
        ├── data/db/        # Room + SQLCipher (مشفر)
        ├── data/sync/      # MobileSyncManager
        ├── service/        # DataLayer, Camera, Reminder services
        └── ui/             # Material3 Compose UI
```

## Security Architecture
- **AES-256-GCM** encryption via Android Keystore
- **SQLCipher** encrypted Room databases
- Database passphrase generated randomly, encrypted with Android Keystore
- All data remains **100% local** - no cloud sync

## Data Sync Flow
```
Watch connected to Phone:
  Write → Local DB (SQLCipher) → DataLayer API → Phone DB (SQLCipher)

Watch disconnected:
  Write → Local Watch DB (SQLCipher) [temporary]

Watch reconnects:
  Pending data → DataLayer → Phone DB (merge)
```

## Voice Activation ("يا قرين")
1. User presses dedicated button on watch bezel
2. `RecognizerIntent` launched with Arabic locale
3. Recognized text parsed for commands:
   - "أضف مهمة [العنوان]" → addTask()
   - "ذكرني بـ [العنوان]" → addReminder()
   - "سجل ملاحظة [النص]" → addNote()
   - "افتح كاميرا" → requestCameraCapture()
   - "مزامنة" → manualSync()

## Camera Remote Flow
```
Watch: requestCameraCapture() → DataLayer Message → Phone
Phone: RemoteCameraService (Foreground) → Camera2 API
      → Capture JPEG → Compress → DataLayer Message → Watch
Watch: Save as Note with photo attachment
```

## Key Technologies
- Kotlin + Coroutines + Flow
- Jetpack Compose (Material3 mobile, Wear Compose watch)
- Room + SQLCipher (AES-256 encryption)
- Wearable DataLayer API (MessageClient + DataClient)
- Google TTS (Arabic + English)
- Android SpeechRecognizer
- Camera2 API
- Wear OS Tiles
