package com.example.premuimlearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.premuimlearning.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainAppScreen()
            }
        }
    }
}

@Composable
fun MainAppScreen(
    billingViewModel: BillingViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCourseForDialog by remember { mutableStateOf<Course?>(null) }

    val isSubscribed by billingViewModel.isSubscribed.collectAsState()
    val activePlanName by billingViewModel.activePlanName.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Courses") },
                    icon = { Text("📚", fontSize = 18.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Plans") },
                    icon = { Text("💳", fontSize = 18.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("Receipts") },
                    icon = { Text("🧾", fontSize = 18.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> LearningHomeScreen(
                    isSubscribed = isSubscribed,
                    activePlanName = activePlanName,
                    onOpenSubscription = { selectedTab = 1 },
                    onSelectCourse = { course -> selectedCourseForDialog = course }
                )
                1 -> SubscriptionScreen(
                    viewModel = billingViewModel
                )
                2 -> PaymentLogsScreen(
                    viewModel = billingViewModel
                )
            }
        }

        selectedCourseForDialog?.let { course ->
            CourseDetailDialog(
                course = course,
                isSubscribed = isSubscribed,
                onDismiss = { selectedCourseForDialog = null },
                onOpenSubscriptions = { selectedTab = 1 }
            )
        }
    }
}

@Composable
fun LearningHomeScreen(
    isSubscribed: Boolean,
    activePlanName: String?,
    onOpenSubscription: () -> Unit,
    onSelectCourse: (Course) -> Unit
) {
    val freeCourses = listOf(
        Course(
            title = "Introduction to Programming",
            description = "Master the fundamentals of variables, loops, logic, and basic object-oriented concepts.",
            isPremium = false,
            lessons = listOf("1. Welcome to Coding", "2. Data Types & Logic", "3. Control Flow & Functions")
        )
    )

    val premiumCourses = listOf(
        Course(
            title = "Advanced Kotlin",
            description = "In-depth guide to Kotlin Coroutines, Flow, Generics, Extension Functions, and Functional Programming.",
            isPremium = true,
            lessons = listOf("1. Coroutines & Structured Concurrency", "2. StateFlow & SharedFlow", "3. Custom DSLs & Generics")
        ),
        Course(
            title = "Android App Development",
            description = "Build modern Jetpack Compose applications with ViewModel, Clean Architecture, and Google Play Services.",
            isPremium = true,
            lessons = listOf("1. Jetpack Compose UI Layouts", "2. Architecture Components & State", "3. Google Play Billing & Monetization")
        ),
        Course(
            title = "Professional Development",
            description = "Software architecture, unit testing strategies, CI/CD pipelines, and publishing apps to production.",
            isPremium = true,
            lessons = listOf("1. Unit Testing & Mocking", "2. Clean Architecture & Hilt DI", "3. Play Store Publishing Checklist")
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Premium Learning",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Subscription Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSubscribed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSubscribed) "✓ Premium Membership Unlocked" else "⭐ Upgrade to Premium Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        color = if (isSubscribed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "ACTIVE" else "UPGRADE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = if (isSubscribed)
                        "Plan: ${activePlanName ?: "Premium Member"}. All courses unlocked!"
                    else
                        "Subscribe monthly or yearly to unlock all advanced modules and project source code.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!isSubscribed) {
                    Button(
                        onClick = onOpenSubscription,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("View Subscription Plans")
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Free Learning Modules",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        freeCourses.forEach { course ->
            CourseCard(
                course = course,
                isUnlocked = true,
                onClick = { onSelectCourse(course) },
                onOpenSubscription = onOpenSubscription
            )
        }

        HorizontalDivider()

        Text(
            text = "Premium Learning Modules",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        premiumCourses.forEach { course ->
            CourseCard(
                course = course,
                isUnlocked = isSubscribed,
                onClick = { onSelectCourse(course) },
                onOpenSubscription = onOpenSubscription
            )
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onOpenSubscription: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = if (!course.isPremium) MaterialTheme.colorScheme.tertiary
                    else if (isUnlocked) Color(0xFF2E7D32)
                    else MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (!course.isPremium) "FREE" else if (isUnlocked) "UNLOCKED" else "PREMIUM",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (isUnlocked) {
                    Button(onClick = onClick) {
                        Text("Start Learning")
                    }
                } else {
                    Button(onClick = onClick) {
                        Text("View Course Details")
                    }
                    OutlinedButton(onClick = onOpenSubscription) {
                        Text("Subscribe")
                    }
                }
            }
        }
    }
}
