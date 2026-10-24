package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun OnboardingDialog(
    currency: String,
    onDismiss: () -> Unit,
    onComplete: (Double, Double, Double) -> Unit,
    onLoadDemoData: () -> Unit
) {
    var startingBalanceText by remember { mutableStateOf("3500") }
    var monthlyIncomeText by remember { mutableStateOf("4200") }
    var monthlyBudgetText by remember { mutableStateOf("2500") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("onboarding_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to My Finance",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Let's personalize your dashboard baseline in 3 quick numbers:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = startingBalanceText,
                    onValueChange = { startingBalanceText = it },
                    label = { Text("Starting Total Balance ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_balance_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = monthlyIncomeText,
                    onValueChange = { monthlyIncomeText = it },
                    label = { Text("Estimated Monthly Income ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_income_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = monthlyBudgetText,
                    onValueChange = { monthlyBudgetText = it },
                    label = { Text("Monthly Budget Limit ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_budget_input")
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        val bal = startingBalanceText.toDoubleOrNull() ?: 3500.0
                        val inc = monthlyIncomeText.toDoubleOrNull() ?: 4200.0
                        val bud = monthlyBudgetText.toDoubleOrNull() ?: 2500.0
                        onComplete(bal, inc, bud)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_finish_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Started")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        onLoadDemoData()
                        val bal = startingBalanceText.toDoubleOrNull() ?: 3500.0
                        val inc = monthlyIncomeText.toDoubleOrNull() ?: 4200.0
                        val bud = monthlyBudgetText.toDoubleOrNull() ?: 2500.0
                        onComplete(bal, inc, bud)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_demo_btn")
                ) {
                    Text("Or start with Sample Data")
                }
            }
        }
    }
}
