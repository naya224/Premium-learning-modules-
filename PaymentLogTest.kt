package com.example.premuimlearning

import com.example.premuimlearning.model.PaymentLog
import org.junit.Assert.*
import org.junit.Test

class PaymentLogTest {

    @Test
    fun testPaymentLogSignatureValidation() {
        val orderId = "GPA.1234-5678-9012-34567"
        val productId = "premium_sub_monthly"
        val planName = "Monthly Premium Plan"
        val token = "test_purchase_token_abc_123_xyz_999"
        val timestamp = System.currentTimeMillis()
        val price = "$9.99/mo"

        val log = PaymentLog.createLog(
            orderId = orderId,
            productId = productId,
            planName = planName,
            purchaseToken = token,
            purchaseTime = timestamp,
            price = price,
            currency = "USD",
            purchaseState = "ACKNOWLEDGED",
            accountId = "user_anon_101"
        )

        assertTrue("Signature should be valid when unaltered", log.isSignatureValid())

        // Verify tampering detection
        val tamperedLog = log.copy(orderId = "GPA.TAMPERED-ORDER-ID")
        assertFalse("Signature validation must fail when log is tampered", tamperedLog.isSignatureValid())
    }

    @Test
    fun testMaskedTokenFormat() {
        val longToken = "token_prefix_123456789_token_suffix"
        val log = PaymentLog.createLog(
            orderId = "GPA.1111",
            productId = "premium_sub_yearly",
            planName = "Yearly Premium Plan",
            purchaseToken = longToken,
            purchaseTime = 1700000000000L,
            price = "$79.99/yr",
            currency = "USD",
            purchaseState = "ACKNOWLEDGED"
        )

        assertTrue(log.maskedToken.contains("..."))
        assertEquals("${longToken.take(8)}...${longToken.takeLast(8)}", log.maskedToken)
    }
}
