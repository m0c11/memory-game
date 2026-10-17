#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo "Ошибка: Java не найдена. Установи JDK 17 или новее (например, sudo apt install openjdk-17-jdk)." >&2
  exit 1
fi
JAVA_MAJOR=$(java -version 2>&1 | sed -n '1s/.*version "\\?\\([0-9]*\\).*/\\1/p')
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "Ошибка: нужна Java 17 или новее; сейчас установлена Java $JAVA_MAJOR." >&2
  exit 1
fi
exec ./gradlew --no-daemon desktop:run
