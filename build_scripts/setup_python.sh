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
cp -r gui $ASSETS_DIR/

echo "Python environment initialized successfully."
