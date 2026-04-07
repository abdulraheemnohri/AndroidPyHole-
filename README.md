# PyHoleX: Global Mesh Edition (Ultimate High-Performance DNS)

PyHoleX is an enterprise-grade, decentralized DNS firewall for Android. By combining a native **Rust Engine**, **AI Heuristics**, and a **Global Mesh Intelligence** network, PyHoleX provides the most advanced privacy and security experience available for mobile devices.

## 🚀 Key Advantages (Mesh Edition)

- **Pure Rust Engine**: Benchmark-breaking 50k+ QPS with a minimal system footprint.
- **Decentralized Mesh Intelligence**: Anonymous, peer-to-peer threat sharing (Gossip Protocol) ensures you are protected by the community in real-time.
- **AI-Powered Prevention**: Real-time entropy and heuristic analysis to block 0-day malware and phishing.
- **Mobile Energy Optimization**: Tracks "Energy Saved" by preventing high-weight trackers and ad payloads from consuming battery and data.
- **DNS-over-HTTPS (DoH)**: Full blueprint for encrypted inbound and outbound DNS traffic.
- **Native Material 3 UI**: Modern, responsive Jetpack Compose interface with deep system integration.

## 🏗️ Architecture

- **Core (Rust)**: High-concurrency `tokio` runtime handling resolution, mesh sync, and AI analysis.
- **Frontend (Kotlin)**: Jetpack Compose mobile dashboard, battery-optimized foreground services, and VPNService orchestration.
- **P2P Layer**: Decentralized threat synchronization using a modern gossip protocol blueprint.

## 🛡️ Feature Stack

- **Global Category Filters**: Effortlessly toggle blocking for Ads, Social Media, Adult Content, and Malware.
- **Sustainability Dashboard**: Visualize your contribution to data and battery savings.
- **Granular App Control**: Monitor and restrict DNS queries on a per-application basis.
- **Audit Logs**: Deep-packet inspection logs with cryptographic reasoning for every block.

## 🛠️ Build & Development

### 1. Build Rust Core
```bash
cd rust-dns-engine
cargo build --release
```

### 2. Android Deployment
- Open in **Android Studio**.
- Ensure **NDK** and **Rust** toolchains are active.
- Deploy the **app** module to your Android device.

## 📦 CI/CD
Fully automated GitHub Actions for Rust testing, multi-arch APK building (aarch64, armv7), and blueprint-based package publishing.

---
*MIT Licensed | Secure. Decentralized. Sustainable.*
