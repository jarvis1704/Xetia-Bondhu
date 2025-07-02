package com.biprangshu.xetiabondhu.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.biprangshu.xetiabondhu.appui.HomeScreen
import com.biprangshu.xetiabondhu.appui.LoadingScreen
import com.biprangshu.xetiabondhu.appui.LoginScreen
import com.biprangshu.xetiabondhu.authentication.AuthViewModel
import com.biprangshu.xetiabondhu.datamodel.AuthState
import kotlinx.coroutines.delay

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navcontroller: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val authState = authViewModel.authState.collectAsState()


    LaunchedEffect(authState) {
        Log.d("authstate","authstate = ${authState}")
        when(authState){
            is AuthState.SignedIn -> {
                delay(600)
                try {
                    //implement what to do when Signed In

                } catch (e: Exception) {
                    Log.e("AppNav", "Error checking first login status", e)
                    //default behaviour, can choose what to do here

                }
            }
            is AuthState.SignedOut -> {
                delay(600)
                //handle what to do when signed out
                navcontroller.navigate(NavigationScreens.LOGINSCREEN){
                    popUpTo(0){inclusive=true}
                }
            }
            is AuthState.Loading -> {
                //handle login
            }
            is AuthState.Error -> {
                Log.e("AppNav", "Auth error: ${(authState as AuthState.Error).message}")
            }
            is AuthState.Initial -> {
                //dont know what to do with it
            }
        }
    }

    NavHost(
        navController = navcontroller,
        startDestination = NavigationScreens.LOGINSCREEN
    ) {
        composable(NavigationScreens.HOMESCREEN) {
            HomeScreen()
        }

        composable (NavigationScreens.LOGINSCREEN){
            LoginScreen()
        }

        composable(NavigationScreens.LOADINGSCREEN) {
            LoadingScreen()
        }
    }
}