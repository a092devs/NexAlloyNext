# Changelog

All notable changes to NexAlloyNext are documented here. Releases are dispatched automatically on
every push to `main` for the version in `app/build.gradle.kts` (`versionName` with `versionCode`
substituted) when that version has no release yet. The section matching the release tag becomes
the release notes, falling back to generated commit notes when no section exists.

## 3.1.27

- Changelog Wrapping: soft-wrapped changelog lines are now merged back into their markdown block the way GitHub renders them, so a bullet or paragraph whose text spans several source lines shows as one wrapped block instead of continuation lines jumping to the left margin.

## 3.1.26

- Changelog Rendering: release notes and bundle changelogs in the dialogs now render their inline markdown instead of showing the raw markers — `code` runs as a monospace chip, **bold**, *italic* and ~~strikethrough~~ merge with the body text, and markdown links plus bare URLs become tappable — so backticks around package, bundle and file names no longer leak into the dialog.

## 3.1.25

- X Bundle Mapping: the supported-app catalog now points X (`com.twitter.android`) at the `a092devs Patches` bundle and its `12.35.0-prod.01` target instead of the removed `Piko Patches` bundle (`12.19.1-release.0`); the README's bundled-app list documents Instagram and X under `a092devs`.

## 3.1.24

- Compact Floating Bar Setting: the Miuix-only floating navigation toggle is now labelled "Floating bar" with a single-line description ("Floats over the content with Miuix blur; off by default.") instead of the oversized title and two-line summary.

## 3.1.23

- Settings Cleanup: the Diagnostics entry and its copyable system/bundle dump were removed from Settings, and Clear DexKit cache moved under General. The framework name and version are no longer surfaced anywhere in the UI.
- Shorter Tagline: the description at the bottom of Settings now reads "NexAlloyNext applies patch bundles from patch sources. Only add sources you trust."

## 3.1.22

- Status Card App Version: the home screen's "Module is active" card now shows the running NexAlloyNext version instead of the Xposed framework name and version (like "Vector 1.9.4"), so screenshots and bug reports name the module build that is actually in use; the framework is still listed in Diagnostics.

## 3.1.21

- Installed-Only App List: the home catalog now lists only applications that are actually installed on the device, so bundle-supported apps that are not installed — for example YouTube Music while the nexalloy bundle is imported — no longer appear under "Supported Apps (Not Installed)". The Not Installed filter chip and the unused catalog helpers behind it were removed as well; the app catalog itself is unchanged for per-app screens and diagnostics.

## 3.1.20

- Patcher Resilience: a patch or bundle that throws while applying no longer aborts the rest of the session, and the fingerprint cache is now persisted even then; previously an escaping exception skipped the cache write, so every following start of the affected app re-ran every fingerprint match and showed the initialization popup again.
- Readable Patch Failures: a failed patch is now logged with its bundle and patch name plus its exception on release builds, and the "Error while applying the following patches" toast is only shown when the set of failing patches changes instead of on every start of the affected app (which previously tripped the system toast quota and got the message discarded entirely).
- Missing-Anchors Reporting: a fingerprint that finds no anchor now raises a named FingerprintNotFoundException instead of surfacing as an anonymous NullPointerException from DexKit's cache layer.
- Check for Updates Feedback: tapping Check for updates now shows an inline "Checking for updates…" state right away and always reports the outcome in a snackbar — including "a new NexAlloy version is available" and "patch updates are available" — where the Settings screen previously stayed silent whenever something was found.
- Release Diagnostics: the per-bundle cache decision ("using cache: true/false"), the applied summary and failures are logged on release builds too, so a device can be audited with logcat alone.

## 3.1.19

- Patcher Cache: fingerprint caches are now reused by debug builds as well, so the companion module stops re-running every fingerprint match each time a patched app starts; the cache key already covers the app, the NexAlloy build and the bundle versions, and Clear patcher cache stays as the manual escape hatch.
- Miuix Update Feedback: the Miuix presentation now surfaces one-off messages, like the result of Check for updates, source refreshes and install failures, in a swipe-dismissible Miuix snackbar anchored above the navigation bar; previously only the Material presentation had a message surface, which made Check for updates look like it did nothing.

## 3.1.18

- Search Bar Alignment: Redesigned `ThemedSearchField` with a centered capsule layout ensuring the search glyph and placeholder/input text share the exact same vertical center line without offset in both MIUIX and Material 3.
- Floating Navigation Bar: Eliminated the sliding animation across navigation tabs in favor of instant, smooth in-place selection pill transitions.
- Module Active Status Card: Redesigned the home screen status card with a lush emerald green container, fine accent border, circular checkmark badge, and active status chip when the LSPosed module is loaded.
- Compact Card Action Buttons: Replaced oversized patch source action buttons on `SourceCard` with sleek, proportional compact pill buttons (`ThemedCompactFilledButton` and `ThemedCompactTonalButton`).
- Changelog Readability: Implemented `ChangelogText` with clean markdown heading typography, accent bullet lists, and scrollable container for release notes dialogs.

## 3.1.17

- Companion Patch Catalog Alignment:
  * Migrated Instagram (`com.instagram.android`) default patch source to `a092devs Patches` with verified compatibility targets `451.0.0.53.80`, `450.0.0.46.79`, and `447.0.0.55.81` ported from `SysAdminDoc/HushGram`.
  * Expanded Facebook (`com.facebook.katana`) target recommendations to include `582.0.0.50.54` and `581.0.0.45.54` ported from `SysAdminDoc/Hushfacebook`.
- Repository & Pipeline Maintenance:
  * Synced `.gitignore` rules across modules and ensured automated release pipelines trigger cleanly on pushed tags without commit squashing.

## 3.1.16

- In-App Updater: Directly download and launch package installation for NexAlloyNext releases inside the app without external browser redirection, using `FileProvider` and background progress reporting.
- Supported Uninstalled Apps Catalog: Ensure all applications supported across patch bundles (such as YouTube Music, Proton VPN, Spotify, etc.) are discovered and displayed in the app catalog with verified recommendations even before installation.
- Miuix & HyperOS Theme Consistency: Replace hardcoded page backgrounds with `MiuixTheme.colorScheme.background`, replace Bootstrap badge colors with adaptive translucent squircle chips in dark/light themes, wrap patch toggle preferences in rounded `MiuixCard` groups, and distinguish primary vs secondary buttons.

## 3.1.15

- Silence routine debug toasts and restrict initialization toast notifications to the main application process (`isMainProcess()`) to eliminate spam across multi-process application startups.
- Refine Miuix floating navigation bar touch targets to 92.dp x 64.dp with 80.dp x 52.dp concentric selection pill and 12.dp bottom inset for comfortable navigation.
- Remove redundant card border outlines (`cardBorder = null`) in Miuix design system theme configuration.
- Upgrade in-app update checker SemVer comparison using `VersionComparator` to correctly evaluate release tags containing zero patch versions (e.g. `3.x.0`).
- Update companion patch catalog descriptions and attributions for Proton VPN, PixelBoard Gboard features, and new YouTube player controls.

## 3.1.14

- Fix Miuix (HyperOS design) text and icon visibility: Material text and icons inside the Miuix
  presentation followed the Miuix palette instead of falling back to black, so card titles,
  filter chips, search field icons and button labels were hard to read.
- Raise the low-contrast Miuix pairings used by filter chips, the search placeholder and text
  field labels to readable roles; section titles are slightly brighter.
- Show labels in the optional floating navigation bar, with the selected item emphasised.
- Paint the Miuix page background from the app's own light/dark mode so forcing a theme no
  longer clashes with the system window background.
- Add selectable colour schemes (dynamic colour on supported devices, or a fixed accent such as
  violet, emerald or amber) applied to both design systems.
- Add animated screen transitions and a sliding selection indicator in the floating navigation
  bar; align paddings and typography across the Material 3 and Miuix presentations.
- Rebuild the Miuix tab selector and the floating-bar selection pill with concentric, evenly
  inset shapes so they show no stray border or clipped corner while sliding.
- Publish releases automatically: a push to `main` dispatches the release workflow for the
  version in `app/build.gradle.kts` whenever that version has no release yet.

## 3.1.12 and earlier

See the GitHub Releases page for earlier release notes.
