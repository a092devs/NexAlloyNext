<div align="center">
  <img src="assets/icon.png" width="128" height="128" alt="NexAlloyNext Logo" />
  <h1>NexAlloyNext</h1>
  <p><strong>Next-generation root + LSPosed runtime bytecode patch manager for Android.</strong></p>
  <p>
    <a href="https://github.com/a092devs/NexAlloyNext/releases"><img src="https://img.shields.io/github/v/release/a092devs/NexAlloyNext?style=flat-square&color=0284C7&label=Release" alt="Latest Release" /></a>
    <img src="https://img.shields.io/badge/Android-8.1%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android 8.1+" />
    <img src="https://img.shields.io/badge/LSPosed-API%20101-blue?style=flat-square" alt="LSPosed API 101" />
    <img src="https://img.shields.io/badge/UI-Miuix%20%7C%20Material%203-blueviolet?style=flat-square" alt="Design Systems" />
    <img src="https://img.shields.io/badge/Icon-Hardware%20Silicon%20Matrix-0284C7?style=flat-square" alt="Hardware Silicon Matrix" />
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=flat-square" alt="GPL-3.0" /></a>
  </p>
  <p>
    <a href="#-getting-started">🚀 Getting Started</a> •
    <a href="#-key-features">⚡ Features</a> •
    <a href="#-curated-patch-repositories">🧩 Patch Bundles</a> •
    <a href="#-appearance--theming">🎨 Theming</a> •
    <a href="#-troubleshooting">🔍 Troubleshooting</a>
  </p>
</div>

---

## 📖 Overview

**NexAlloyNext** is an advanced Android application that applies modular runtime bytecode patches to supported applications via **LSPosed / libxposed (API 101)** and **DexKit**.

Unlike traditional ahead-of-time (AOT) patchers that decompile, modify, re-sign, and reinstall target APKs, **NexAlloyNext operates purely in-memory at process startup**:

- 🔒 **Zero APK Tampering:** Target applications remain official, unaltered on disk, and preserve their authentic Play Store signatures and Play Protect integrity.
- ⚡ **Dynamic DexKit Interception:** Obfuscated classes, methods, and fields are resolved on the fly via bytecode opcode pattern analysis and string fingerprinting.
- 🛡️ **Fail-Safe Isolation:** Hooks execute within protected `runCatching` runtime boundaries, preventing target app crashes if non-critical symbols change across updates.
- 📦 **Modular `.nxb` Bundles:** Patches are packaged into signed `.nxb` (NexAlloy Bundle) APK containers loaded directly into process memory — no APK installation required.

> [!IMPORTANT]
> - **Root & Framework Requirements:** Android 8.1+ (API 27) with root access and LSPosed or any modern framework implementing `libxposed` API 101.
> - **Security Boundary:** Patch bundles execute bytecode within target application processes. Always source bundles from trusted maintainers.
> - **Integrity Guarantee:** NexAlloy deliberately excludes banking, payment, and security-critical applications from patching.

---

## ⚡ Key Features

### 1. 🌐 Multi-Source Patch Aggregation & Provenance
- Add, update, and manage multiple independent patch repositories simultaneously.
- **Source Provenance Classification:** Clearly badges sources as **Official**, **NexAlloy**, **Community**, or **Custom**.
- **Conflict Detection:** Proactively flags applications whose patches are declared by more than one bundle before enabling.
- **Offline Caching:** Instant application startup using local metadata caching with pull-to-refresh synchronization.

### 2. 🎨 Dual Design Systems: Official Miuix & Material 3
- **Official Miuix Integration:** Built directly with the official [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix) library (`miuix-ui`, `miuix-blur`, `miuix-preference`, `miuix-icons`), delivering the authentic MIUI / HyperOS aesthetic.
- **Material 3 Mode:** Native AndroidX Material 3 design with dynamic Monet color theming on Android 12+.
- **Cohesive Surface Switching:** Changing the design system instantly transforms every screen, dialog, card, and navigation bar in unison.
- **Optional Floating Navigation Bar:** An opt-in Miuix arrangement featuring `FloatingNavigationBar` with real-time blur shaders on Android 13+ and safe fallbacks on older versions.

### 3. 🎯 Truthful Dynamic Compatibility Engine
- Version recommendations and compatibility badges are derived directly from the targets declared by active patch bundles at runtime:
  ```kotlin
  val allTargets: List<AppTarget> = if (matchingBundles.isNotEmpty()) {
      matchingBundles.flatMap { it.targets[packageName].orEmpty() }
  } else {
      known?.targets.orEmpty()
  }
  ```
- Uses semantic version evaluation with trailing zero normalization (`2026.42.0` vs `2026.42`).

### 4. 🔄 Honest Process Lifecycle & Force-Stop Control
- Distinguishes genuine root `am force-stop` termination from background process killing (`killBackgroundProcesses`), providing truthful feedback.
- Integrated 1-tap **App Info navigation** directly from the patch screen for manual force-stop on unrooted configurations.

---

## 🎨 Appearance & Theming

Configure via **Settings → Appearance**:

| Setting | Options | Description |
|---|---|---|
| **Colour Mode** | ☀️ Light / 🌙 Dark / ⚙️ Follow System | System-wide theme brightness, persisted across reboots |
| **Design System** | 📱 Material 3 / 💎 Miuix | Seamlessly switches the visual design language across all screens |
| **Floating Navigation Bar** | 🟢 On / ⚪ Off | *(Miuix only)* Detaches bottom bar with real-time blur shader (Android 13+) |

---

## 🚀 Getting Started

1. **📥 Install NexAlloyNext:** Download the signed release APK from [Releases](https://github.com/a092devs/NexAlloyNext/releases).
2. **🛡️ Configure LSPosed Scope:** Enable the **NexAlloyNext** module in LSPosed and check the scope for your target apps (e.g. YouTube, Spotify, Reddit, Instagram, Facebook, Telegram, X).
3. **📦 Install Patch Bundles:** Open NexAlloyNext → **Patches** tab. The official companion repository (`a092devs/NexAlloyNext-Patches`) is pre-configured; tap **Install** on `NexFusion Patches`.
4. **⚙️ Toggle Desired Patches:** Open the **Home** tab, tap any supported application card, and enable your preferred runtime hooks.
5. **🔄 Apply & Restart:** Tap **Force Stop** or use the 1-tap **App Info** shortcut to restart the target application and let LSPosed attach the updated bytecode hooks.

---

## 🧩 Curated Patch Repositories

The official companion patch repository is maintained at:
- 🔗 **[a092devs/NexAlloyNext-Patches](https://github.com/a092devs/NexAlloyNext-Patches)**

### Included Bundles:
- **`nexalloy`:** Streaming core patches for YouTube, YouTube Music, and Google Photos cloud spoofing.
- **`nexfusion` (`NexFusion Patches`):** Unified community runtime distribution covering all **17 supported application targets**:
  - 🎥 **Video & Audio:** YouTube, YouTube Music, Spotify
  - 💬 **Social & Communication:** Telegram, Telegram (Web), Instagram, Facebook, X (Twitter), Reddit, Discord, LinkedIn
  - 🛠️ **System & Productivity:** Google Photos, Gboard, Gmail, Google App (Discover), MiXplorer, Proton VPN

---

## 🛠️ Building from Source

### Prerequisites
- JDK 21 (Temurin or OpenJDK)
- Android SDK with API 37 platform tools (configured via `ANDROID_HOME` or `local.properties`)

```bash
# Clone the repository
git clone https://github.com/a092devs/NexAlloyNext.git
cd NexAlloyNext

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK (app/build/outputs/apk/debug/)
./gradlew assembleDebug
```

---

## 🔍 Troubleshooting

| Issue | Likely Cause | Solution |
|---|---|---|
| **Module shows Inactive** | Module not enabled in LSPosed | Open LSPosed Manager, enable NexAlloyNext, and reboot device. |
| **Patches not applying** | Target app not in scope | Check LSPosed scope; NexAlloy Home screen displays a yellow scope warning if missing. |
| **Compatibility Warning** | Installed app differs from target | Inspect the target app card in Home to check verified versions; hooks may still resolve dynamically. |
| **Blur missing on Floating Bar** | Android version limitation | Runtime shader blur requires Android 13+ (API 33+); older versions render an opaque surface. |
| **Need Support / Diagnostics** | Reporting an issue | Go to **Settings → Copy diagnostics** and attach the report when submitting a GitHub issue. |

---

## 🤝 Contributing

- Issues and pull requests are welcome. All contributions must adhere to the **runtime-only architecture**: zero APK modification, re-signing, or decompilation.
- Run `./gradlew testDebugUnitTest` prior to opening pull requests.
- Patch development occurs in the companion repository: [a092devs/NexAlloyNext-Patches](https://github.com/a092devs/NexAlloyNext-Patches).

---

## 📜 Credits & Acknowledgments

NexAlloyNext is developed and maintained by **a092devs**. We gratefully acknowledge the open-source projects, reverse engineers, and library authors whose research and tools empower our ecosystem:

| Project | Contribution |
|---|---|
| **[LSPosed](https://github.com/LSPosed/LSPosed)** | Core ART runtime hooking framework and official `libxposed` (API 101) |
| **[DexKit](https://luckypray.org/DexKit/en/)** | High-performance Dex bytecode pattern matching and opcode resolution |
| **[compose-miuix-ui](https://github.com/compose-miuix-ui/miuix)** | Official Compose Multiplatform MIUI / HyperOS design components by YuKongA |
| **[Morphe](https://morphe.software)** | Bytecode runtime hook patterns, fingerprint specifications, and shared utilities |
| **[ReVanced](https://revanced.app)** | Reverse engineering research, feature specifications, and community contributions |
| **[SysAdminDoc](https://github.com/SysAdminDoc)** | HushGram, HushFacebook, and HushTelegram stealth, ad-blocking, and downloaders |
| **[Piko](https://github.com/crimera/piko)** | Instagram and X (Twitter) runtime hook logic and downloader patterns |
| **[VenusIsJaded](https://github.com/VenusIsJaded/Venus-Patches)** | Discord native privacy target tables and font styling |
| **[heyymichii](https://github.com/heyymichii/michii-patches)** | LinkedIn share-link cleaning, direct navigation, and tracking removal |
| **[ak800i](https://github.com/ak800i/mixplorer-patches-for-morphe)** | MiXplorer scoped-storage file sharing fix for Android 11+ |
| **[jkennethcarino](https://github.com/jkennethcarino/adobo)** | Reddit screenshot-banner suppression, link sanitization, and UI hooks |
| **[gnadgnaoh](https://github.com/gnadgnaoh/NexAlloy-XES)** | Gmail inbox ad removal and Google Discover feed ad filtering |
| **[2eno](https://github.com/2eno)** | Early contributions to manager tooling, Spotify hooks, and Google Ad filters |

> [!NOTE]
> **Attribution Policy:** All ports, adaptations, and integrated specifications record upstream repository URLs and maintain original author copyright and license notices. Code derived from GPL-3.0 projects remains licensed under GPL-3.0.

---

## ⚖️ License

NexAlloyNext is open-source software developed by **a092devs** and licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE). Third-party libraries, ported feature specifications, and upstream dependencies retain their respective licenses and attribution.
