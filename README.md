<div align="center">
  <h1>NexAlloyNext</h1>
  <p><strong>Next-generation root + LSPosed runtime patch manager for Android.</strong></p>
</div>

NexAlloyNext is an Android application that applies modular runtime bytecode patches to supported applications via LSPosed / libxposed (API 101) and DexKit.

Unlike traditional ahead-of-time (AOT) patchers that decompile, modify, re-sign, and reinstall target APKs, **NexAlloyNext operates purely in-memory at process startup**:
- **No APK Modifications:** Your target applications remain official, untampered, and preserve their original signatures and integrity.
- **Dynamic Bytecode Interception:** Methods and fields are resolved on-the-fly using DexKit opcode and string pattern matching.
- **Modular `.nxb` Bundles:** Patches are packaged into portable `.nxb` (NexAlloy Bundle) files installed directly from independent GitHub repositories or bundle feeds. A `.nxb` is a signed APK container that LSPosed loads in-process — it is never installed as an app.

>[!IMPORTANT]
> - **Requirements:** Android 8.1+ (API 27) with root access and LSPosed or a modern framework implementing `libxposed` API 101.
> - **Security:** Patch bundles execute bytecode within target processes. Only install patch sources you trust.
> - **Integrity:** NexAlloy does not patch, rebuild, or re-sign banking, payment, or security-critical applications.

---

## Key Features

### 1. Multi-Source Patch Aggregation & Provenance
- Add, update, and manage multiple independent patch repositories simultaneously.
- **Source Provenance Classification:** Clearly badges sources as **Official**, **NexAlloy**, **Community**, or **Custom**.
- **Conflict Detection:** Flags applications whose patches are provided by more than one bundle, so overlapping functionality is visible before enabling anything.
- **Offline Caching:** Fast startup using local release metadata caching with seamless manual or pull-to-refresh synchronization.

### 2. Two Design Systems: Official Miuix & Material 3
- **Official Miuix integration:** Built directly with the official [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix) library (`top.yukonga.miuix.kmp:miuix-ui`, `miuix-blur`, `miuix-preference`, `miuix-icons`), the Compose implementation of the MIUI / HyperOS design language.
- **Material 3 mode:** Official AndroidX Material 3 components, including dynamic colour on Android 12+.
- **Every surface switches together:** The selected design system drives screens, cards, dialogs, menus, search fields, switches, tabs, and bottom navigation — the two systems are never mixed within one active theme.
- **Colour mode:** Light, Dark, or follow the system, persisted across restarts.
- **Standard navigation by default:** In Miuix mode the app uses the official Miuix `NavigationBar`.
- **Optional floating navigation bar:** An opt-in Miuix-only arrangement using the official `FloatingNavigationBar` and `FloatingNavigationBarItem`. It is **off by default** and enabled from `Appearance → Floating navigation bar` (see below).
- **Evidence-based module status card:** Positioned directly beneath the top app bar on the Home dashboard, reporting the module's activation state and the detected Xposed framework name/version when the framework service is reachable, plus actionable warnings (missing scope entries, updates). States that cannot be verified are shown honestly instead of being assumed active.

### 3. Truthful Compatibility Engine
- Semantic version comparison with trailing zero normalization and fail-safe SemVer evaluation.
- No hardcoded "tested" claims: version recommendations reflect the targets declared by the loaded bundles.

---

## Appearance & Theming

Settings → **Appearance** contains:

| Setting | Options | Notes |
|---|---|---|
| Colour mode | Light / Dark / System | Applies to both design systems |
| Design system | Material 3 / Miuix | Switches every screen and component immediately |
| Floating navigation bar | On / Off | **Miuix only, off by default** |

The floating navigation bar detaches the bottom bar from the layout over the content. On Android 13+ (API 33+) it uses the official `miuix-blur` runtime shader backdrop; on older Android versions it falls back to an opaque surface container without raising the app's minimum SDK and without crashing. When it is disabled, the standard Miuix `NavigationBar` occupies its usual place and no leftover spacing remains. In Material 3 mode the option is not applicable and is not shown.

---

## Getting Started

1. **Install NexAlloyNext:** Download the latest APK from [Releases](https://github.com/a092devs/NexAlloyNext/releases) and enable the module in LSPosed.
2. **Configure Scope:** Ensure the target applications (e.g. YouTube, YouTube Music, Spotify, Reddit, Google Photos, Instagram, X) are included in the module's LSPosed scope.
3. **Install Bundles:** Navigate to the **Patches** tab. The curated repository (`a092devs/NexAlloyNext-Patches`) is preconfigured; tap **Install**.
4. **Customize Hooks:** Select an app on the **Home** tab to toggle individual patches (e.g. ad blocking, background playback, SponsorBlock).
5. **Apply:** Force stop the target application to let LSPosed attach the updated hooks.

---

## Curated Patch Repositories

The official companion patch repository is maintained at:
- [a092devs/NexAlloyNext-Patches](https://github.com/a092devs/NexAlloyNext-Patches)

Bundles provided include:
- `nexalloy`: YouTube, YouTube Music, Reddit, and Google Photos.
- `piko`: Instagram and X (Twitter) privacy, ad-blocking, and media downloaders.
- `2eno`: Spotify ad muting/UI customization and Instagram Stealth Follow.
- `a092devs`: community-ported patches for Discord, Facebook, Gboard (PixelBoard rounded keycaps & AI flag ports), LinkedIn, MiXplorer, Proton VPN, and Reddit.

---

## Compatibility & Limitations

- **Version recommendations** come from each bundle's declared targets — the versions its maintainers verified the hooks against. A newer installed app version is not guaranteed to work; the app shows the bundle's target and lets you decide.
- **Runtime-only scope:** Features that require manifest, resource, or bytecode changes that cannot be reproduced through runtime hooks are intentionally not implemented. NexAlloy will not fall back to APK patching.
- **Excluded targets:** Banking, payment, and security-critical applications are deliberately unsupported, even where upstream patch projects target them.
- **Per-app feasibility** (including features that were investigated and found runtime-incompatible) is documented in the patches repository's [installed-app audit](https://github.com/a092devs/NexAlloyNext-Patches/blob/main/docs/installed-app-patch-audit.md).

---

## Building from Source

Prerequisites: JDK 21 and an Android SDK with API 37 platform/build-tools (set `ANDROID_HOME` or `local.properties`).

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

## Troubleshooting

- **Module shows as inactive:** Make sure NexAlloyNext is enabled in LSPosed and the framework is running. If you hid the launcher icon, open the app from LSPosed's module list (Settings → Hide launcher icon explains this). States that cannot be verified are reported as unknown rather than active.
- **Patches do not apply to an app:** Force stop the target app after changing its patches. Confirm the app is inside the module's LSPosed scope (`Home` shows a scope warning with a *Request scope* action).
- **An app shows a compatibility warning:** The installed version differs from the bundle's verified targets. Check the app detail page for the target version before reporting a bug.
- **Floating navigation bar has no blur:** Blur requires Android 13+ (API 33+). Below that, the bar renders with an opaque container — this is the intended fallback.
- **A patch source fails to refresh:** Failed refreshes keep the last cached metadata and show the error on the source card; retry or re-add the source.
- **Reporting a bug:** Use `Settings → Copy diagnostics` and attach the output.

---

## Contributing

- Issues and pull requests are welcome. Keep the runtime-only architecture: no APK patching, rebuilding, or re-signing, and no shipping of proprietary or reference APKs.
- Run `./gradlew testDebugUnitTest` before opening a pull request.
- Patch development lives in the companion repository: [a092devs/NexAlloyNext-Patches](https://github.com/a092devs/NexAlloyNext-Patches).

---

## Credits & Acknowledgments

- [2eno/NexAlloyNext](https://github.com/2eno/NexAlloyNext): The upstream project this repository is built upon; its consolidated history forms the base of this repository
- [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix): Official Compose Multiplatform implementation of the MIUI / HyperOS design language by YuKongA
- [NexAlloy](https://github.com/NexAlloy/NexAlloy): The foundational LSPosed patcher concept by ChsBuffer
- [DexKit](https://luckypray.org/DexKit/en/): High-performance Dex runtime analysis library
- [LSPosed](https://github.com/LSPosed/LSPosed): ART hooking framework and libxposed API 101
- [Morphe](https://morphe.software): Fingerprint API inspiration and bytecode hook specifications

---

## License

NexAlloyNext is licensed under the GNU General Public License v3.0 — see [LICENSE](LICENSE). Third-party libraries and ported feature specifications keep their own licenses and credits, listed above.
