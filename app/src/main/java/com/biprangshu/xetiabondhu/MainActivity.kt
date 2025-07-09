package com.biprangshu.xetiabondhu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.biprangshu.xetiabondhu.appui.BottomBar
import com.biprangshu.xetiabondhu.navigation.Navigation
import com.biprangshu.xetiabondhu.navigation.NavigationScreens
import com.biprangshu.xetiabondhu.ui.theme.XetiaBondhuTheme
import com.biprangshu.xetiabondhu.utils.apiKey
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XetiaBondhuTheme {

                val navController = rememberNavController()
                apiKey = stringResource(R.string.gemini_api_key)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination


                        val bottomBarVisibleRoutes = listOf(
                            NavigationScreens.HOMESCREEN,
                            NavigationScreens.HISTORYSCREEN,
                            NavigationScreens.USERDETAILSCREEN,
                            NavigationScreens.CHATSCREEN
                        )

                        if(currentDestination?.route in bottomBarVisibleRoutes){
                            BottomBar(
                                onHomeClick = {
                                    navController.navigate(NavigationScreens.HOMESCREEN) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onHistoryClick = {
                                    navController.navigate(NavigationScreens.HISTORYSCREEN) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onUserClick = {
                                    navController.navigate(NavigationScreens.USERDETAILSCREEN) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onChatClick = {
                                    navController.navigate(NavigationScreens.CHATSCREEN){
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Navigation(
                        navcontroller = navController
                    )
                }
            }
        }
    }
}