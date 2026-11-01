package com.example.premuimlearning.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.premuimlearning.billing.BillingManager
import com.example.premuimlearning.data.PaymentLogManager
import com.example.premuimlearning.model.PaymentLog
import com.example.premuimlearning.model.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    val paymentLogManager = PaymentLogManager(application)
    val billingManager = BillingManager(application, paymentLogManager)

    val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = billingManager.subscriptionPlans
    val isSubscribed: StateFlow<Boolean> = billingManager.isSubscribed
    val activePlanName: StateFlow<String?> = billingManager.activePlanName
    val billingStatusMessage: StateFlow<String?> = billingManager.billingStatusMessage

    private val _paymentLogs = MutableStateFlow<List<PaymentLog>>(emptyList())
    val paymentLogs: StateFlow<List<PaymentLog>> = _paymentLogs.asStateFlow()

    private val _selectedReceipt = MutableStateFlow<PaymentLog?>(null)
    val selectedReceipt: StateFlow<PaymentLog?> = _selectedReceipt.asStateFlow()

    init {
        refreshLogs()
    }

    fun refreshLogs() {
        _paymentLogs.value = paymentLogManager.getAllPaymentLogs()
    }

    fun selectReceipt(log: PaymentLog?) {
        _selectedReceipt.value = log
    }

    fun launchPurchaseFlow(activity: Activity, plan: SubscriptionPlan) {
        billingManager.launchPurchaseFlow(activity, plan)
        refreshLogs()
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
        refreshLogs()
    }

    fun manageSubscriptions(activity: Activity) {
        billingManager.cancelOrManageSubscriptionInPlayStore(activity)
    }

    fun resetSubscriptionForTesting() {
        billingManager.debugResetSubscription()
        refreshLogs()
    }

    fun getReceiptText(log: PaymentLog): String {
        return paymentLogManager.generateReceiptText(log)
    }
}
