package com.example.premuimlearning.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.premuimlearning.data.PaymentLogManager
import com.example.premuimlearning.model.PaymentLog
import com.example.premuimlearning.model.PlanType
import com.example.premuimlearning.model.SubscriptionPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BillingManager(
    private val context: Context,
    private val paymentLogManager: PaymentLogManager
) : PurchasesUpdatedListener {

    private val TAG = "BillingManager"

    companion object {
        const val SUB_MONTHLY_ID = "premium_sub_monthly"
        const val SUB_YEARLY_ID = "premium_sub_yearly"
    }

    private val _subscriptionPlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = _subscriptionPlans.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _activePlanName = MutableStateFlow<String?>(null)
    val activePlanName: StateFlow<String?> = _activePlanName.asStateFlow()

    private val _billingStatusMessage = MutableStateFlow<String?>("Initializing Play Billing...")
    val billingStatusMessage: StateFlow<String?> = _billingStatusMessage.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    init {
        // Load initial state from local secure storage
        val activeProd = paymentLogManager.getActiveSubscriptionProduct()
        _isSubscribed.value = activeProd != null
        _activePlanName.value = paymentLogManager.getActiveSubscriptionPlanName()

        startBillingConnection()
    }

    fun startBillingConnection() {
        _billingStatusMessage.value = "Connecting to Google Play Store..."
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client connected successfully.")
                    _billingStatusMessage.value = "Connected to Google Play"
                    querySubscriptionProducts()
                    queryActivePurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                    _billingStatusMessage.value = "Play Billing unavailable (${billingResult.responseCode}). Sandbox mode active."
                    setupFallbackPlans()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected.")
                _billingStatusMessage.value = "Billing service disconnected. Reconnecting..."
                setupFallbackPlans()
            }
        })
    }

    private fun querySubscriptionProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_MONTHLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUB_YEARLY_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val plans = productDetailsList.map { details ->
                    val offer = details.subscriptionOfferDetails?.firstOrNull()
                    val pricing = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    val formattedPrice = pricing?.formattedPrice ?: if (details.productId == SUB_MONTHLY_ID) "$9.99/mo" else "$79.99/yr"

                    SubscriptionPlan(
                        id = details.productId,
                        title = if (details.productId == SUB_MONTHLY_ID) "Monthly Premium" else "Yearly Premium",
                        description = if (details.productId == SUB_MONTHLY_ID)
                            "Full access to all premium learning modules, updated monthly."
                        else
                            "Best Value! Save ~33% annually on all premium courses.",
                        priceText = formattedPrice,
                        rawPriceMicros = pricing?.priceAmountMicros ?: 0L,
                        currencyCode = pricing?.priceCurrencyCode ?: "USD",
                        planType = if (details.productId == SUB_MONTHLY_ID) PlanType.MONTHLY else PlanType.YEARLY,
                        savingsBadge = if (details.productId == SUB_YEARLY_ID) "Save 33%" else null,
                        productDetails = details,
                        offerToken = offer?.offerToken
                    )
                }
                _subscriptionPlans.value = plans
            } else {
                Log.w(TAG, "No product details returned from Play Store. Using sandbox plans.")
                setupFallbackPlans()
            }
        }
    }

    private fun setupFallbackPlans() {
        val monthly = SubscriptionPlan(
            id = SUB_MONTHLY_ID,
            title = "Monthly Premium Plan",
            description = "Access all premium courses, coding labs, and completion certificates.",
            priceText = "$9.99 / month",
            rawPriceMicros = 9990000L,
            currencyCode = "USD",
            planType = PlanType.MONTHLY,
            savingsBadge = null
        )

        val yearly = SubscriptionPlan(
            id = SUB_YEARLY_ID,
            title = "Yearly Premium Plan",
            description = "Unlimited access for 1 full year. Includes offline notes & priority support.",
            priceText = "$79.99 / year",
            rawPriceMicros = 79990000L,
            currencyCode = "USD",
            planType = PlanType.YEARLY,
            savingsBadge = "Best Value - Save 33%"
        )

        _subscriptionPlans.value = listOf(monthly, yearly)
    }

    fun launchPurchaseFlow(activity: Activity, plan: SubscriptionPlan) {
        val details = plan.productDetails
        if (billingClient.isReady && details != null && plan.offerToken != null) {
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(plan.offerToken)
                .build()

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .setObfuscatedAccountId("user_anon_101")
                .build()

            billingClient.launchBillingFlow(activity, flowParams)
        } else {
            // Simulated / Sandbox transaction when Play Store product is not published yet
            simulatePurchase(plan)
        }
    }

    private fun simulatePurchase(plan: SubscriptionPlan) {
        scope.launch {
            val mockOrderId = "GPA.${(1000..9999).random()}-${(1000..9999).random()}-${(1000..9999).random()}-${(10000..99999).random()}"
            val mockToken = "sim_token_${UUID.randomUUID().toString().take(18)}"
            val now = System.currentTimeMillis()
            val expiryDuration = if (plan.planType == PlanType.MONTHLY) 30L * 24 * 3600 * 1000 else 365L * 24 * 3600 * 1000

            val log = PaymentLog.createLog(
                orderId = mockOrderId,
                productId = plan.id,
                planName = plan.title,
                purchaseToken = mockToken,
                purchaseTime = now,
                price = plan.priceText,
                currency = plan.currencyCode,
                purchaseState = "ACKNOWLEDGED (Sandbox)",
                accountId = "user_anon_101"
            )

            paymentLogManager.savePaymentLog(log)
            paymentLogManager.saveActiveSubscription(plan.id, plan.title, now + expiryDuration)

            _isSubscribed.value = true
            _activePlanName.value = plan.title
            _billingStatusMessage.value = "Subscription active: ${plan.title}"
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _billingStatusMessage.value = "Purchase canceled by user."
        } else {
            _billingStatusMessage.value = "Purchase failed: ${billingResult.debugMessage}"
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully.")
                        recordSuccessfulPurchase(purchase)
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${result.debugMessage}")
                    }
                }
            } else {
                recordSuccessfulPurchase(purchase)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _billingStatusMessage.value = "Purchase pending approval (e.g., cash payment)."
        }
    }

    private fun recordSuccessfulPurchase(purchase: Purchase) {
        val prodId = purchase.products.firstOrNull() ?: SUB_MONTHLY_ID
        val planName = if (prodId == SUB_YEARLY_ID) "Yearly Premium Plan" else "Monthly Premium Plan"
        val priceStr = if (prodId == SUB_YEARLY_ID) "$79.99/yr" else "$9.99/mo"
        val expiryTime = System.currentTimeMillis() + if (prodId == SUB_YEARLY_ID) 365L * 24 * 3600 * 1000 else 30L * 24 * 3600 * 1000

        val log = PaymentLog.createLog(
            orderId = purchase.orderId ?: "GPA.PLAY_${System.currentTimeMillis()}",
            productId = prodId,
            planName = planName,
            purchaseToken = purchase.purchaseToken,
            purchaseTime = purchase.purchaseTime,
            price = priceStr,
            currency = "USD",
            purchaseState = "ACKNOWLEDGED",
            accountId = purchase.accountIdentifiers?.obfuscatedAccountId ?: "user_anon_101"
        )

        paymentLogManager.savePaymentLog(log)
        paymentLogManager.saveActiveSubscription(prodId, planName, expiryTime)

        _isSubscribed.value = true
        _activePlanName.value = planName
        _billingStatusMessage.value = "Subscription successfully activated!"
    }

    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (active.isNotEmpty()) {
                    val latest = active.maxByOrNull { it.purchaseTime }
                    if (latest != null) {
                        recordSuccessfulPurchase(latest)
                    }
                } else {
                    // Check local cache
                    val cacheProd = paymentLogManager.getActiveSubscriptionProduct()
                    _isSubscribed.value = cacheProd != null
                }
            }
        }
    }

    fun restorePurchases() {
        _billingStatusMessage.value = "Restoring previous purchases..."
        if (billingClient.isReady) {
            queryActivePurchases()
        } else {
            // Check stored local logs
            val logs = paymentLogManager.getAllPaymentLogs()
            if (logs.isNotEmpty()) {
                val lastLog = logs.first()
                val now = System.currentTimeMillis()
                val expiry = lastLog.purchaseTime + (30L * 24 * 3600 * 1000)
                if (now < expiry || lastLog.productId == SUB_YEARLY_ID) {
                    paymentLogManager.saveActiveSubscription(lastLog.productId, lastLog.planName, lastLog.purchaseTime + (365L * 24 * 3600 * 1000))
                    _isSubscribed.value = true
                    _activePlanName.value = lastLog.planName
                    _billingStatusMessage.value = "Restored purchase from receipt history."
                    return
                }
            }
            _billingStatusMessage.value = "No active subscriptions found to restore."
        }
    }

    fun cancelOrManageSubscriptionInPlayStore(activity: Activity) {
        val url = "https://play.google.com/store/account/subscriptions"
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        activity.startActivity(intent)
    }

    fun debugResetSubscription() {
        paymentLogManager.clearActiveSubscription()
        _isSubscribed.value = false
        _activePlanName.value = null
        _billingStatusMessage.value = "Subscription reset for testing."
    }
}
