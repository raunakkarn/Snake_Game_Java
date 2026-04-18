#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/out"

if ! command -v javac >/dev/null 2>&1; then
  echo "javac not found. Install a full JDK to build this project." >&2
  exit 1
fi

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR" "$ROOT_DIR/data"

javac \
  -d "$OUT_DIR" \
  $(find "$ROOT_DIR/src" -name '*.java' | sort)
