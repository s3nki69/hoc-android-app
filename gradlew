#!/usr/bin/env sh
set -eu

# HOC Android build wrapper.
# The project uses Android Gradle Plugin 8.2.0 + Kotlin 1.9.20,
# so we deliberately pin Gradle to the compatible 8.2.1 release.
GRADLE_VERSION="8.2.1"
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
CACHE_ROOT="${GRADLE_USER_HOME:-$HOME/.gradle}/hoc-wrapper"
DIST_DIR="$CACHE_ROOT/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"
ZIP_FILE="$CACHE_ROOT/gradle-$GRADLE_VERSION-bin.zip"
DIST_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_DIR"
  if [ ! -f "$ZIP_FILE" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl --fail --location --retry 3 --output "$ZIP_FILE" "$DIST_URL"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_FILE" "$DIST_URL"
    else
      echo "ERROR: curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi

  rm -rf "$DIST_DIR"/*
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ZIP_FILE" -d "$DIST_DIR"
  else
    echo "ERROR: unzip is required to unpack Gradle." >&2
    exit 1
  fi
fi

cd "$APP_HOME"
exec "$GRADLE_BIN" "$@"
