package com.foriad.telegramdrive

/**
 * AdMob ad unit configuration.
 */
object AdsConfig {
    // Banner ad unit
    const val BANNER_ID = "ca-app-pub-8301774269044238/2806003913"

    // Interstitial ad unit
    const val INTERSTITIAL_ID = "ca-app-pub-8301774269044238/1946996625"

    // Minimum interval between interstitial ads
    const val INTERSTITIAL_MIN_INTERVAL_MS = 5 * 60 * 1000L

    // No full-screen ad during the first moments after the app opens -
    // the user must be settled in before any interstitial can appear.
    const val INTERSTITIAL_FIRST_DELAY_MS = 90 * 1000L

    // The first successful upload of each app session is always ad-free;
    // interstitials can only appear from the second upload onwards.
    const val INTERSTITIAL_MIN_UPLOADS_BEFORE_FIRST = 2
}
