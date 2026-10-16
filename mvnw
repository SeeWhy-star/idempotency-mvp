#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MAVEN_VERSION=3.9.16
MAVEN_HOME="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION"
ARCHIVE="$BASE_DIR/.mvn/apache-maven-$MAVEN_VERSION-bin.tar.gz"
URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz
EXPECTED_SHA256=80ffca22aed9e8b9713a232f3394fd81d7f20322df75efdb2b047dbd3e3a23bb

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  command -v curl >/dev/null 2>&1 || {
    echo "curl is required to bootstrap Maven" >&2
    exit 1
  }
  mkdir -p "$BASE_DIR/.mvn"
  curl --fail --location --output "$ARCHIVE" "$URL"
  ACTUAL_SHA256=$(sha256sum "$ARCHIVE" | awk '{print $1}')
  if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
    echo "Maven distribution checksum mismatch" >&2
    exit 1
  fi
  tar -xzf "$ARCHIVE" -C "$BASE_DIR/.mvn"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
