#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to JDK21}"
: "${ANDROID_JAR:?Set ANDROID_JAR to platform35/android.jar}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to build-tools35.0.0}"
export PATH="$JAVA_HOME/bin:$PATH"
PULSE_FRAMEWORK_LIBS="${PULSE_FRAMEWORK_LIBS:-$PWD/build/framework-libs}"
mkdir -p "$PULSE_FRAMEWORK_LIBS" build/robo-generated build/robo-classes
if [ "${PULSE_SKIP_RESOLVE:-0}" != 1 ]; then
  PULSE_MVN="${PULSE_MVN:-mvn}"
  PULSE_MVN_ARGS=(-B)
  if [ -n "${PULSE_MAVEN_SETTINGS:-}" ]; then PULSE_MVN_ARGS+=(-s "$PULSE_MAVEN_SETTINGS"); fi
  "$PULSE_MVN" "${PULSE_MVN_ARGS[@]}" -f tests/robolectric/pom.xml dependency:copy-dependencies "-DoutputDirectory=$PULSE_FRAMEWORK_LIBS"
fi
PULSE_NATIVE_SDK=android-all-14-robolectric-10818077.jar
if [ ! -f "$PULSE_FRAMEWORK_LIBS/$PULSE_NATIVE_SDK" ]; then
  curl -fL --max-time 180 "https://repo.maven.apache.org/maven2/org/robolectric/android-all/14-robolectric-10818077/$PULSE_NATIVE_SDK" -o "$PULSE_FRAMEWORK_LIBS/$PULSE_NATIVE_SDK"
fi
python3 - "$PULSE_FRAMEWORK_LIBS" <<'PY'
import sys,zipfile,pathlib
for p in pathlib.Path(sys.argv[1]).glob('*.aar'):
    with zipfile.ZipFile(p) as z:
        if 'classes.jar' in z.namelist():p.with_suffix('.jar').write_bytes(z.read('classes.jar'))
PY
"$BUILD_TOOLS/aapt" package -f -m -J build/robo-generated -M app/src/main/AndroidManifest.xml -S app/src/main/res -I "$ANDROID_JAR"
javac --release 8 -cp "$ANDROID_JAR:$PULSE_FRAMEWORK_LIBS/*:build/classes" -d build/robo-classes build/robo-generated/work/solour/pulse/R.java tests/robolectric/*.java
PULSE_JAVA_FLAGS=(--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED -Drobolectric.offline=true "-Drobolectric.dependency.dir=$PULSE_FRAMEWORK_LIBS" -Drobolectric.usePreinstrumentedJars=false)
java "${PULSE_JAVA_FLAGS[@]}" -Drobolectric.resourcesMode=legacy -cp "$PULSE_FRAMEWORK_LIBS/android-all-9-robolectric-4913185-2.jar:$PULSE_FRAMEWORK_LIBS/*:build/classes:build/robo-classes" org.junit.runner.JUnitCore work.solour.pulse.PulseUiTest | tee build/framework-test.log
rg -q '^OK \(4 tests\)' build/framework-test.log
java "${PULSE_JAVA_FLAGS[@]}" -Drobolectric.resourcesMode=binary -cp "$PULSE_FRAMEWORK_LIBS/$PULSE_NATIVE_SDK:$PULSE_FRAMEWORK_LIBS/*:build/classes:build/robo-classes" org.junit.runner.JUnitCore work.solour.pulse.NativePreviewTest | tee build/native-preview-test.log
rg -q '^OK \(1 test\)' build/native-preview-test.log
