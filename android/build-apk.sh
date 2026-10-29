#!/usr/bin/env bash
# بناء WhatsApp Lite كملف APK موقّع — بدون Android Studio، أدوات محلية فقط
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR=/usr/lib/android-sdk/platforms/android-23/android.jar
OUT=build
APP=WhatsApp-Lite
PKGHTML=../app/index.html

rm -rf "$OUT"; mkdir -p "$OUT/classes"

echo "» تحديث نسخة الواجهة في assets"
cp "$PKGHTML" assets/index.html

echo "» [1/6] تجميع الموارد وبناء APK مبدئي (aapt)"
aapt package -f \
  -M AndroidManifest.xml \
  -S res \
  -A assets \
  -I "$ANDROID_JAR" \
  -F "$OUT/$APP-unaligned.apk"

echo "» [2/6] ترجمة Java (javac)"
javac -source 8 -target 8 -classpath "$ANDROID_JAR" \
  -d "$OUT/classes" \
  src/app/whatsapplite/*.java

echo "» [3/6] تحويل إلى Dalvik (dx)"
( cd "$OUT" && dalvik-exchange --dex --output=classes.dex classes )

echo "» [4/6] إضافة classes.dex إلى الحزمة"
( cd "$OUT" && aapt add "$APP-unaligned.apk" classes.dex >/dev/null )

echo "» [5/6] محاذاة (zipalign)"
zipalign -f 4 "$OUT/$APP-unaligned.apk" "$OUT/$APP-aligned.apk"

echo "» [6/6] التوقيع (apksigner)"
KS=wl.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias wl -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass android -keypass android \
    -dname "CN=WhatsApp Lite, O=WhatsApp Lite, C=SA" >/dev/null 2>&1
fi
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT/$APP.apk" "$OUT/$APP-aligned.apk"

apksigner verify --print-certs "$OUT/$APP.apk" >/dev/null && echo "✓ التوقيع صحيح"
echo "✓ جاهز: $OUT/$APP.apk"
ls -la "$OUT/$APP.apk"
