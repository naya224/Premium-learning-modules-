package com.example.premuimlearning.data

import android.content.Context
import android.content.SharedPreferences
import com.example.premuimlearning.model.PaymentLog
import org.json.JSONArray
import org.json.JSONObject

class PaymentLogManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePaymentLog(log: PaymentLog) {
        val existingLogs = getAllPaymentLogs().toMutableList()
        // Check if orderId already logged to avoid duplicates
        val index = existingLogs.indexOfFirst { it.orderId == log.orderId && it.orderId.isNotEmpty() }
        if (index >= 0) {
            existingLogs[index] = log
        } else {
            existingLogs.add(0, log)
        }
        persistLogs(existingLogs)
    }

    fun getAllPaymentLogs(): List<PaymentLog> {
        val rawJson = prefs.getString(KEY_PAYMENT_LOGS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(rawJson)
            val list = mutableListOf<PaymentLog>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PaymentLog(
                        id = obj.optString("id"),
                        orderId = obj.optString("orderId"),
                        productId = obj.optString("productId"),
                        planName = obj.optString("planName"),
                        purchaseToken = obj.optString("purchaseToken"),
                        purchaseTime = obj.optLong("purchaseTime"),
                        price = obj.optString("price"),
                        currency = obj.optString("currency"),
                        purchaseState = obj.optString("purchaseState"),
                        obfuscatedAccountId = obj.optString("obfuscatedAccountId"),
                        signatureHash = obj.optString("signatureHash")
                    )
                )
            }
            list.sortedByDescending { it.purchaseTime }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun generateReceiptText(log: PaymentLog): String {
        return """
            ========================================
                   PREMIUM LEARNING RECEIPT
            ========================================
            Transaction ID   : ${log.id}
            Order ID         : ${log.orderId.ifEmpty { "N/A" }}
            Product / Plan   : ${log.planName} (${log.productId})
            Date & Time      : ${log.formattedDate}
            Amount Paid      : ${log.price} ${log.currency}
            Status           : ${log.purchaseState}
            User Account Hash: ${log.obfuscatedAccountId}
            Purchase Token   : ${log.maskedToken}
            Digital Signature: ${log.signatureHash.take(16)}...
            Signature Status : ${if (log.isSignatureValid()) "VERIFIED [OK]" else "TAMPERED / INVALID"}
            ========================================
            Thank you for investing in your skills!
            ========================================
        """.trimIndent()
    }

    fun saveActiveSubscription(productId: String?, planName: String?, expiryTimestamp: Long) {
        prefs.edit()
            .putString(KEY_ACTIVE_PRODUCT, productId)
            .putString(KEY_ACTIVE_PLAN_NAME, planName)
            .putLong(KEY_SUBS_EXPIRY, expiryTimestamp)
            .apply()
    }

    fun getActiveSubscriptionProduct(): String? {
        val expiry = prefs.getLong(KEY_SUBS_EXPIRY, 0)
        return if (expiry > System.currentTimeMillis() || expiry == -1L) {
            prefs.getString(KEY_ACTIVE_PRODUCT, null)
        } else {
            null
        }
    }

    fun getActiveSubscriptionPlanName(): String? {
        val prod = getActiveSubscriptionProduct() ?: return null
        return prefs.getString(KEY_ACTIVE_PLAN_NAME, "Premium Active")
    }

    fun clearActiveSubscription() {
        prefs.edit()
            .remove(KEY_ACTIVE_PRODUCT)
            .remove(KEY_ACTIVE_PLAN_NAME)
            .remove(KEY_SUBS_EXPIRY)
            .apply()
    }

    private fun persistLogs(logs: List<PaymentLog>) {
        val jsonArray = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("orderId", log.orderId)
                put("productId", log.productId)
                put("planName", log.planName)
                put("purchaseToken", log.purchaseToken)
                put("purchaseTime", log.purchaseTime)
                put("price", log.price)
                put("currency", log.currency)
                put("purchaseState", log.purchaseState)
                put("obfuscatedAccountId", log.obfuscatedAccountId)
                put("signatureHash", log.signatureHash)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PAYMENT_LOGS, jsonArray.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "premium_learning_payment_prefs"
        private const val KEY_PAYMENT_LOGS = "key_payment_logs_json"
        private const val KEY_ACTIVE_PRODUCT = "key_active_subscription_product"
        private const val KEY_ACTIVE_PLAN_NAME = "key_active_plan_name"
        private const val KEY_SUBS_EXPIRY = "key_subscription_expiry_time"
    }
}
