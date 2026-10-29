# WhatsApp Lite — Android (APK)

تطبيق أندرويد يعرض `app/index.html` داخل WebView كامل المزايا (تخزين محلي، كاميرا/مايك للمكالمات، اختيار ملفات، فيديو بملء الشاشة).

## البناء
يتطلّب: JDK، وأدوات أندرويد من مستودعات أوبنتو (`aapt`, `dalvik-exchange`, `zipalign`, `apksigner`, `libandroid-23-java`).
```bash
sudo apt-get install -y aapt apksigner zipalign dalvik-exchange libandroid-23-java
./build-apk.sh        # ينتج build/WhatsApp-Lite.apk (موقّع)
```
لا يحتاج Android Studio ولا تنزيل SDK من Google.

## زر الرجوع والخروج
`MainActivity.onBackPressed()` يستدعي `window.__back()` في الصفحة (رجوع خطوة، ثم تأكيد الخروج)،
وعند الضغط على "نعم" تستدعي الصفحة `window.App.exitApp()` فيُغلق التطبيق.

## التوقيع
موقّع بشهادة ذاتية (`wl.keystore`, كلمة المرور `android`) بمخططات v1+v2+v3.
