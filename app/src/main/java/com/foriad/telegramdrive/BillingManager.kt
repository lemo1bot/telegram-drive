package com.foriad.telegramdrive

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play Billing for the one-time, non-consumable Premium product
 * ([PremiumConfig.PRODUCT_ID] — $10 lifetime, removes all ads forever).
 *
 * Rules followed here:
 * - The purchase is ACKNOWLEDGED (required for non-consumables) and
 *   NEVER consumed — consuming would let the user buy it again and
 *   would break lifetime ownership / restore.
 * - Premium is only unlocked after Play reports a real PURCHASED state.
 *   Nothing is ever "fake-unlocked" locally.
 * - SIDELOAD REALITY: an APK installed from a GitHub Release was not
 *   installed by Google Play, so Billing cannot connect / the product
 *   is not found. In that case [Listener.onBillingUnavailable] fires and
 *   the UI shows a friendly "available in the Google Play version"
 *   message. No crash, no fake unlock.
 */
class BillingManager(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        /** Real, verified price text from Play, e.g. "$10.00". */
        fun onPriceLoaded(formattedPrice: String)
        /** A purchase (new or restored) is owned and acknowledged. */
        fun onPremiumOwned()
        /** Play Billing can't be used in this install (sideloaded, no Play, product missing...). */
        fun onBillingUnavailable(reason: String)
        /** Something else went wrong (network, user cancelled is NOT reported here). */
        fun onBillingError(message: String)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val purchasesListener = com.android.billingclient.api.PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    postError("No purchase returned by Google Play.")
                } else {
                    purchases.forEach { handlePurchase(it) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // User backed out of the Play sheet — not an error, stay silent.
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // They already own it (e.g. bought on another device) — restore it.
                restorePurchases()
            }
            else -> postError("Purchase failed (${result.responseCode}): ${result.debugMessage}")
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private var productDetails: ProductDetails? = null
    private var connecting = false
    private var connected = false

    /** Connects to Play (once) and loads the Premium product + real price. */
    fun start() {
        if (connected) {
            productDetails?.let { postPrice(it) } ?: queryProduct()
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    queryProduct()
                    // Silently pick up ownership bought on another device / earlier install.
                    restorePurchases()
                } else {
                    postUnavailable(
                        "Google Play Billing is not available on this install " +
                            "(response ${result.responseCode})."
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connected = false
                // Play will reconnect on the next start() call (e.g. dialog reopened).
            }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PremiumConfig.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && detailsList.isNotEmpty()) {
                val details = detailsList.first()
                productDetails = details
                postPrice(details)
            } else {
                // Product missing in Play Console for this app, or this APK
                // was sideloaded so Play doesn't serve products to it.
                postUnavailable(
                    "The Premium product was not found in Google Play for this install."
                )
            }
        }
    }

    /** Opens the Google Play purchase sheet. Returns false if buying isn't possible here. */
    fun buy(activity: Activity): Boolean {
        val details = productDetails
        if (!connected || details == null) {
            postUnavailable("Purchases are only available in the Google Play version of this app.")
            return false
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            postError("Could not open the purchase sheet (${result.responseCode}): ${result.debugMessage}")
            return false
        }
        return true
    }

    /**
     * Restore: asks Play which INAPP products this Google account owns.
     * Used by the "Restore Purchase" button and automatically on connect,
     * so a reinstall or a new phone re-activates Premium for free.
     */
    fun restorePurchases() {
        if (!connected) {
            // Connect first; onBillingSetupFinished -> queryProduct + restore run there.
            start()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val mine = purchases.firstOrNull { PremiumConfig.PRODUCT_ID in it.products }
                if (mine != null) {
                    handlePurchase(mine)
                } else {
                    postError("No earlier Premium purchase was found for this Google account.")
                }
            } else {
                postUnavailable("Could not check earlier purchases with Google Play (${result.responseCode}).")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (PremiumConfig.PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (purchase.isAcknowledged) {
                    postPremiumOwned()
                } else {
                    // Non-consumables MUST be acknowledged within 3 days or
                    // Play auto-refunds. Acknowledge — never consume.
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    client.acknowledgePurchase(ackParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            postPremiumOwned()
                        } else {
                            postError("Purchase received but could not be confirmed (${ackResult.responseCode}). Tap Restore Purchase.")
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                postError("Your purchase is pending (e.g. cash payment). Premium turns on automatically once Google Play confirms payment.")
            }
            else -> postError("Purchase is not complete yet.")
        }
    }

    fun end() {
        if (client.isReady) client.endConnection()
        connected = false
    }

    private fun postPrice(details: ProductDetails) {
        val price = details.oneTimePurchaseOfferDetails?.formattedPrice
        if (price != null) mainHandler.post { listener.onPriceLoaded(price) }
    }

    private fun postPremiumOwned() = mainHandler.post { listener.onPremiumOwned() }
    private fun postUnavailable(reason: String) = mainHandler.post { listener.onBillingUnavailable(reason) }
    private fun postError(message: String) = mainHandler.post { listener.onBillingError(message) }
}
