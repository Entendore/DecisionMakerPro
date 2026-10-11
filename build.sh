#!/usr/bin/env bash
# DecisionMaker Pro — one-command build
set -euo pipefail
cd "$(dirname "$0")"

GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'

if [ ! -f ./gradlew ]; then
  echo -e "${CYAN}▸ Bootstrapping Gradle wrapper…${NC}"
  if command -v gradle &>/dev/null; then
    gradle wrapper --gradle-version 8.5
  elif command -v curl &>/dev/null; then
    curl -fsSL -o gradle/wrapper/gradle-wrapper.jar \
      "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar"
    curl -fsSL -o gradlew \
      "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradlew"
    curl -fsSL -o gradlew.bat \
      "https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradlew.bat"
    chmod +x gradlew
  else
    echo "Open in Android Studio instead — it generates the wrapper."
    exit 1
  fi
fi

echo -e "${CYAN}▸ Unit tests…${NC}"
./gradlew testDebugUnitTest
echo -e "${CYAN}▸ Assembling debug APK…${NC}"
./gradlew assembleDebug
echo -e "${GREEN}✔ Build complete${NC}"
echo "  APK: app/build/outputs/apk/debug/app-debug.apk"
