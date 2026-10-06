package com.ghosttrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ghosttrack.app.ui.admin.AdminDashboardScreen
import com.ghosttrack.app.ui.auth.*
import com.ghosttrack.app.ui.driver.DriverBroadcastScreen
import com.ghosttrack.app.ui.driver.DriverDashboardScreen
import com.ghosttrack.app.ui.splash.SplashDestination
import com.ghosttrack.app.ui.splash.SplashScreen
import com.ghosttrack.app.ui.student.LiveTrackingScreen
import com.ghosttrack.app.ui.student.StudentDashboardScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object RoleSelect : Screen("role_select")
    object StudentLogin : Screen("student_login")
    object StudentSignup : Screen("student_signup")
    object DriverLogin : Screen("driver_login")
    object DriverSignup : Screen("driver_signup")
    object AdminLogin : Screen("admin_login")
    object StudentDashboard : Screen("student_dashboard")
    object LiveTracking : Screen("live_tracking/{sessionId}") {
        fun createRoute(sessionId: String) = "live_tracking/$sessionId"
    }
    object DriverDashboard : Screen("driver_dashboard")
    object DriverBroadcast : Screen("driver_broadcast/{sessionId}/{busName}") {
        fun createRoute(sessionId: String, busName: String): String {
            val encodedBusName = URLEncoder.encode(busName, StandardCharsets.UTF_8.toString())
            return "driver_broadcast/$sessionId/$encodedBusName"
        }
    }
    object AdminDashboard : Screen("admin_dashboard")
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // 1. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigate = { destination ->
                    val targetRoute = when (destination) {
                        is SplashDestination.RoleSelect -> Screen.RoleSelect.route
                        is SplashDestination.StudentDashboard -> Screen.StudentDashboard.route
                        is SplashDestination.DriverDashboard -> Screen.DriverDashboard.route
                        is SplashDestination.AdminDashboard -> Screen.AdminDashboard.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Role Select Screen
        composable(Screen.RoleSelect.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val currentServer by authViewModel.currentServerUrl.collectAsState()
            RoleSelectScreen(
                currentServerUrl = currentServer,
                onUpdateServerUrl = { authViewModel.setServerUrl(it) },
                onSelectStudent = { navController.navigate(Screen.StudentLogin.route) },
                onSelectDriver = { navController.navigate(Screen.DriverLogin.route) },
                onSelectAdmin = { navController.navigate(Screen.AdminLogin.route) }
            )
        }

        // 3. Student Auth
        composable(Screen.StudentLogin.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            StudentLoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.StudentDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.StudentSignup.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StudentSignup.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            StudentSignupScreen(
                viewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate(Screen.StudentDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Screen.StudentLogin.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // 4. Driver Auth
        composable(Screen.DriverLogin.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            DriverLoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.DriverDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.DriverSignup.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DriverSignup.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            DriverSignupScreen(
                viewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate(Screen.DriverDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Screen.DriverLogin.route) },
                onBack = { navController.popBackStack() }
            )
        }

        // 5. Admin Auth
        composable(Screen.AdminLogin.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AdminLoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.RoleSelect.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 6. Student Dashboard
        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(
                onTrackSession = { sessionId ->
                    navController.navigate(Screen.LiveTracking.createRoute(sessionId))
                },
                onLogout = {
                    navController.navigate(Screen.RoleSelect.route) {
                        popUpTo(Screen.StudentDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // 7. Live Tracking (Student Map)
        composable(
            route = Screen.LiveTracking.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            LiveTrackingScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }

        // 8. Driver Dashboard
        composable(Screen.DriverDashboard.route) {
            DriverDashboardScreen(
                onOpenBroadcast = { sessionId, routeInfo ->
                    navController.navigate(Screen.DriverBroadcast.createRoute(sessionId, routeInfo))
                },
                onLogout = {
                    navController.navigate(Screen.RoleSelect.route) {
                        popUpTo(Screen.DriverDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        // 9. Driver Broadcast Screen
        composable(
            route = Screen.DriverBroadcast.route,
            arguments = listOf(
                navArgument("sessionId") { type = NavType.StringType },
                navArgument("busName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            val rawBusName = backStackEntry.arguments?.getString("busName") ?: "Bus"
            val busName = try {
                URLDecoder.decode(rawBusName, StandardCharsets.UTF_8.toString())
            } catch (e: Exception) {
                rawBusName
            }
            DriverBroadcastScreen(
                sessionId = sessionId,
                busName = busName,
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Admin Dashboard
        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onTrackSession = { sessionId ->
                    navController.navigate(Screen.LiveTracking.createRoute(sessionId))
                },
                onLogout = {
                    navController.navigate(Screen.RoleSelect.route) {
                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
