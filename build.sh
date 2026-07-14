#!/bin/bash
set -e
cd "$(dirname "$0")"

echo "============================================"
echo "  Netdisk - Full Build"
echo "============================================"
echo

# ---------- 1. Frontend ----------
echo "[1/4] Building frontend..."
cd front_end
npx vite build
echo "[OK] Frontend built -> back_end/src/main/resources/static/"
cd ..
echo

# ---------- 2. Backend compile ----------
echo "[2/4] Compiling backend..."
cd back_end
mvn compile -q
echo "[OK] Backend compiled."
echo

# ---------- 3. Unit tests ----------
echo "[3/4] Running unit tests..."
mvn test -q
echo "[OK] All tests passed."
echo

# ---------- 4. Package jar ----------
echo "[4/4] Packaging jar..."
mvn package -DskipTests -q
JAR=$(ls target/Netdisk-*.jar | head -1)
echo "[OK] Package complete."
echo

echo "============================================"
echo "  Build successful!"
echo "  Output: $JAR"
echo "============================================"
echo
echo "Run with: java -jar $JAR"
