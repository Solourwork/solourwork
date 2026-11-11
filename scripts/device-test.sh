#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export PATH="$JAVA_HOME/bin:$PATH"
rm -rf build/device-classes build/device-dex
mkdir -p build/device-classes build/device-dex
javac --release 8 -cp "$ANDROID_JAR:build/classes" -d build/device-classes tests/android/*.java
jar cf build/device.jar -C build/device-classes .
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --classpath build/classes.jar --min-api 26 --output build/device-dex build/device.jar
"$BUILD_TOOLS/aapt" package -f -M tests/android/AndroidManifest.xml -I "$ANDROID_JAR" -F build/test.apk
(cd build/device-dex && zip -q -u ../test.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f 4 build/test.apk build/test-aligned.apk
"$BUILD_TOOLS/apksigner" sign --ks "$SIGNING_KEYSTORE" --ks-pass env:SIGNING_PASSWORD --ks-key-alias pulse --out build/test-signed.apk build/test-aligned.apk
"$ADB" install -r build/test-signed.apk
"$ADB" shell am instrument -w "work.solour.pulse.test/work.solour.pulse.${TEST_RUNNER:-FlowRunner}" | tee build/last-device-test.log
if rg -q "^FAIL|INSTRUMENTATION_FAILED|Process crashed|INSTRUMENTATION_ABORTED" build/last-device-test.log; then exit 1; fi
rg -q "^PASS [0-9]+" build/last-device-test.log
