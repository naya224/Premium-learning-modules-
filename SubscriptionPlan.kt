package com.example.premuimlearning.model

import com.android.billingclient.api.ProductDetails

enum class PlanType {
    MONTHLY,
    YEARLY
}

data class SubscriptionPlan(
    val id: String,
    val title: String,
    val description: String,
    val priceText: String,
    val rawPriceMicros: Long,
    val currencyCode: String,
    val planType: PlanType,
    val savingsBadge: String? = null,
    val productDetails: ProductDetails? = null,
    val offerToken: String? = null
)
