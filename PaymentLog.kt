package com.example.premuimlearning.model

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PaymentLog(
    val id: String,
    val orderId: String,
    val productId: String,
    val planName: String,
    val purchaseToken: String,
    val purchaseTime: Long,
    val price: String,
    val currency: String,
    val purchaseState: String,
    val obfuscatedAccountId: String = "user_anon_101",
    val signatureHash: String = ""
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(purchaseTime))

    val maskedToken: String
        get() = if (purchaseToken.length > 16) {
            "${purchaseToken.take(8)}...${purchaseToken.takeLast(8)}"
        } else {
            purchaseToken
        }

    fun isSignatureValid(): Boolean {
        return calculateSignature(orderId, productId, purchaseTime, purchaseToken) == signatureHash
    }

    companion object {
        fun createLog(
            orderId: String,
            productId: String,
            planName: String,
            purchaseToken: String,
            purchaseTime: Long,
            price: String,
            currency: String,
            purchaseState: String,
            accountId: String = "user_anon_101"
        ): PaymentLog {
            val hash = calculateSignature(orderId, productId, purchaseTime, purchaseToken)
            val logId = "LOG_${System.currentTimeMillis()}"
            return PaymentLog(
                id = logId,
                orderId = orderId,
                productId = productId,
                planName = planName,
                purchaseToken = purchaseToken,
                purchaseTime = purchaseTime,
                price = price,
                currency = currency,
                purchaseState = purchaseState,
                obfuscatedAccountId = accountId,
                signatureHash = hash
            )
        }

        fun calculateSignature(orderId: String, productId: String, purchaseTime: Long, token: String): String {
            val raw = "$orderId|$productId|$purchaseTime|$token|PREMIUM_LEARNING_SECRET_KEY"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
