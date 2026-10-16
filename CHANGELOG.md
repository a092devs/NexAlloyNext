# Changelog

All notable changes to NexAlloyNext are documented here. Releases are dispatched automatically on
every push to `main` for the version in `app/build.gradle.kts` (`versionName` with `versionCode`
substituted) when that version has no release yet. The section matching the release tag becomes
the release notes, falling back to generated commit notes when no section exists.

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
