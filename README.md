# AndroidPyHole: The Ultimate Ad-Blocking Experience on Android

AndroidPyHole is a comprehensive, feature-rich Pi-hole clone built specifically for Android. It combines the power of a Python-based DNS engine with the accessibility of an Android app, providing enterprise-grade network security and ad-blocking without the need for external hardware.

## 🌟 Features Breakdown

### 🛠️ Core Engine
- **Asynchronous DNS Handling**: High-performance resolution using `asyncio` and `aiodns`.
- **Rootless Operation**: Seamlessly intercepts device traffic using the Android VPNService API.
- **Rooted Support**: Direct binding to port 53 for full system-wide coverage.
- **Automated Gravity Updates**: Daily blocklist synchronization to keep your protection fresh.

### 📊 Advanced Dashboard & Analytics
- **Live Monitoring**: Real-time stats for total queries, blocks, and percentage blocked.
- **Trend Visualizations**: Beautiful 24-hour traffic charts and advanced query type distributions.
- **Detailed Query Logs**: Filterable, color-coded history of all DNS activity.
- **Client Intelligence**: Monitor traffic per device and assign custom tags/groups.

### 🔒 Privacy & Security
- **Dashboard Authentication**: Secure your statistics and settings with a custom password.
- **Multiple Privacy Modes**:
    - **Show All**: Default full-visibility mode.
    - **Anonymous**: Masks domains and client IPs in all logs and stats.
    - **Hide All**: Disables all data visualization for maximum privacy.
- **Encrypted Upstreams**: Full blueprint for DNS-over-HTTPS (DoH) and DNS-over-TLS (DoT).

### ⚙️ Full Management Control
- **Whitelist/Blacklist**: Dedicated management for always-allowed or always-blocked domains.
- **Local DNS Records**: Custom hostname mapping (e.g., redirecting `pi.hole` to the local dashboard).
- **Upstream DNS Selection**: Easily switch between Google, Cloudflare, OpenDNS, or custom providers.
- **Configuration Management**: Effortlessly **Export** or **Import** your entire setup via JSON.

### 🎨 Premium User Experience
- **Modern UI**: Dark-mode primary theme (Obsidian Shield) with a toggleable light-mode option.
- **Responsive Web Dashboard**: Accessible from any local browser at `http://127.0.0.1:8080`.
- **Branded Splash Screen**: A professional entry experience for the Android application.

## 🚀 Quick Start (Build Instructions)

1. **Setup Assets**: Run `./build_scripts/setup_python.sh` to initialize assets.
2. **Open in Android Studio**: Sync Gradle and ensure all dependencies are met.
3. **Build APK**: Run `./gradlew assembleDebug` or use the Build menu.
4. **Deploy**: Install the APK on your device and launch "PYHOLE".

## 📦 CI/CD
- **Testing**: Automated unit tests for all Python backend components.
- **Publishing**: Workflows included for releasing Python packages, NPM assets, and Android libraries.

---
*MIT Licensed | Created for private and educational network security.*
