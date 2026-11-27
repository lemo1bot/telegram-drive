# Amazon Appstore — Step-by-Step Submission Guide

For: **Telegram Drive v1.0.0** · Cost: **FREE** (Amazon charges nothing to
open the account or submit an app) · Time needed: about 30–45 minutes ·
You need: a computer or phone browser, the APK from your GitHub Release,
and the files in this pack.

Every value you must type is ready in **LISTING_COPY.md** — just copy-paste.

---

## Step 0 — Before you start, keep these ready

1. The installable APK from your GitHub Release: **`TelegramDrive.apk`**
   (the debug-signed one — NOT the file with "unsigned" in its name).
2. **LISTING_COPY.md** open in another tab.
3. **PRIVACY_POLICY.md** published at a public URL (see Step 5 — do that first
   if you have not).
4. Screenshots and icon ready (see **ASSETS_CHECKLIST.md**).

---

## Step 1 — Create your FREE Amazon Developer account

1. Go to **https://developer.amazon.com** → **Sign in** → **Create your
   Amazon Developer account** (you can use your normal Amazon account).
2. Fill your name, email, and developer/company name. For "developer name",
   using your own name is fine as a first-time individual developer.
3. Accept the Amazon Appstore Developer agreement.
4. **Identity / tax / payment details:** Amazon may ask for identity
   verification and, for payouts (ad money is paid by Google AdMob, not
   Amazon — but Amazon still asks some account questions), tax information.
   Fill only YOUR real details, only on amazon.com's own pages. Only you can
   do this step — nobody else can do it for you, and never share OTPs or
   passwords with anyone to "help" with it.
5. There is **no fee**. If any page asks for money to open a developer
   account, you are on the wrong page — stop.

## Step 2 — Create the app

1. In the Amazon Developer Console, go to **Apps & Services → My Apps →
   Add a New App → Android**.
2. Fill:
   - **App title:** paste from LISTING_COPY.md —
     `Telegram Drive — Private Cloud Storage`
   - **App SKU:** type `telegram-drive-001` (any unique code you invent;
     users never see it).
   - **Category:** **Productivity** (or Utilities & Tools if shown).
   - **Support email:** `foriadahemed2000@gmail.com`
   - **Support website / Privacy policy URL:** see Step 5.
3. Save / continue.

## Step 3 — Description tab (copy-paste)

- **Short description:** paste the Amazon short description from
  LISTING_COPY.md.
- **Long description:** paste the full description from LISTING_COPY.md.
- **Keywords:** Amazon mainly uses your title + description for search, so
  no separate action is needed; if a keywords box appears, paste the
  keyword list from LISTING_COPY.md.
- **App icon:** upload the 512×512 icon (see ASSETS_CHECKLIST.md).
- **Screenshots:** upload your phone screenshots (see ASSETS_CHECKLIST.md).

## Step 4 — Upload the APK (Binary / APK tab)

1. Open the **APK File / Binary** section → **Upload APK** → choose
   **`TelegramDrive.apk`**.
2. Amazon reads the package name automatically — it must show
   `com.foriad.telegramdrive`, version 1.0.0.
3. **Device targeting:** leave the default (all Amazon devices + Android
   devices) unless the portal forces a choice — then tick phones/tablets.
   Note: the app needs a real Android phone with Telegram; Fire tablets
   without Google services can still run it, ads come via AdMob which may
   be limited on Fire devices — that is normal, not an error.
4. **Export / DRM questions if asked:** choose "No DRM" / free app,
   **App price: Free**.

## Step 5 — Privacy Policy URL (REQUIRED — do this before submitting)

Amazon requires a public web page link for the privacy policy. Free way:

1. Your code is being pushed to a GitHub repo named **telegram-drive**.
2. In that repo, add the file **PRIVACY_POLICY.md** (it is in this pack).
3. Easiest free page: turn on **GitHub Pages** for the repo
   (Repo → Settings → Pages → Source: main branch) — your policy URL becomes
   `https://lemo1bot.github.io/telegram-drive/PRIVACY_POLICY`
   (your GitHub username + repo name; check the exact link GitHub shows you).
   If Pages is troublesome, the raw file link also works as a URL:
   `https://github.com/lemo1bot/telegram-drive/blob/main/PRIVACY_POLICY.md`
4. Paste that URL into Amazon's **Privacy policy URL** field (and anywhere
   else a website/policy URL is asked).
5. Open the link in a private/incognito browser window to confirm strangers
   can read it without logging in. If they can't, the reviewer can't either.

## Step 6 — Content rating questionnaire (answer honestly like this)

Amazon asks questions about the app's content. Telegram Drive's truthful
answers:

- **Violence / sexual content / bad language in the app itself:** **No** to all.
  The app itself contains none of these. (Users store their own files in
  their own private group — the app does not provide content.)
- **Does the app contain advertising?** **YES** — it shows Google AdMob ads.
- **Is the app directed at / made for children?** **NO.** Do not tick any
  "designed for children/families" option.
- **Does the app share user location / personal data?** The app itself
  collects no personal data and has no servers (see the privacy policy).
  Answer data questions based on the app itself: **No data collected by
  the developer.** Ads by Google may use an advertising ID — that is
  covered by answering YES to advertising above.
- **Does the app let users communicate / share content with the public?**
  **No** — files go only to the user's OWN private Telegram group.
- **In-app purchases:** On Amazon, answer **No / not functional in this
  version** if it asks whether purchases work here — see the important
  note below. (The app does contain a Premium screen that explains the
  purchase is on Google Play; it cannot charge money on Amazon.)

Expected result: a rating like **Everyone / Guidance Suggested** because of
ads. That is normal.

## ⚠️ Important honest notes for Amazon

- **Premium ($10) will NOT work on Amazon.** The purchase uses **Google
  Play Billing**, which only works in apps installed from Google Play.
  In the Amazon version, the Premium screen shows a friendly message that
  purchases are available in the Google Play version. It will not crash
  and will not charge anyone. This is expected behaviour.
- **AdMob ads DO work on Amazon** (that is how the free version earns) —
  as long as your own AdMob IDs are in the build, not Google's test IDs.
  See the project README's AdMob section.
- **Do not click your own ads**, ever — Google bans AdMob accounts for that.

## Step 7 — Review & submit

1. Check every tab shows a green tick / "Complete".
2. Click **Submit App**.
3. **Review timeline:** Amazon usually reviews in **1–3 business days**
   (first submissions can take a bit longer). You get an email at
   foriadahemed2000@gmail.com with the result.
4. If rejected: read the reason calmly — the most common first-time reasons
   are a privacy policy link that does not open, screenshots that do not
   match the app, or a rating answer that conflicts with "contains ads".
   Fix exactly what they name, re-upload/re-answer, and submit again.
   Rejection is normal for first apps and costs nothing.

## Step 8 — After approval

- Your app page is live on the Amazon Appstore. Share its link from the
  console — do NOT promise users the $10 Premium purchase on Amazon; tell
  them: "Free with ads on Amazon — Premium is on Google Play."
- For every update: upload the new APK in the same app listing, paste new
  "What's New" text from LISTING_COPY.md style, submit again.
