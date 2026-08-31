package com.kaushik.railway

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaushik.railway.ui.theme.Navy
import com.kaushik.railway.ui.theme.Orange
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kaushik.railway.nav.Routes
import com.kaushik.railway.ui.screens.AvailabilityScreen
import com.kaushik.railway.ui.screens.BookingsScreen
import com.kaushik.railway.ui.screens.HomeScreen
import com.kaushik.railway.ui.screens.LoginScreen
import com.kaushik.railway.ui.screens.MoreScreen
import com.kaushik.railway.ui.screens.PassengerScreen
import com.kaushik.railway.ui.screens.PaymentScreen
import com.kaushik.railway.ui.screens.PnrScreen
import com.kaushik.railway.ui.screens.ProfileScreen
import com.kaushik.railway.ui.screens.RegisterScreen
import com.kaushik.railway.ui.screens.ReviewScreen
import com.kaushik.railway.ui.screens.RunningStatusScreen
import com.kaushik.railway.ui.screens.SplashScreen
import com.kaushik.railway.ui.screens.TicketScreen
import com.kaushik.railway.ui.screens.TrainListScreen
import com.kaushik.railway.ui.screens.VacancyChartScreen
import com.kaushik.railway.ui.theme.RailwayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RailwayTheme { RailApp() }
        }
    }
}

@Composable
fun RailApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val route = back?.destination?.route
    val tabs = setOf(Routes.Home, Routes.Bookings, Routes.Pnr, Routes.More)
    val showBar = route in tabs

    Scaffold(
        bottomBar = {
            if (showBar) {
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Column {
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            NavItem("Home", route == Routes.Home, Icons.Default.Home) {
                                nav.navigate(Routes.Home) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                            }
                            NavItem("Bookings", route == Routes.Bookings, Icons.Default.Train) {
                                nav.navigate(Routes.Bookings) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                            }
                            NavItem("PNR", route == Routes.Pnr, Icons.Default.ConfirmationNumber) {
                                nav.navigate(Routes.Pnr) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                            }
                            NavItem("More", route == Routes.More, Icons.Default.MoreHoriz) {
                                nav.navigate(Routes.More) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                            }
                        }
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Routes.Splash, modifier = Modifier.padding(pad)) {
            composable(Routes.Splash) { SplashScreen { nav.navigate(Routes.Login) { popUpTo(Routes.Splash) { inclusive = true } } } }
            composable(Routes.Login) {
                LoginScreen(vm, onLogin = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } }, onRegister = { nav.navigate(Routes.Register) })
            }
            composable(Routes.Register) {
                RegisterScreen(vm, onDone = { nav.navigate(Routes.Home) { popUpTo(Routes.Login) { inclusive = true } } }, onBack = { nav.popBackStack() })
            }
            composable(Routes.Home) {
                HomeScreen(
                    vm,
                    onSearch = { nav.navigate(Routes.Trains) },
                    onPnr = { nav.navigate(Routes.Pnr) },
                    onRunning = { nav.navigate(Routes.Running) },
                    onBookings = { nav.navigate(Routes.Bookings) }
                )
            }
            composable(Routes.Trains) {
                TrainListScreen(vm, onBack = { nav.popBackStack() }, onSelect = {
                    vm.selectedTrain = it
                    nav.navigate(Routes.Availability)
                })
            }
            composable(Routes.Availability) {
                AvailabilityScreen(vm, onBack = { nav.popBackStack() }, onVacancy = {
                    vm.selectedTravelClass = it
                    nav.navigate(Routes.Vacancy)
                })
            }
            composable(Routes.Vacancy) {
                VacancyChartScreen(vm, onBack = { nav.popBackStack() }, onBook = { nav.navigate(Routes.Passengers) })
            }
            composable(Routes.Passengers) {
                PassengerScreen(vm, onBack = { nav.popBackStack() }, onContinue = { nav.navigate(Routes.Review) })
            }
            composable(Routes.Review) {
                ReviewScreen(vm, onBack = { nav.popBackStack() }, onPay = { nav.navigate(Routes.Payment) })
            }
            composable(Routes.Payment) {
                PaymentScreen(vm, onBack = { nav.popBackStack() }, onSuccess = {
                    nav.navigate(Routes.Ticket) { popUpTo(Routes.Home) }
                })
            }
            composable(Routes.Ticket) { TicketScreen(vm) { nav.navigate(Routes.Home) { popUpTo(Routes.Home) { inclusive = true } } } }
            composable(Routes.Pnr) { PnrScreen(vm, onBack = { if (!nav.popBackStack()) nav.navigate(Routes.Home) }) }
            composable(Routes.Running) { RunningStatusScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.Bookings) { BookingsScreen(vm, onBack = { if (!nav.popBackStack()) nav.navigate(Routes.Home) }) }
            composable(Routes.Profile) {
                ProfileScreen(
                    onBack = { nav.popBackStack() },
                    userId = vm.userId,
                    userName = vm.userName,
                    email = vm.email,
                    mobile = vm.mobile,
                    onLogout = { vm.loggedIn = false; nav.navigate(Routes.Login) { popUpTo(0) { inclusive = true } } }
                )
            }
            composable(Routes.More) {
                MoreScreen(
                    onPnr = { nav.navigate(Routes.Pnr) },
                    onRunning = { nav.navigate(Routes.Running) },
                    onProfile = { nav.navigate(Routes.Profile) },
                    onBookings = { nav.navigate(Routes.Bookings) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(label: String, selected: Boolean, icon: ImageVector, onClick: () -> Unit) {
    val tint = if (selected) Orange else Navy.copy(alpha = 0.55f)
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = tint)
        Text(label, color = tint, fontSize = 11.sp)
    }
}
