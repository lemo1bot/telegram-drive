package com.foriad.telegramdrive

/**
 * ============================================================
 *  ADMOB ADS - THE ONE PLACE TO CHANGE YOUR AD IDs
 * ============================================================
 * Right now these are Google's OFFICIAL TEST IDs.
 * They work immediately, but you earn NO money from them.
 *
 * To earn money:
 *  1. Create an account at https://admob.google.com
 *  2. Create an app + a Banner ad unit + an Interstitial ad unit
 *  3. Replace BANNER_ID and INTERSTITIAL_ID below with YOUR IDs
 *  4. Replace the App ID in AndroidManifest.xml
 *     (look for: com.google.android.gms.ads.APPLICATION_ID)
 *     with YOUR AdMob App ID (looks like ca-app-pub-XXXX~YYYY)
 * Full step-by-step: see README.md -> "How to earn with AdMob"
 */
object AdsConfig {
    // Google official TEST banner ID - REPLACE with your own Banner ad unit ID
    const val BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

    // Google official TEST interstitial ID - REPLACE with your own Interstitial ad unit ID
    const val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    // Never show an interstitial more often than this (avoids annoying users / AdMob policy issues)
    const val INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60 * 1000L
}
