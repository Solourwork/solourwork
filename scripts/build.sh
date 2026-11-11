#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_JAR:?Set ANDROID_JAR to platforms/android-35/android.jar}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to Android build-tools/35.0.0}"
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
export PATH="$JAVA_HOME/bin:$PATH"
rm -rf build/classes build/test build/dex
mkdir -p build/classes build/test build/dex dist
javac --release 8 -d build/test app/src/main/java/work/solour/pulse/Engine.java app/src/main/java/work/solour/pulse/CompanionDialogue.java tests/EngineTest.java tests/CompanionDialogueTest.java
java -cp build/test work.solour.pulse.EngineTest
java -cp build/test work.solour.pulse.CompanionDialogueTest
javac --release 8 -cp "$ANDROID_JAR" -d build/classes app/src/main/java/work/solour/pulse/*.java
jar cf build/classes.jar -C build/classes .
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --min-api 26 --output build/dex build/classes.jar
"$BUILD_TOOLS/aapt" package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -A app/src/main/assets -I "$ANDROID_JAR" -0 wav -F build/base.apk
cp build/base.apk build/unsigned.apk
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [ -n "${SIGNING_KEYSTORE:-}" ]; then
 : "${SIGNING_PASSWORD:?Set SIGNING_PASSWORD privately}"
 "$BUILD_TOOLS/apksigner" sign --ks "$SIGNING_KEYSTORE" --ks-pass env:SIGNING_PASSWORD --ks-key-alias pulse --out dist/pulse-${APP_VERSION:-1.2.0-ui-preview}.apk build/aligned.apk
 "$BUILD_TOOLS/apksigner" verify --verbose --print-certs dist/pulse-${APP_VERSION:-1.2.0-ui-preview}.apk
else
 echo 'Unsigned aligned APK at build/aligned.apk. Set signing environment variables to sign.'
fi
