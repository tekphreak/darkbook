# Darkbook — Personal Diary App (Android)

A private, dark-themed journal app. Local-only storage, locked behind biometrics/PIN, with optional manual sharing to Threads.

## Stack
- Application ID / package: `com.tekphreak.darkbook` (Pro flavor: `com.tekphreak.darkbook.pro`)
- Kotlin + Jetpack Compose (Material3)
- Room DB with SQLCipher for encrypted local storage
- AndroidX Biometric library for lock screen
- Google Mobile Ads SDK (`free` flavor only)
- Min SDK 26, Target SDK latest stable

> **Status: implemented.** This doc started as the pre-build spec; the
> sections below have been updated to reflect what actually shipped,
> including several features added after the initial build (image
> attachments, location capture, font family + size selection, a generic
> long-press share, a splash-screen preview toggle in Settings, and a
> free/Pro build-flavor split with banner ads).

## Core Screens
1. **Lock Screen** — shown on cold start and on resume-from-background (after a 30s grace period, to avoid re-locking on quick app-switches)
   - Biometric prompt (`BiometricPrompt` API) as primary method
   - PIN fallback (4–6 digit, stored as a salted hash via `EncryptedSharedPreferences`, never plaintext)
   - "Forgot PIN" = no recovery by design (this is a private diary)
2. **Entry List** — reverse-chronological list of entries, black background (#000000), white text (#FFFFFF), minimal dividers; each row shows the date, the captured coordinates (if any), and a body preview. `free` flavor shows a banner ad pinned to the bottom of the screen; Pro shows none
3. **New/Edit Entry** — plain text editor, black bg / white text, with a paperclip icon (attach an image) and a floppy-disk icon (save) in the top bar, both line-drawing icons in `#808080`
4. **Entry Detail** — read view with the auto-stamped date/time + coordinates header, attached image (if any — tap to view full-screen, tap/back again to return), edit/share/delete actions
5. **Settings** — reachable via the entry list's overflow menu: long-press-to-share toggle, font family picker, entry text-size slider (with a live preview), and a "Show Splash Screen" preview button
6. **Splash** — the app's `LAUNCHER` activity per this folder's app-wide standard; auto-advances to the Entry List after 500ms on a real launch, but its content is also reused as an on-demand preview from Settings (shown until tapped again, or until Back is pressed)

## Features

### Auto-date/time stamping
- Every entry gets an immutable `createdAt` timestamp on creation (system clock, stored as epoch millis + device timezone)
- If edited, store a separate `editedAt` timestamp — don't overwrite `createdAt`
- Display format: `MM/dd/yyyy hh:mm a`, e.g. `08/30/2026 02:42 PM` (via `DateTimeFormatter`, shared across the list and detail screens so it can't drift)

### Location capture
- A snapshot of the device's coordinates is captured **once, at creation time only** — same immutability rule as `createdAt`; editing an entry never re-captures or overwrites it
- Uses `FusedLocationProviderClient.getCurrentLocation()` (Play Services location), balanced-power priority, with an 8s timeout so a save is never blocked waiting on a fix
- Location permission is requested **just-in-time**, the first time a *new* entry is saved — not on app launch, and never for edits
- Denying permission, having location services off, or timing out all degrade gracefully: the entry still saves, just without coordinates
- Displayed as `lat, lon` to 5 decimal places, directly below the date — same font size as the date, on both the list row and the Entry Detail header

### Image attachments
- One optional image per entry, picked via the system Photo Picker (`ActivityResultContracts.PickVisualMedia`) — no storage permission needed
- Copied into app-private storage and encrypted with `EncryptedFile`, using the same Keystore-backed master key as everything else — never stored in the clear, never touches shared/external storage
- Shown inline (cropped to a fixed height) in both the edit screen and Entry Detail; tapping it in Entry Detail toggles a full-screen, centered, non-cropped view — tap again (or press Back) to return
- Replacing or removing an image, or deleting the entry, deletes the now-orphaned encrypted file

### Encrypted storage
- Room database wrapped with SQLCipher (`net.zetetic:android-database-sqlcipher`)
- Passphrase for SQLCipher derived from Android Keystore-backed key, not hardcoded
- No entry content ever leaves the device except via the two explicit, user-initiated actions below (Threads share, generic long-press share) — nothing here logs, caches, or transmits it any other way

### Lock (Biometric + PIN fallback)
- Use `BiometricPrompt.PromptInfo` with `setNegativeButtonText` routing to PIN entry
- Lock triggers: app cold start, and `onStop()`/`onPause()` past a short grace period (e.g. 30s) to avoid re-locking on quick app-switches
- Store lock state in memory only, never persisted as "unlocked"

### Share to Threads
- A share button on the Entry Detail screen builds an `Intent.ACTION_SEND` with `type = "text/plain"` and (optionally) `setPackage("com.instagram.barcelona")` (Threads' package name — verify current value at build time since Meta has changed it before) to deep-link straight into Threads' composer, falling back to the system share sheet if Threads isn't installed
- **Important UX guardrail:** since this is a private diary, the share flow should:
  1. Never auto-fill Threads' own post text with the raw entry — pass the text into Threads' compose field so the user can review/edit it before posting
  2. Show a one-time (dismissible) confirmation dialog before the intent fires: *"This will open Threads with your entry text loaded. Review it before posting — diary entries can include things you don't mean to share."*
  3. Never log, cache, or transmit entry content anywhere else as part of this flow

### Long-press share (generic, list screen)
- A second, separate egress path: long-pressing an entry row in the Entry List opens the plain system share sheet (`ACTION_SEND`, no target package) with that entry's text — for dropping into Notes, email, Drive, etc., not specifically Threads
- Gated behind a Settings toggle ("Long press to export text"), **on by default**; turning it off disables the long-press gesture entirely

### Font selection
- Settings has a picker for the entry text's font family: Roboto, Roboto Flex, Roboto Mono, Roboto Serif, Noto Sans, Noto Serif
- Bundled locally as static font resources (no network fetch at runtime, consistent with the app's local-only ethos) and applied app-wide immediately on selection
- A separate slider (12sp–28sp, default 16sp) controls entry text *size*, with a live preview line in Settings. Deliberately isolated behind a dedicated `LocalEntryFontSize` composition local rather than `Typography.bodyLarge` — Material3's `MaterialTheme` wraps everything in `ProvideTextStyle(typography.bodyLarge)`, so overloading that slot made the *entire* Settings UI (checkboxes, radio labels) resize with the slider on the first pass. Only the three actual entry-text render sites (list row, edit screen, detail body) opt into the size

### Ads (free flavor) / Darkbook Pro
- Two product flavors from one codebase: `free` (`com.tekphreak.darkbook`, ad-supported) and `pro` (`com.tekphreak.darkbook.pro`, no ads) — two separate APKs/Play listings, not an in-app purchase
- `free` shows one AdMob banner, pinned to the bottom of the Entry List via Scaffold's `bottomBar` slot
- The Google Mobile Ads SDK, its manifest `APPLICATION_ID` meta-data, and the `INTERNET`/`ACCESS_NETWORK_STATE` permissions all live in flavor-specific source sets (`app/src/free/...`) — `pro` never links the ads SDK at all, not even as dead code
- Shared code (`EntryListScreen.kt`) calls one flavor-agnostic `AdBanner()` composable; each flavor supplies its own implementation (real `AdView` in `src/free`, a no-op in `src/pro`) so `pro`'s Kotlin compilation never references an AdMob class
- **Currently wired to Google's official test App ID and test banner ad unit ID** — both must be replaced with real IDs from `apps.admob.com` before either flavor ships (see `app/src/free/AndroidManifest.xml` and `app/src/free/java/.../ui/AdBanner.kt`)

## Data Model
```
Entry(
  id: Long (PK, autoincrement)
  createdAt: Long (epoch millis)
  editedAt: Long? (epoch millis, nullable)
  body: String
  imagePath: String? (filename within app-private encrypted image storage)
  latitude: Double? (captured once, at creation)
  longitude: Double? (captured once, at creation)
)
```
`imagePath`/`latitude`/`longitude` were added after the initial build (Room
migrations v1→v2 added `imagePath`, v2→v3 added the two location columns) —
current schema version is 3.

## Theme
- Background: `#000000`
- Text: `#FFFFFF`
- Secondary/hint text: `#888888`
- Accent (buttons, cursor): `#CCCCCC` or a single subdued color of choice
- No pure-white surfaces/cards — keep everything black to stay OLED-friendly; use subtle `#111111` elevation tints if needed for Compose surfaces

## Build order (completed)
1. Room + SQLCipher setup, Entry entity/DAO
2. Lock screen (biometric + PIN) gating navigation
3. Entry list + create/edit screens, wired to DB, auto-stamping
4. Entry detail screen
5. Threads share intent + confirmation dialog
6. Proper system Back-button handling for every screen (including the
   full-screen image and splash-preview overlays taking priority over the
   screen underneath them)
7. Image attachments (Photo Picker + `EncryptedFile` storage)
8. Location capture (Play Services, permission requested just-in-time)
9. Generic long-press share + font selection, both in Settings
10. Settings "Show Splash Screen" preview toggle
11. Entry text-size slider in Settings
12. `free`/`pro` product flavors + AdMob banner ad on the Entry List (free only)

Not done: wipe-and-reset after N failed PIN attempts (the app tracks a
failed-attempt counter in `PinManager` but nothing acts on it yet), and the
edit/delete confirmation dialogs only cover delete, not edit.

## Bugs found and fixed during implementation

- **Crash on first image attach**: `androidx.biometric:1.1.0` strictly pins
  `androidx.fragment:fragment` to `1.2.5`, whose `FragmentActivity` predates
  a fix in how the modern Activity Result API (`rememberLauncherForActivityResult`,
  used for the Photo Picker) generates request codes. Tapping the attach-image
  icon threw `IllegalArgumentException: Can only use lower 16 bits for
  requestCode` and crashed instantly. Fixed by forcing
  `androidx.fragment:fragment:1.6.2` via a `resolutionStrategy` in
  `app/build.gradle` — Gradle's transitive resolution otherwise keeps the
  lower version because biometric's constraint is `strictly`, which a plain
  higher version request elsewhere can't override.
- **Invisible splash title**: the "DARKBOOK" title `Text` never set an
  explicit color, and titleLarge's color is unspecified in this app's
  typography — with no `Surface` ancestor providing an ambient content
  color, Compose's fallback is black, rendering white-on-black as
  black-on-black. Invisible on the real splash too, just never noticed
  because it auto-advances in 500ms; only surfaced once the Settings
  "Show Splash Screen" preview kept it on screen long enough to see. Fixed
  by setting the title's color explicitly to `colorScheme.onBackground`.
- **Font-size slider resized the whole Settings screen**: making `Typography.bodyLarge`'s
  size itself configurable seemed like the obvious approach, but Material3's
  `MaterialTheme` wraps all content in `ProvideTextStyle(typography.bodyLarge)`,
  so every unstyled `Text()` in the app — including Settings' own checkboxes
  and radio labels — inherited the slider value. Fixed by moving the size
  behind a dedicated `LocalEntryFontSize` composition local that only the
  actual entry-text render sites read.
- **`play-services-ads` 24.x+ breaks kapt on this project's Kotlin 1.9.22**:
  releases from 24.6.0 up through the current 25.4.0 ship some submodules
  built with Kotlin 2.x metadata, which crashes kapt with
  `InvocationTargetException` (or, inconsistently, a "Compilation error" a
  step later — which task fails seems to depend on Gradle daemon/compile
  caching state, not something to rely on as a signal). 23.6.0 is clean —
  zero Kotlin-version warnings — and is what's pinned. Revisit this pin
  if/when the project's Kotlin version is upgraded off 1.9.22.

## Publishing to Google Play

Because `free` and `pro` are separate application IDs, they need **two
separate Play Console listings** (each goes through everything below on its
own — separate store listing, data safety form, testing track, etc.). Before
either one, **swap the AdMob test IDs for real ones** in
`app/src/free/AndroidManifest.xml` and `AdBanner.kt` — Google will reject (or
worse, flag the account for) an app shipping with test ad units.

1. **Google Play Console account** — one-time $25 registration at `play.google.com/console`, tied to a Google account. Google now requires new personal accounts to complete identity verification and a 20-tester/14-day closed testing track before a first app can go public.
2. **App signing** — generate an upload keystore (`keytool -genkeypair ...` or via Android Studio's "Generate Signed Bundle"), enroll in Play App Signing so Google holds the release key and you keep the upload key.
3. **Build the release artifact** — produce a signed `.aab` (Android App Bundle) per flavor, not a `.apk`: `./gradlew bundleFreeRelease` / `./gradlew bundleProRelease`.
4. **Store listing** — app name, short/full description, screenshots (phone + optional tablet), feature graphic, icon, category (likely "Lifestyle" or "Productivity"). Worth making the `free` and `pro` listings' copy clearly cross-reference each other ("Want an ad-free experience? Try Darkbook Pro").
5. **Privacy policy** — required, and non-negotiable for this app: it stores personal diary content and uses biometrics, and `free` serves ads. Host a simple policy page (even a static page on your own domain) stating what's stored (locally only), what's never transmitted, and that biometric data never leaves the device/OS layer — plus, for `free`, AdMob's standard disclosures about ad-serving and any device/advertising identifiers it accesses.
6. **Data safety form** — in Play Console, declare: biometric auth used for app access (not collected/transmitted); diary text, attached images, and precise location all stored locally and encrypted, never collected or shared with third parties or transmitted off-device; two optional user-initiated share actions (Threads share, generic long-press share) that only fire on explicit user action. Precise location (`ACCESS_FINE_LOCATION`) will also need a runtime-permission justification and a prominent in-app disclosure per Play's location policy, since it's requested from a non-map-centric app. For `free` specifically, also declare AdMob's data collection (advertising ID, device/network info) per its own disclosed practices.
7. **Content rating questionnaire** — fill out via Play Console (typically nets a low/all-ages rating for a plain journaling app).
8. **Testing track** — upload to Internal testing first, then Closed testing (this satisfies the 14-day/20-tester requirement) before Production.
9. **Rollout** — once testing requirements are met and the listing/data-safety/content-rating sections are complete, submit the release for review and promote to Production.

Since this is a personal/private-use app, closed testing with a small tester group (e.g. your own alt accounts or a few trusted people) is enough to clear Google's requirements without a public beta.
