#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$ROOT_DIR/out"
LIB_DIR="$ROOT_DIR/lib"

"$ROOT_DIR/build.sh"

CLASSPATH="$OUT_DIR"
if [ -f "$LIB_DIR/sqlite-jdbc.jar" ]; then
  CLASSPATH="$CLASSPATH:$LIB_DIR/sqlite-jdbc.jar"
fi

java -cp "$CLASSPATH" com.heisenberg.snake.Main
