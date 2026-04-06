# AndroidPyHole

AndroidPyHole is a full-featured Pi-hole clone for Android devices. It provides DNS-level ad and tracker blocking using a Python backend and an Android VPNService, allowing it to work on both rooted and rootless devices.

## Features

- **DNS Ad/Tracker Blocking**: Intercepts DNS queries and blocks them based on custom or community blocklists.
- **Rootless Operation**: Uses the Android VPNService API to redirect DNS traffic without requiring root.
- **Web Dashboard**: An embedded Flask-based dashboard for viewing real-time statistics, query logs, and managing settings.
- **Query Logs**: A detailed, filterable view of recent DNS activity (Allowed vs. Blocked).
- **Blocklist Management**: Automatically updates blocklists and allows for custom domain blocking/allowing.
- **Query Logging**: Persistent SQLite database for tracking all DNS queries and block rates.
- **Client Management**: Track and manage different devices making DNS requests.
- **Modern UI**: Dark-mode dashboard inspired by the 'Obsidian Shield' design system.
- **Splash Screen**: Professional app entry with branded splash activity.

## Architecture

- **Android Frontend (Kotlin)**: Manages the VPNService and the life cycle of the Python backend via Chaquopy.
- **Python Backend**: Handles DNS resolution (asyncio/aiodns), blocklist management, and the Flask web server for the dashboard.
- **Web Dashboard (Flask/HTML/JS)**: Accessible within the app's WebView or from any local browser.

## Getting Started: Build-Ready Instructions

### 1. Prerequisites

- **Android Studio (Giraffe or newer)**
- **Java JDK 17+**
- **Python 3.10+** (installed on build machine)
- **Internet Connection** (to download dependencies)

### 2. Setup the Environment

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-repo/AndroidPyHole.git
    cd AndroidPyHole
    ```

2.  **Initialize Python Assets**:
    Run the setup script to copy python and web assets to the appropriate Android project directories:
    ```bash
    ./build_scripts/setup_python.sh
    ```

### 3. Build and Run

1.  **Open in Android Studio**:
    Open the root folder of the project in Android Studio.

2.  **Sync Project with Gradle Files**:
    Ensure all dependencies (Chaquopy, Android SDK, etc.) are downloaded.

3.  **Build the APK**:
    From the terminal:
    ```bash
    ./gradlew assembleDebug
    ```
    Or use **Build > Build Bundle(s) / APK(s) > Build APK(s)** in Android Studio.

4.  **Install on Device**:
    Use ADB or the Android Studio "Run" button to deploy to your device.

## Project Structure

- `app/`: Android Kotlin source code, resources, and build configuration.
- `python/`: Core backend logic (DNS server, blocklist manager, logger, etc.).
- `gui/`: HTML, CSS, and JS files for the web-based dashboard.
- `storage/`: Placeholder for local data storage (logs, blocklists, config).
- `build_scripts/`: Scripts for initializing and packaging the project.

## Credits

- **Pi-hole**: Inspiration for the DNS blocking functionality and dashboard.
- **Chaquopy**: For enabling Python integration in Android.
- **Asyncio/aiodns**: For efficient asynchronous DNS handling in Python.

## License

MIT License
