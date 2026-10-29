# WhatsApp Lite — Desktop (EXE)

تطبيق سطح المكتب لنظام Windows مبني من `app/index.html` باستخدام Electron.

## التعديلات على الواجهة
- عند حذف رسالة لدى الجميع تظهر تحتها **«تم حذف هذه الرسالة»** (أو «لقد حذفت هذه الرسالة» للمرسِل)، وتظهر كذلك في قائمة المحادثات.
- الضغط على صورة المستخدم يفتح **معلومات جهة الاتصال** (اسم المستخدم، مشاركة، نسخ، تثبيت، حظر…).
- زر الرجوع: يرجع خطوة للخلف (من الدردشة إلى القائمة)، وعند الضغط مرة أخرى يسأل **«هل تريد الخروج؟»** ثم يُغلق التطبيق عند «نعم». يعمل أيضاً عبر Alt+← وEsc وزر الفأرة الجانبي.

## الحماية
- الواجهة (`index.html`) مخزّنة **مشفّرة AES-256-GCM** داخل الحزمة (`app.dat`) ولا تُفك إلا في الذاكرة.
- كود Electron (`main.js` / `preload.js`) **مموّه** (javascript-obfuscator).
- تُحمّل الصفحة عبر بروتوكول داخلي آمن `app://` وليس من ملف، مع `contextIsolation` و`sandbox` وبدون `nodeIntegration`.
- تعطيل أدوات المطوّر وقائمة السياق واختصارات الفحص.
- تفعيل **Electron Fuses** (منع runAsNode، التحقق من سلامة asar، التحميل من asar فقط) وتشفير الكوكيز.

## البناء
```bash
npm install
npm run dist      # ينتج المثبّت والنسخة المحمولة في مجلد release/
```
المخرجات:
- `release/WhatsApp-Lite-Setup-1.0.0.exe` — مثبّت (NSIS) باختيار مجلد التثبيت وأيقونة سطح المكتب.
- `release/WhatsApp-Lite-Portable-1.0.0.exe` — نسخة محمولة تعمل مباشرة بدون تثبيت.

> البناء على Linux يتطلّب Wine (wine + wine32:i386) لتضمين الأيقونة وبيانات الإصدار.

## التوقيع بشهادة ذاتية
التطبيق موقَّع بشهادة **توقيع ذاتية** (self-signed، صالحة 10 سنوات) بتوقيع مزدوج SHA-1 + SHA-256، باستخدام `osslsigncode` عبر خطّاف `scripts/sign.js`.

لإعادة التوليد والتوقيع:
```bash
# توليد الشهادة (مرة واحدة)
openssl req -x509 -newkey rsa:3072 -keyout cert/key.pem -out cert/cert.pem -days 3650 -nodes \
  -subj "/CN=WhatsApp Lite/O=WhatsApp Lite/C=SA" \
  -addext "keyUsage=digitalSignature" -addext "extendedKeyUsage=codeSigning"
openssl pkcs12 -export -out cert/WhatsAppLite.pfx -inkey cert/key.pem -in cert/cert.pem -passout pass:whatsapplite
# البناء مع التوقيع
export CSC_LINK="$PWD/cert/WhatsAppLite.pfx" CSC_KEY_PASSWORD="whatsapplite"
npm run dist
```

> المفتاح الخاص (`cert/*.pfx` و`cert/key.pem`) غير مرفوع إلى Git.

### على جهاز المستخدم
الشهادة الذاتية لا تثق بها Windows افتراضياً (ستظهر SmartScreen). لإزالة التحذير:
شغّل `release/تثبيت-الشهادة.bat` **كمسؤول** مرة واحدة — يثبّت `WhatsApp-Lite-Certificate.cer` في Trusted Root وTrusted Publishers، فيظهر الناشر "WhatsApp Lite".
