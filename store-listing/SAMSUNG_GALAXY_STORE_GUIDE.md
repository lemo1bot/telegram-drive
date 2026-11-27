# Samsung Galaxy Store — Step-by-Step Submission Guide

For: **Telegram Drive v1.0.0** · Cost: **FREE** to register and submit ·
Time needed: about 30–45 minutes · Big plus: Galaxy Store is already on
almost every Samsung phone, and Samsung phones are everywhere in India.

Every value you must type is ready in **LISTING_COPY.md** — just copy-paste.

---

## Honest warning before you start — read this first

1. **Seller types:** Samsung has different seller types. A **free app with
   ads can be published by a regular (non-commercial) seller** — you do not
   need a company. Samsung may ask you to upgrade to **Commercial Seller**
   status only if you want to sell paid apps or in-app items **through
   Samsung's own payment system**, with extra identity, bank, and tax
   verification. Telegram Drive does NOT use Samsung payments, so start as
   a normal free seller.
2. **Premium ($10) will NOT work on Samsung either.** The purchase uses
   **Google Play Billing**, which only works in apps installed from Google
   Play. In the Galaxy Store version, the Premium screen politely says the
   purchase is available on Google Play. Nobody can be charged on Samsung,
   and the app must not pretend otherwise.
3. **AdMob ads DO work** in the Galaxy Store version (with your own AdMob
   IDs in the build) — ads are how this version earns.

---

## Step 0 — Before you start, keep these ready

1. The installable APK from your GitHub Release: **`TelegramDrive.apk`**
   (NOT the "unsigned" file).
2. **LISTING_COPY.md** open in another tab.
3. **Privacy Policy published at a public URL** — do the GitHub Pages steps
   in the Amazon guide (Step 5) once; the same URL works for Samsung.
4. Icon + screenshots ready (see **ASSETS_CHECKLIST.md**).

---

## Step 1 — Create your FREE Samsung Seller account

1. Go to **https://seller.samsungapps.com** (the Samsung Galaxy Store
   Seller Portal) → **Sign up / Join**.
2. Sign in with a Samsung account (create one free if you don't have one —
   use an email you check, like foriadahemed2000@gmail.com, or your usual
   Samsung phone account).
3. Choose seller registration as an **individual**. Fill your real name,
   country **India**, and contact details.
4. **Verification:** Samsung may ask for identity verification (and for
   commercial sellers, bank/tax documents). For a free app, complete only
   what the portal asks for a free seller. Fill only YOUR real details,
   only on samsungapps.com / samsung.com pages. Only you can do this —
   never share OTPs, passwords, or documents with anyone "helping" you.
5. Accept the Galaxy Store seller terms. **No fee** is charged to register
   or to submit a free app. If a page demands payment just to register a
   free seller account, stop — you are in the wrong flow.

## Step 2 — Register the app (Add Application)

1. In Seller Portal: **Applications → Add Application → Android**.
2. Fill the basic info:
   - **App title:** paste from LISTING_COPY.md —
     `Telegram Drive — Private Cloud Storage`
   - **Default language:** English.
   - **Seller / support email:** `foriadahemed2000@gmail.com`
   - **Privacy Policy URL:** paste your published policy URL (Step 0.3).
3. Save to create the draft listing. Samsung assigns it an app ID — you
   don't need to do anything with it.

## Step 3 — Listing details (copy-paste)

- **Short description:** paste the Samsung short line from LISTING_COPY.md:
  `Your private drive inside Telegram — unlimited, simple, free.`
- **Full / long description:** paste the full description from
  LISTING_COPY.md.
- **Tags / keywords:** paste the keyword list from LISTING_COPY.md
  (Samsung allows a limited number of tags — if it makes you choose, keep
  these first: `telegram drive, cloud storage, private storage, file backup,
  unlimited storage`).
- **Category:** **Productivity** (fallback: **Utilities**).
- **Age rating / target:** General / everyone — **NOT for children**.
  Samsung runs its own rating questionnaire (Step 5).
- **What's New / release notes:** paste the v1.0.0 notes from
  LISTING_COPY.md.
- **Icon:** upload the 512×512 icon.
- **Screenshots:** upload phone screenshots (see ASSETS_CHECKLIST.md —
  Samsung wants at least 4; upload 5–6 good ones).

## Step 4 — Upload the binary (APK)

1. In the app's **Binary** section → **Add binary / Upload** → choose
   **`TelegramDrive.apk`**.
2. Confirm the portal reads: package `com.foriad.telegramdrive`,
   version 1.0.0.
3. **Device / screen support:** leave defaults (all Galaxy devices,
   phones). If asked about Google Mobile Services dependencies, answer
   honestly: the app uses AdMob and (non-functioning here) Play Billing
   code, but its core features — Telegram storage — work without Google
   services. Most Samsung phones have Google services anyway.
4. **Price:** **Free**. Do not enable Samsung in-app items — the app has
   none for Samsung.

## Step 5 — Content rating questionnaire (answer honestly like this)

- App itself contains violence / sexual content / bad language: **No**.
- App contains **advertising: YES** (Google AdMob).
- App is made for / targeted at children: **NO**.
- Users share content publicly or chat with strangers in the app: **No** —
  files go only to the user's own private Telegram group.
- Developer collects personal data: **No** — no developer servers; bot
  token and files stay between the user's phone and Telegram (link the
  privacy policy you already published).
- Location sharing by the app: **No**.

Expected outcome: an **All / Everyone-type rating with an ads notice**.
Normal for a free ad-supported utility.

## Step 6 — Review & submit

1. Run the portal's completeness check; fix anything it flags (usually a
   missing screenshot size or the privacy URL).
2. Click **Submit for review**.
3. **Review timeline:** typically **2–5 business days** for a first app
   (can be longer). Status changes appear in the Seller Portal, and
   Samsung emails you.
4. If rejected: the portal gives a reason file/message — most common for
   first-timers: screenshot problems, rating answers clashing with
   "contains ads", or the privacy policy link not opening for the
   reviewer. Fix exactly that, resubmit. It's free to resubmit.

## Step 7 — After approval

- Your app is live in Galaxy Store on Samsung phones. 🎉
- Tell users honestly: "Free with ads on Galaxy Store — the $10 lifetime
  Premium purchase is available in the Google Play version."
- Updates: upload the new APK to the same listing, update What's New,
  submit for review again.
