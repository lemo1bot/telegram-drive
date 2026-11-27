# Telegram Drive

**Your private cloud drive, powered by Telegram.**

Telegram Drive turns a private Telegram group into your personal cloud storage. Connect your own Telegram bot in a simple 3-step wizard, then send photos, videos, documents and notes to the app — everything is stored in *your* private group, on *your* Telegram account. No extra cloud account, no storage subscription.

## Download

Get the latest Android APK from the [**Releases**](https://github.com/lemo1bot/telegram-drive/releases) page:

1. Download **TelegramDrive.apk** from the Assets section of the latest release.
2. Open it on your phone and allow **Install unknown apps** if Android asks.
3. Open Telegram Drive and follow the 3-step setup.

## Getting started

The app guides you through everything, one step at a time:

1. **Create a bot** — talk to [@BotFather](https://t.me/BotFather) in Telegram, send `/newbot`, and copy the token he gives you. The app has a copy button and checks the token for you.
2. **Create a private group** in Telegram, add your bot to it as an admin, and send any message in the group. The app finds your group automatically.
3. **Connect** — the app confirms the connection with a message in your group. Done.

## Features

- 📁 **Drive-style storage** — files, folders, search, grid & list views
- ⬆️ **Upload anything** — photos, videos, PDFs, documents, APKs and more, up to 50 MB per file
- ⬇️ **Download anytime** — your files live in your Telegram group, available on every device where you use Telegram
- 📝 **Notes & links** — save text notes and links alongside your files
- 🔒 **Private by design** — your bot token is stored only on your phone, and files go directly from your phone to your own Telegram group. There is no Telegram Drive server in between
- ⭐ **Premium** — an optional one-time purchase removes all ads forever, with free restore on reinstall or a new phone

## How it works

Telegram Drive uses the official Telegram Bot API. When you upload a file, the app sends it as a message into your private group using your bot. Your file list is kept on your device, so browsing is instant, and downloads fetch the file back from Telegram. Deleting in the app also removes the message from the group.

## Privacy

- Your bot token and file index never leave your phone, except to talk to Telegram's own API.
- Your files are stored only in your private Telegram group, under your account, and are covered by Telegram's privacy policy.
- The app displays ads provided by Google AdMob; Google's advertising privacy practices apply to ads. Premium removes ads entirely.

See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for the full policy.

## Build from source

Requirements: Android Studio (or JDK 17 + Android SDK 34).

```bash
git clone https://github.com/lemo1bot/telegram-drive.git
cd telegram-drive
./gradlew assembleDebug
```

The project is a single-module Kotlin app (Material 3) with no backend.

## Tech

- Kotlin, AndroidX, Material 3
- Telegram Bot API (direct HTTPS, no server)
- Google Mobile Ads SDK (AdMob)
- Google Play Billing (Premium, Play version)

## Follow

Updates and new releases: **X — [@Bank_of_eth](https://x.com/Bank_of_eth)**

## License

MIT — see [LICENSE](LICENSE).
