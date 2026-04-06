# PyHoleX: The Next-Gen High-Performance Android DNS Firewall

PyHoleX is a complete re-engineering of the Pi-hole concept for the Android ecosystem. By utilizing a **Native Rust DNS Engine**, PyHoleX achieves 10x faster resolution speeds, minimal battery consumption, and a lightweight memory footprint, making it the ultimate privacy tool for mobile devices.

## 🚀 Key Advantages

- **Rust-Powered DNS Core**: High-concurrency packet processing with `tokio` and O(1) domain lookups.
- **Ultra-Efficiency**: Designed specifically for mobile constraints—low CPU/RAM usage and battery optimization.
- **Native Android UI**: Built with **Jetpack Compose** for a modern, fluid, and responsive user experience.
- **Rootless Interception**: Uses the Android VPNService API to provide system-wide protection without requiring root.
- **Integrated Intelligence**: Native SQLite logging and real-time statistics API.

## 🏗️ Architecture Overview

- **Android App (Kotlin/Compose)**: Modern frontend that manages the engine lifecycle and displays real-time analytics.
- **Rust DNS Engine**: A standalone high-performance binary that handles all DNS traffic, filtering, and logging.
- **Communication Bridge**: Secure internal API (Warp) for high-speed data exchange between the UI and Engine.

## 🛡️ Core Features

- **Advanced Ad/Tracker Blocking**: Leveraging high-quality blocklists with instantaneous lookups.
- **Malware Protection**: Integrated threat feeds to block malicious C2 and phishing domains.
- **Customizable Control**: Whitelist, Blacklist, and Regex-based filtering.
- **Network Insights**: Detailed query logs and hourly traffic visualizations.
- **Security**: Support for Encrypted DNS (DoH/DoT) upstreams.

## 🛠️ Build & Installation

### 1. Build Rust Engine
```bash
cd rust-dns-engine
cargo build --release
```

### 2. Android Studio Deployment
1. Open the root project in **Android Studio**.
2. Sync Gradle (configured for AGP 8.2.2).
3. Build and Run the **app** module.

## 📦 CI/CD Pipeline
- **testing.yml**: Automated Rust engine unit testing and Android build verification.
- **apk-debug.yml**: Automated APK building for multiple Android architectures (aarch64, armv7, etc.).

---
*MIT Licensed | Optimized for Speed, Privacy, and Performance.*
