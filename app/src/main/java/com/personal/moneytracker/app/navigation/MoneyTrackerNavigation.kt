package com.personal.moneytracker.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.personal.moneytracker.feature.debug.DebugEventDetailScreen
import com.personal.moneytracker.feature.debug.DebugEventListScreen
import com.personal.moneytracker.feature.onboarding.OnboardingScreen
import com.personal.moneytracker.feature.transactions.TransactionDetailScreen
import com.personal.moneytracker.feature.transactions.TransactionListScreen
import com.personal.moneytracker.feature.rules.RulesScreen
import com.personal.moneytracker.feature.sync.SyncSettingsScreen

@Composable
fun MoneyTrackerNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "onboarding") {
        composable("onboarding") { OnboardingScreen(onDebug = { nav.navigate("debug") }, onTransactions = { nav.navigate("transactions") }, onRules = { nav.navigate("rules") }, onSync = { nav.navigate("sync") }) }
        composable("debug") {
            DebugEventListScreen(onBack = nav::popBackStack, onOpen = { nav.navigate("event/$it") })
        }
        composable("event/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            DebugEventDetailScreen(entry.arguments?.getString("id").orEmpty(), nav::popBackStack)
        }
        composable("transactions") { TransactionListScreen(nav::popBackStack, onOpen = { nav.navigate("transaction/$it") }) }
        composable("transaction/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            TransactionDetailScreen(entry.arguments?.getString("id").orEmpty(), nav::popBackStack)
        }
        composable("rules") { RulesScreen(nav::popBackStack) }
        composable("sync") { SyncSettingsScreen(nav::popBackStack) }
    }
}
