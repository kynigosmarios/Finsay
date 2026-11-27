package com.example.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    private val context: Context,
    private val onPurchasePending: () -> Unit = {},
    private val onActivePurchasesUpdated: (purchases: List<PlayPurchaseEntitlement>, isFreshPurchase: Boolean) -> Unit
) : PurchasesUpdatedListener {

    data class PlayPurchaseEntitlement(
        val purchaseToken: String,
        val obfuscatedAccountId: String?,
        val productIds: List<String>
    )

    data class ProPlanPrices(
        val monthlyPrice: String? = null,
        val yearlyPrice: String? = null,
        val lifetimePrice: String? = null
    )

    companion object {
        private const val TAG = "FinsayBilling"
        const val SUB_MONTHLY = "finsay_pro_monthly"
        const val SUB_YEARLY = "finsay_pro_yearly"
        const val INAPP_LIFETIME = "finsay_pro_lifetime"
        private const val MAX_ACKNOWLEDGE_RETRIES = 2
    }

    private var billingClient: BillingClient? = try {
        BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .build()
    } catch (t: Throwable) {
        Log.w(TAG, "BillingClient creation safe fallback: ${t.message}")
        null
    }

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _monthlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val monthlyProductDetails: StateFlow<ProductDetails?> = _monthlyProductDetails.asStateFlow()

    private val _yearlyProductDetails = MutableStateFlow<ProductDetails?>(null)
    val yearlyProductDetails: StateFlow<ProductDetails?> = _yearlyProductDetails.asStateFlow()

    private val _lifetimeProductDetails = MutableStateFlow<ProductDetails?>(null)
    val lifetimeProductDetails: StateFlow<ProductDetails?> = _lifetimeProductDetails.asStateFlow()

    private val _planPrices = MutableStateFlow(ProPlanPrices())
    val planPrices: StateFlow<ProPlanPrices> = _planPrices.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val purchaseQueryLock = Any()
    private var activeQueryGeneration = 0L
    private var subsQueryDone = false
    private var inappQueryDone = false
    private var subsQueryOk = false
    private var inappQueryOk = false
    private var activeSubsList: List<PlayPurchaseEntitlement> = emptyList()
    private var activeInappList: List<PlayPurchaseEntitlement> = emptyList()
    private val notifiedPendingTokens = mutableSetOf<String>()

    private fun extractSubscriptionFormattedPrice(details: ProductDetails?): String? {
        val offers = details?.subscriptionOfferDetails.orEmpty()
        if (offers.isEmpty()) return null
        val baseOffer = offers.firstOrNull { it.offerId.isNullOrBlank() } ?: offers.firstOrNull() ?: return null
        val phases = baseOffer.pricingPhases.pricingPhaseList
        val recurringPhase = phases.lastOrNull { it.priceAmountMicros > 0L } ?: phases.lastOrNull()
        return recurringPhase?.formattedPrice?.takeIf { it.isNotBlank() }
    }

    private fun updatePlanPricesSnapshot() {
        val monthly = extractSubscriptionFormattedPrice(_monthlyProductDetails.value)
        val yearly = extractSubscriptionFormattedPrice(_yearlyProductDetails.value)
        val lifetime = _lifetimeProductDetails.value?.oneTimePurchaseOfferDetails?.formattedPrice?.takeIf { it.isNotBlank() }
        _planPrices.value = ProPlanPrices(
            monthlyPrice = monthly,
            yearlyPrice = yearly,
            lifetimePrice = lifetime
        )
    }

    fun startConnection() {
        val client = billingClient ?: return
        try {
            if (client.isReady) {
                _isBillingReady.value = true
                queryAllProducts()
                queryActivePurchases()
                return
            }
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    try {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            _isBillingReady.value = true
                            queryAllProducts()
                            queryActivePurchases()
                        } else {
                            _isBillingReady.value = false
                            Log.i(TAG, "Billing service not available on current environment (code: ${billingResult.responseCode}): ${billingResult.debugMessage}")
                        }
                    } catch (t: Throwable) {
                        _isBillingReady.value = false
                        Log.w(TAG, "onBillingSetupFinished safe fallback: ${t.message}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isBillingReady.value = false
                    Log.d(TAG, "Billing service disconnected")
                }
            })
        } catch (t: Throwable) {
            _isBillingReady.value = false
            Log.w(TAG, "startConnection safe fallback: ${t.message}")
        }
    }

    fun queryAllProducts() {
        val client = billingClient ?: return
        try {
            if (!client.isReady) return

            // 1. Query Subscription products (Monthly & Yearly)
            val subsList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUB_MONTHLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SUB_YEARLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val subsParams = QueryProductDetailsParams.newBuilder().setProductList(subsList).build()
            client.queryProductDetailsAsync(subsParams) { billingResult, queryResult ->
                try {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val list = queryResult.productDetailsList ?: emptyList()
                        for (details in list) {
                            when (details.productId) {
                                SUB_MONTHLY -> _monthlyProductDetails.value = details
                                SUB_YEARLY -> _yearlyProductDetails.value = details
                            }
                            Log.d(TAG, "Found subscription: ${details.productId} - ${details.title}")
                        }
                        updatePlanPricesSnapshot()
                    } else {
                        Log.d(TAG, "Query subscriptions status: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Subs query callback warning: ${t.message}")
                }
            }

            // 2. Query In-App One-Time product (Lifetime)
            val inappList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(INAPP_LIFETIME)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val inappParams = QueryProductDetailsParams.newBuilder().setProductList(inappList).build()
            client.queryProductDetailsAsync(inappParams) { billingResult, queryResult ->
                try {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val list = queryResult.productDetailsList ?: emptyList()
                        for (details in list) {
                            if (details.productId == INAPP_LIFETIME) {
                                _lifetimeProductDetails.value = details
                                Log.d(TAG, "Found in-app product: ${details.productId} - ${details.title}")
                            }
                        }
                        updatePlanPricesSnapshot()
                    } else {
                        Log.d(TAG, "Query in-app status: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "In-app query callback warning: ${t.message}")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "queryAllProducts safe fallback: ${t.message}")
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productId: String = SUB_YEARLY,
        obfuscatedAccountId: String? = null
    ): Boolean {
        val client = billingClient ?: return false
        return try {
            if (!client.isReady) {
                startConnection()
                return false
            }

            if (productId == INAPP_LIFETIME) {
                val details = _lifetimeProductDetails.value ?: run {
                    Log.w(TAG, "No product details available for $productId.")
                    return false
                }
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
                val flowBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                if (!obfuscatedAccountId.isNullOrBlank()) {
                    flowBuilder.setObfuscatedAccountId(obfuscatedAccountId)
                }
                val result = client.launchBillingFlow(activity, flowBuilder.build())
                result.responseCode == BillingClient.BillingResponseCode.OK
            } else {
                val details = when (productId) {
                    SUB_MONTHLY -> _monthlyProductDetails.value
                    SUB_YEARLY -> _yearlyProductDetails.value
                    else -> {
                        Log.w(TAG, "Refusing billing flow for unrecognized productId: $productId")
                        return false
                    }
                }
                val offerToken = details?.let { selectBestOfferToken(it) }
                if (details == null || offerToken == null) {
                    Log.w(TAG, "No product details or offer token available for $productId.")
                    return false
                }
                val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .setOfferToken(offerToken)
                    .build()
                val flowBuilder = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                if (!obfuscatedAccountId.isNullOrBlank()) {
                    flowBuilder.setObfuscatedAccountId(obfuscatedAccountId)
                }
                val result = client.launchBillingFlow(activity, flowBuilder.build())
                result.responseCode == BillingClient.BillingResponseCode.OK
            }
        } catch (t: Throwable) {
            Log.w(TAG, "launchPurchaseFlow safe fallback: ${t.message}")
            false
        }
    }

    /**
     * Deterministically selects the best eligible subscription offer returned by Google Play:
     * 1. Prefer an eligible promotional/trial offer (offerId != null) with the lowest initial phase price
     *    (e.g., 0 micros free trial first, then lowest intro price).
     * 2. Fall back to the base plan (offerId == null).
     * 3. Final fallback to firstOrNull().
     */
    private fun selectBestOfferToken(details: ProductDetails): String? {
        val offers = details.subscriptionOfferDetails.orEmpty()
        if (offers.isEmpty()) return null

        val bestPromoOffer = offers
            .filter { !it.offerId.isNullOrBlank() }
            .minByOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros ?: Long.MAX_VALUE
            }
        if (bestPromoOffer != null) return bestPromoOffer.offerToken

        val basePlanOffer = offers.firstOrNull { it.offerId.isNullOrBlank() }
        return (basePlanOffer ?: offers.firstOrNull())?.offerToken
    }

    fun queryActivePurchases() {
        val client = billingClient ?: return
        try {
            if (!client.isReady) return

            val queryGeneration = synchronized(purchaseQueryLock) {
                activeQueryGeneration += 1L
                subsQueryDone = false
                inappQueryDone = false
                subsQueryOk = false
                inappQueryOk = false
                activeSubsList = emptyList()
                activeInappList = emptyList()
                activeQueryGeneration
            }

            // 1. Query Subscriptions
            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            client.queryPurchasesAsync(subsParams) { billingResult, purchases ->
                try {
                    val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    val active = if (ok) processPurchasesBatch(purchases, isPurchaseUpdate = false) else emptyList()
                    evaluateCombinedQueryResult(
                        queryGeneration = queryGeneration,
                        isSubs = true,
                        queryOk = ok,
                        activePurchases = active
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "querySubsPurchases callback warning: ${t.message}")
                    evaluateCombinedQueryResult(
                        queryGeneration = queryGeneration,
                        isSubs = true,
                        queryOk = false,
                        activePurchases = emptyList()
                    )
                }
            }

            // 2. Query In-App purchases
            val inappParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(inappParams) { billingResult, purchases ->
                try {
                    val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    val active = if (ok) processPurchasesBatch(purchases, isPurchaseUpdate = false) else emptyList()
                    evaluateCombinedQueryResult(
                        queryGeneration = queryGeneration,
                        isSubs = false,
                        queryOk = ok,
                        activePurchases = active
                    )
                } catch (t: Throwable) {
                    Log.w(TAG, "queryInAppPurchases callback warning: ${t.message}")
                    evaluateCombinedQueryResult(
                        queryGeneration = queryGeneration,
                        isSubs = false,
                        queryOk = false,
                        activePurchases = emptyList()
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "queryActivePurchases safe fallback: ${t.message}")
        }
    }

    private fun evaluateCombinedQueryResult(
        queryGeneration: Long,
        isSubs: Boolean,
        queryOk: Boolean,
        activePurchases: List<PlayPurchaseEntitlement>
    ) {
        var combinedToEmit: List<PlayPurchaseEntitlement>? = null
        synchronized(purchaseQueryLock) {
            // Ignore stale callbacks from an older overlapping queryActivePurchases() call
            if (queryGeneration != activeQueryGeneration) return

            if (isSubs) {
                subsQueryDone = true
                subsQueryOk = queryOk
                activeSubsList = activePurchases
            } else {
                inappQueryDone = true
                inappQueryOk = queryOk
                activeInappList = activePurchases
            }

            if (subsQueryDone && inappQueryDone) {
                val merged = (activeSubsList + activeInappList).distinctBy { it.purchaseToken }
                // CRITICAL: Never revoke Pro (emit an empty list) unless BOTH SUBS and INAPP queries succeeded.
                // A transient error on one query type must never strip an active subscriber's entitlement.
                if (subsQueryOk && inappQueryOk) {
                    combinedToEmit = merged
                } else if (merged.isNotEmpty()) {
                    combinedToEmit = merged
                } else {
                    Log.w(TAG, "Skipping entitlement revocation because Billing query was incomplete (subsOk=$subsQueryOk, inappOk=$inappQueryOk)")
                }
            }
        }
        combinedToEmit?.let { list ->
            Log.d(TAG, "Google Play active purchases resolved: ${list.size}")
            onActivePurchasesUpdated(list, false)
        }
    }

    private fun processPurchasesBatch(
        purchases: List<Purchase>?,
        isPurchaseUpdate: Boolean
    ): List<PlayPurchaseEntitlement> {
        val activeList = mutableListOf<PlayPurchaseEntitlement>()
        var notifyPending = false
        try {
            purchases?.forEach { purchase ->
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        activeList.add(
                            PlayPurchaseEntitlement(
                                purchaseToken = purchase.purchaseToken,
                                obfuscatedAccountId = purchase.accountIdentifiers?.obfuscatedAccountId,
                                productIds = purchase.products.toList()
                            )
                        )
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        }
                    }
                    Purchase.PurchaseState.PENDING -> {
                        val token = purchase.purchaseToken
                        val isNewPending = synchronized(purchaseQueryLock) {
                            if (isPurchaseUpdate || token.isBlank() || notifiedPendingTokens.add(token)) {
                                if (token.isNotBlank()) notifiedPendingTokens.add(token)
                                true
                            } else {
                                false
                            }
                        }
                        if (isNewPending) {
                            notifyPending = true
                        }
                    }
                    else -> Unit
                }
            }
            if (notifyPending) {
                Log.d(TAG, "Purchase is PENDING confirmation from Google Play.")
                onPurchasePending()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "processPurchasesBatch warning: ${t.message}")
        }
        return activeList
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        try {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val freshPurchases = processPurchasesBatch(purchases, isPurchaseUpdate = true)
                    if (freshPurchases.isNotEmpty()) {
                        Log.d(TAG, "Purchase completed via Google Play.")
                        val merged = synchronized(purchaseQueryLock) {
                            (activeSubsList + activeInappList + freshPurchases).distinctBy { it.purchaseToken }
                        }
                        onActivePurchasesUpdated(merged, true)
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Log.d(TAG, "User canceled Google Play purchase flow.")
                }
                else -> {
                    Log.w(TAG, "Purchase update: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "onPurchasesUpdated safe fallback: ${t.message}")
        }
    }

    private fun acknowledgePurchase(purchase: Purchase, attempt: Int = 0) {
        val client = billingClient ?: return
        try {
            if (purchase.isAcknowledged) return
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(acknowledgeParams) { result ->
                try {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
                        val entitlement = PlayPurchaseEntitlement(
                            purchaseToken = purchase.purchaseToken,
                            obfuscatedAccountId = purchase.accountIdentifiers?.obfuscatedAccountId,
                            productIds = purchase.products.toList()
                        )
                        val merged = synchronized(purchaseQueryLock) {
                            (activeSubsList + activeInappList + entitlement).distinctBy { it.purchaseToken }
                        }
                        onActivePurchasesUpdated(merged, true)
                    } else {
                        Log.w(TAG, "Purchase acknowledge note (code=${result.responseCode}, attempt=$attempt): ${result.debugMessage}")
                        val isRetryable = result.responseCode == BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ||
                            result.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ||
                            result.responseCode == BillingClient.BillingResponseCode.NETWORK_ERROR ||
                            result.responseCode == BillingClient.BillingResponseCode.ERROR
                        if (isRetryable && attempt < MAX_ACKNOWLEDGE_RETRIES) {
                            val delayMs = (attempt + 1) * 3_000L
                            mainHandler.postDelayed({
                                if (billingClient?.isReady == true) {
                                    acknowledgePurchase(purchase, attempt + 1)
                                }
                            }, delayMs)
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "acknowledgePurchase callback warning: ${t.message}")
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "acknowledgePurchase safe fallback: ${t.message}")
        }
    }

    fun endConnection() {
        try {
            val client = billingClient ?: return
            if (client.isReady) {
                client.endConnection()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "endConnection safe fallback: ${t.message}")
        }
    }
}
