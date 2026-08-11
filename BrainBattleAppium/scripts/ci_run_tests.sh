#!/bin/bash
set -e

echo "Installing APK..."
adb install -r "${APK_PATH}"

echo "Starting Appium..."
appium --log-level warn > /tmp/appium.log 2>&1 &

# Wait for Appium
echo "Waiting for Appium to start..."
timeout 60 bash -c 'until curl -s http://127.0.0.1:4723/status > /dev/null; do sleep 1; done'
echo "Appium is ready."

# Inject GITHUB_PATH into PATH if present so we have node
if [ -f "$GITHUB_PATH" ]; then
  while IFS= read -r line; do
    export PATH="$line:$PATH"
  done < "$GITHUB_PATH"
fi

export WDIO_CI_SPEC="./tests/12_e2e/mega_android_1100.test.js"

echo "Running WDIO Appium tests..."
# Use node directly or npx
if ! npx wdio run wdio.conf.js; then
    echo "WDIO run failed. Generating fallback report..."
    node utils/generateFallbackReport.js
    exit 1
fi

echo "Appium tests completed successfully."
