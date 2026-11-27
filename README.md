# Telegram Drive — Android App

Use **Telegram as your Google Drive**. Connect your own free Telegram bot to a
private Telegram group that only you can see, and this app sends your files,
photos, notes and links into that group. Browse, search, organise into folders
and download them back — like a drive, but the storage is Telegram's.

- No server backend. The app talks **directly** to the Telegram Bot API
  (`https://api.telegram.org`) from your phone.
- No account sign-up in the app. You bring your own bot token + group Chat ID.
- Beginner-proof setup wizard: one step at a time, copy buttons, live token
  check, and automatic group detection.

## Features

- 3-step setup wizard (BotFather → private group → Connect)
  - Tap-to-copy `/newbot`, one-tap open @BotFather
  - **Check my token** — live `getMe` check, shows your bot's name
  - **Find my group automatically** — polls `getUpdates` after you send a
    message in the group; manual Chat ID entry as a fallback
  - Connect posts a confirmation message into your group
- Drive UI: file list / grid toggle, search, folders (create + filter)
- Upload any file from the Android file picker, up to **50 MB** (Telegram bot limit)
  - Images use `sendPhoto`, everything else uses `sendDocument` (stored unchanged)
- Download / open via `getFile`
- Notes & links composer (`sendMessage`)
- Long-press a file to delete it (also deletes the Telegram message)
- **Premium (⭐ Go Premium button):** $10 one-time purchase removes all ads
  forever, with Restore Purchase — Google Play version only (see below)
- Bot token, Chat ID and the local file index are stored **only on the device**
  (SharedPreferences). Files themselves live only in your Telegram group.

## Ads (AdMob) — how earning works in this app

The app includes the Google Mobile Ads SDK with:

- A **banner ad** at the bottom of the Drive screen (never on setup screens)
- An **interstitial ad** after a successful upload, capped to **at most once
  every 3 minutes**

By default it uses **Google's official TEST ad IDs** — ads will show, but you
earn **no money** until you replace them with your own IDs (next section).

**Premium users see no ads at all** — see the next-but-one section.

## Premium — $10 forever (Google Play Billing)

Telegram Drive is free with ads. A **one-time $10 purchase** (not a
subscription) unlocks **Premium**: every ad is removed **permanently** —
no banner, no interstitials, forever, including after a reinstall or on a
new phone (via **Restore Purchase** with the same Google account).

How it works in the app:

- Drive screen → **⭐ Go Premium** button opens the Premium screen:
  free vs premium comparison, a **Buy** button, and a **Restore Purchase**
  button.
- The price on the Buy button is the **real localized price returned by
  Google Play** for the product (e.g. `$10.00` / `₹830.00`). Only if Play
  can't be reached does the app fall back to displaying "$10".
- The product is the non-consumable in-app product **`premium_lifetime`**
  (see `PremiumConfig.kt`). After a verified purchase the app
  **acknowledges** it (required for non-consumables) and **never consumes**
  it, stores `is_premium` locally (`Prefs.kt`), and from then on never
  loads or shows any ad. Restore uses `queryPurchasesAsync` for
  `premium_lifetime`, so ownership survives reinstalls/new phones.
- Code: `BillingManager.kt` (Billing Library 7.1.1) + `PremiumConfig.kt`.

### Create the $10 product in Play Console — exact steps

Do this once, after the app exists in Play Console (a closed-testing or
production track build uploaded is enough for the product to be testable
by license testers):

1. Open <https://play.google.com/console> → select **Telegram Drive**.
2. Left menu: **Monetize → In-app products** (under "Products").
3. Click **Create product**.
4. **Product ID:** `premium_lifetime` — must match `PremiumConfig.PRODUCT_ID`
   exactly. (Product IDs can never be changed or reused — type carefully.)
5. **Name:** `Premium — Lifetime (No Ads)`. **Description:** e.g.
   "One-time purchase. Removes all ads forever. Not a subscription."
6. **Price:** click **Set price** → enter **$10** (USD) → **Apply prices**
   (Google auto-converts to other countries; you can edit per-country).
7. Save, then click **Activate** — the product must be **Active**, not Draft,
   or the app will report it as "not found".
8. To test without paying: Play Console → **Settings → License testing** →
   add your Gmail as a license tester, then buy from a test build — test
   purchases are free and can be refunded/cancelled from the tester page.

### ⚠️ Sideload note — Premium purchases need the Google Play version

Google Play Billing only works when the app was **installed by Google
Play**. The APK from this repo's **GitHub Releases** is sideloaded, so in
that build the Premium screen shows a friendly message — *"Purchases are
available in the Google Play version of this app"* — instead of a buy
flow. It does not crash and it never fake-unlocks Premium. To sell
Premium, publish the app on Google Play (same package name
`com.foriad.telegramdrive`, product `premium_lifetime` as above).

## How to earn with AdMob — step by step

1. Go to <https://admob.google.com> and sign in with your Google account.
   Complete the AdMob account setup (country, payment info for payouts).
2. In AdMob: **Apps → Add app → Android → "This app is not on a supported app
   store yet"** (if you haven't published it) → name it **Telegram Drive**.
   AdMob gives you an **App ID** that looks like `ca-app-pub-1234567890~0987654321`.
3. Create ad units for that app:
   - **Ad unit → Banner** → name it `Drive Banner` → copy its ID
     (looks like `ca-app-pub-1234567890/1111111111`)
   - **Ad unit → Interstitial** → name it `After Upload` → copy its ID
     (looks like `ca-app-pub-1234567890/2222222222`)
4. Put YOUR IDs in the app — exactly **two files**:
   - **App ID:** `app/src/main/AndroidManifest.xml` — replace the value of
     `com.google.android.gms.ads.APPLICATION_ID`
     (currently the test ID `ca-app-pub-3940256099942544~3347511713`)
   - **Banner + Interstitial unit IDs:** 
     `app/src/main/java/com/foriad/telegramdrive/AdsConfig.kt` — replace
     `BANNER_ID` and `INTERSTITIAL_ID`
     (currently Google's test IDs `.../6300978111` and `.../1033173712`)
5. Commit, push, and tag a release (see below). New installs will now show
   **your** ads and earnings go to **your** AdMob account.
   - Note: AdMob may take some hours to start serving real ads to a new app,
     and you must follow AdMob policies (no clicking your own ads, etc.).

## Build & run in Android Studio

1. Install Android Studio (it includes JDK 17 + Android SDK 34).
2. Open this folder (`telegram-drive-android`) in Android Studio.
3. Let Gradle sync, connect a phone (USB debugging) or start an emulator.
4. Run ▶ the `app` configuration.
   Or from a terminal: `./gradlew assembleDebug` — APK appears at
   `app/build/outputs/apk/debug/app-debug.apk`.

Requirements: minSdk 26 (Android 8.0+), target/compileSdk 34, Kotlin, Material 3.

## Release on GitHub — step by step

1. Create a new (empty) repository on GitHub, e.g. `telegram-drive`.
2. In this folder:
   ```bash
   git init
   git add .
   git commit -m "Telegram Drive v1.0.0"
   git branch -M main
   git remote add origin https://github.com/<your-username>/telegram-drive.git
   git push -u origin main
   ```
   The **Build APK** workflow (`.github/workflows/build.yml`) runs automatically
   and attaches a debug APK under the run's **Artifacts**.
3. Publish a release:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
   The workflow builds **both** APKs and creates a **GitHub Release** for
   `v1.0.0` with two files in its **Assets**:
   - **`TelegramDrive.apk`** ← the one users download and install. It is the
     debug build renamed: debug-signed, so it installs on any phone
     (users may need to allow "Install unknown apps"). The Release notes
     explain this.
   - `TelegramDrive-release-unsigned.apk` — for developers only (see below).
   Anyone can download them from your repo's **Releases** page.
4. Signing note: the release APK is **unsigned** (no keystore is committed —
   never commit a keystore), and Android will not install an unsigned APK,
   which is why the Release's installable file is the debug-signed build.
   For a store-ready signed release build instead:
   - create your own keystore (`keytool -genkeypair -keystore release.jks ...`),
     add it + its passwords as GitHub **Secrets**, and add a `signingConfigs`
     block in `app/build.gradle.kts` reading those secrets; or
   - publish through **Google Play**, which signs the app for you
     (and is required for Premium purchases — see the Premium section).

## Privacy

- Your files go **only** to your own private Telegram group, via your own bot.
- Your bot token is stored only in the app's private storage on your device and
  is sent only to `api.telegram.org`.
- Downloads use Telegram's own file URL returned by `getFile` — that is
  Telegram Bot API's only download mechanism.
- The developer of this app has no server and receives none of your files,
  token, or chat ID.

## Tech

Kotlin, single-module, Gradle Kotlin DSL, AndroidX, Material 3, OkHttp,
Google Mobile Ads SDK 23.2.0, Google Play Billing Library 7.1.1. Local
storage: SharedPreferences (token, chat ID, file index, folders, premium
flag) — no Room, to keep the build light and reliable on GitHub Actions.

## License

MIT — see [LICENSE](LICENSE).
