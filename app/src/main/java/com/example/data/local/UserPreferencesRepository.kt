package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserSettings(
    val currency: String = "$",
    val monthlyBudget: Double = 2500.0,
    val startingBalance: Double = 3500.0,
    val monthlyIncomeTarget: Double = 4000.0,
    val userName: String = "User",
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val isOnboardingCompleted: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val STARTING_BALANCE = doublePreferencesKey("starting_balance")
        val MONTHLY_INCOME_TARGET = doublePreferencesKey("monthly_income_target")
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            currency = preferences[PreferencesKeys.CURRENCY] ?: "$",
            monthlyBudget = preferences[PreferencesKeys.MONTHLY_BUDGET] ?: 2500.0,
            startingBalance = preferences[PreferencesKeys.STARTING_BALANCE] ?: 3500.0,
            monthlyIncomeTarget = preferences[PreferencesKeys.MONTHLY_INCOME_TARGET] ?: 4000.0,
            userName = preferences[PreferencesKeys.USER_NAME] ?: "Finance Explorer",
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    suspend fun updateMonthlyBudget(budget: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_BUDGET] = budget
        }
    }

    suspend fun updateStartingBalance(balance: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STARTING_BALANCE] = balance
        }
    }

    suspend fun updateMonthlyIncomeTarget(income: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONTHLY_INCOME_TARGET] = income
        }
    }

    suspend fun updateThemeMode(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme
        }
    }

    suspend fun completeOnboarding(startingBalance: Double, monthlyIncome: Double, monthlyBudget: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STARTING_BALANCE] = startingBalance
            preferences[PreferencesKeys.MONTHLY_INCOME_TARGET] = monthlyIncome
            preferences[PreferencesKeys.MONTHLY_BUDGET] = monthlyBudget
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun resetPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
