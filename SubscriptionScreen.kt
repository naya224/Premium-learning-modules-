package com.example.premuimlearning.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.premuimlearning.model.PlanType
import com.example.premuimlearning.model.SubscriptionPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: BillingViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val plans by viewModel.subscriptionPlans.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val activePlanName by viewModel.activePlanName.collectAsState()
    val statusMessage by viewModel.billingStatusMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription Management", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.restorePurchases() }) {
                        Text("Restore")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status banner
            if (!statusMessage.isNull_or_Empty_Placeholder()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status: ${statusMessage ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Active Subscription Card
            if (isSubscribed) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "ACTIVE SUBSCRIPTION",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = activePlanName ?: "Premium Member",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "You have unlocked all premium modules, source code labs, and certified tutorials.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (activity != null) {
                                        viewModel.manageSubscriptions(activity)
                                    }
                                }
                            ) {
                                Text("Manage in Google Play")
                            }

                            TextButton(
                                onClick = { viewModel.resetSubscriptionForTesting() }
                            ) {
                                Text("Reset (Test)")
                            }
                        }
                    }
                }
            } else {
                // Subscription Paywall Header
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unlock All Premium Courses",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose a monthly or yearly plan to get full access to advanced Kotlin, Android Architecture, and expert learning modules.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Text(
                text = "Available Plans",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Subscription Options List
            plans.forEach { plan ->
                SubscriptionPlanCard(
                    plan = plan,
                    isCurrentPlan = isSubscribed && activePlanName == plan.title,
                    onSelect = {
                        if (activity != null) {
                            viewModel.launchPurchaseFlow(activity, plan)
                        }
                    }
                )
            }

            // Benefits breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Included with Premium Subscription:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    BenefitItem("Full access to all Premium Learning Modules")
                    BenefitItem("Downloadable source code and practice projects")
                    BenefitItem("Verified transaction log & digital payment receipt")
                    BenefitItem("Cancel or switch plans anytime via Google Play Store")
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    onSelect: () -> Unit
) {
    val isYearly = plan.planType == PlanType.YEARLY

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isYearly) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (plan.savingsBadge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = plan.savingsBadge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = plan.priceText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = plan.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onSelect,
                enabled = !isCurrentPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isCurrentPlan) "Current Active Plan" else "Subscribe Now",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun String?.isNull_or_Empty_Placeholder(): Boolean {
    return this.isNullOrEmpty()
}
