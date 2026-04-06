# AndroidPyHole: The Ultimate Private DNS & Ad-Blocking Solution for Android

AndroidPyHole is an advanced, absolute ultimate feature-complete Pi-hole clone built for Android. It leverages a powerful Python-based DNS engine (asyncio/aiodns) and the Android VPNService API to provide a seamless, hardware-free network security experience for both rooted and rootless devices.

## 🚀 Key Feature Highlights

### 🛡️ Professional DNS Engine
- **Full Packet Parsing**: Real DNS packet inspection using `dnslib`.
- **Regex Filter Engine**: Support for complex regular expression blocking patterns.
- **Per-Client Control**: Assign specific blocking rules and tags to individual devices.
- **Local DNS Mapping**: Custom hostname resolution (e.g., mapping `pi.hole` to your local instance).
- **Encrypted Upstreams**: Configurable support for DNS-over-HTTPS (DoH) and DNS-over-TLS (DoT).

### 📊 Advanced Analytics & Dashboard
- **Real-time Monitoring**: Instant visibility into network traffic, block rates, and cache hits.
- **Hourly Trends**: 24-hour visual traffic charts using Chart.js.
- **Client Intelligence**: Detailed breakdown of top-requesting devices and their status.
- **Audit Logs**: Filterable, color-coded history of every DNS query handled.

### 🔒 Enterprise-Grade Security & Privacy
- **Authenticated Dashboard**: Password-protected access to your statistics and settings.
- **Tiered Privacy Modes**:
    - **Show All**: Standard full visibility.
    - **Anonymous**: Mask all domains and client IPs in the UI and logs.
    - **Hide All**: Maximum privacy; no logs or statistics are displayed.
- **Secure Storage**: All data is kept locally in a secure SQLite database.

### 🛠️ Diagnostic & Management Tools
- **Network Scanner**: Discover and identify active devices on your local network.
- **System Alerts**: Real-time notifications for unusual DNS activity or system events.
- **Gravity Maintenance**: Automated and manual blocklist synchronization.
- **Config Portability**: Effortlessly **Export** or **Import** your entire configuration via JSON.
- **System Tools**: Flush logs, clear DNS cache, and restart the DNS engine from the UI.

### 🎨 Premium Experience
- **Adaptive UI**: High-contrast 'Obsidian Shield' dark mode with a clean light mode alternative.
- **Mobile Optimized**: Responsive web dashboard served via embedded Flask and Android WebView.
- **Branded Startup**: Custom Splash Screen for a professional application experience.

## 🏗️ Technical Architecture

- **Backend (Python 3.10+)**: Asyncio resolution engine, Flask web server, and SQLite storage.
- **Frontend (Kotlin)**: Life-cycle management, VPNService bridge, and WebView integration.
- **Integration**: Powered by **Chaquopy** for high-performance Python-on-Android execution.

## 🛠️ Build & Development

### 1. Initialize Assets
Prepare the Python and Web assets:
```bash
./build_scripts/setup_python.sh
```

### 2. Android Build
Open the root directory in **Android Studio (Giraffe+)**, sync Gradle, and build:
```bash
./gradlew assembleDebug
```

## 📦 CI/CD Pipeline
Fully configured GitHub Actions:
- `testing.yml`: Automatic unit tests (Python) and build verification.
- `apk-debug.yml`: Manual build and artifact upload for debug APKs.
- `apk-release.yml`: Release build automation.
- `multi-package`: Publishing workflows for Python (PyPI), Web (NPM), and Android (Maven).

---
*Created for secure, private, and educational network management. Licensed under MIT.*
