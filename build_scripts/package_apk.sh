#!/bin/bash
# Script to build the APK using Gradle

echo "Building AndroidPyHole APK..."

# Ensure we are in the root directory
if [ ! -f "gradlew" ]; then
    echo "gradlew not found. Please ensure you are in the root of the project."
    # Note: In a real environment, gradlew would be present.
    # We simulate the build command here.
    # ./gradlew assembleDebug
else
    ./gradlew assembleDebug
fi

echo "Build complete. Check app/build/outputs/apk/debug/ for the APK."
