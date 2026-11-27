package com.foriad.telegramdrive

/**
 * ============================================================
 *  PREMIUM — $10 LIFETIME (ONE-TIME PURCHASE, NOT A SUBSCRIPTION)
 * ============================================================
 * One non-consumable in-app product removes ALL ads forever:
 * no banner, no interstitials — on this phone, and restorable
 * on a new phone with the same Google account ("Restore Purchase").
 *
 * The price is NOT hard-coded in the app. It is set in Play Console:
 *   Play Console → Monetize → In-app products → Create product
 *   Product ID: premium_lifetime  (must match PRODUCT_ID below)
 *   Price: $10 → Activate
 * The app asks Google Play for the product and displays the REAL
 * localized price (e.g. $10.00 / ₹830.00) on the Buy button.
 * FALLBACK_PRICE_TEXT below is only shown when Play cannot be
 * reached (e.g. the app was sideloaded from GitHub — Play Billing
 * does not work in sideloaded APKs).
 *
 * Full steps: see README.md → "Premium — $10 forever (Google Play Billing)".
 */
object PremiumConfig {
    // Must exactly match the product ID created in Play Console.
    const val PRODUCT_ID = "premium_lifetime"

    // Shown only when Google Play is unavailable and the real price can't be loaded.
    const val FALLBACK_PRICE_TEXT = "$10"
}
