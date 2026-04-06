#!/bin/bash
# Setup script for Python environment inside the Android app
# This script is intended to be run during the build process to prepare the Python assets

echo "Initializing Python environment for AndroidPyHole..."

# Create necessary directories in assets
# This is where Chaquopy looks for python code and libraries
ASSETS_DIR="app/src/main/python"
mkdir -p $ASSETS_DIR

# Copy python files to the assets directory
cp python/*.py $ASSETS_DIR/

# Copy gui files to assets (assuming web dashboard is served from here)
# Use -R for recursive copy of gui directory
cp -R gui/static $ASSETS_DIR/
cp -R gui/templates $ASSETS_DIR/
# Copy individual files if any in gui root
cp gui/package.json $ASSETS_DIR/ 2>/dev/null || true

echo "Python environment initialized successfully."
