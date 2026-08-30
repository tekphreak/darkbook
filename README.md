# Darkbook

A private, dark-themed diary app for Android. Everything is local and
encrypted — there's no account, no sync, and no network access except the
two explicit share actions you trigger yourself.

- Locked behind biometrics (with a salted-hash PIN fallback) on cold start
  and after backgrounding the app
- Entries are stored in a Room database wrapped with SQLCipher; the
  passphrase is random per-install and held in `EncryptedSharedPreferences`
  behind an Android Keystore master key
- Optional image attachment per entry (system Photo Picker, image copied
  into app-private storage and encrypted with `EncryptedFile`)
- Optional location capture, once at creation time, degrading gracefully
  if permission is denied or no fix is available
- Share a specific entry to Threads (with a review-first confirmation
  dialog), or long-press any entry in the list for a generic share sheet
- Six bundled font choices (Roboto, Roboto Flex, Roboto Mono, Roboto Serif,
  Noto Sans, Noto Serif), applied app-wide from Settings

See [SPEC.md](SPEC.md) for the full design spec, current data model, and a
couple of interesting bugs found (and fixed) along the way.

## Stack

Kotlin, Jetpack Compose (Material3), Room + SQLCipher, AndroidX Biometric,
Play Services location. Min SDK 26.

## Building

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # any JDK 17 works
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## License

MIT — see [LICENSE](LICENSE).
