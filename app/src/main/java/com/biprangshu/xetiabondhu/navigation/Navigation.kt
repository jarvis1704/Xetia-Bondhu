package com.biprangshu.xetiabondhu.navigation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun Navigation(
    modifier: Modifier = Modifier,
    navcontroller: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val authState = authViewModel.authState.collectAsState().value
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(authState) {
        Log.d("authstate","authstate = ${authState}")
        when(authState){
            is AuthState.SignedIn -> {
                delay(600)
                try {
                    //implement what to do when Signed In
                    navcontroller.navigate(NavigationScreens.HOMESCREEN){
                        popUpTo(0){inclusive=true}
                    }

                } catch (e: Exception) {
                    Log.e("AppNav", "Error checking first login status", e)
                    //default behaviour, can choose what to do here
                    navcontroller.navigate(NavigationScreens.HOMESCREEN){
                        popUpTo(0){inclusive=true}
                    }
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
        startDestination = NavigationScreens.LOADINGSCREEN
    ) {
        composable(NavigationScreens.HOMESCREEN) {
            HomeScreen()
        }

        composable (NavigationScreens.LOGINSCREEN){
            //google login
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if(result.resultCode == Activity.RESULT_OK){
                    result.data?.let { intent ->
                        authViewModel.handleGoogleSignInResult(intent)
                    }
                } else {
                    Log.d("AppNav", "Google sign-in cancelled or failed")
                    authViewModel.resetAuthState()
                }
            }

            LoginScreen(
                onGoogleSignInClick = {
                    coroutineScope.launch {
                        try {
                            val signInIntentSender = authViewModel.signInWithGoogle()
                            signInIntentSender?.let { intentSender ->
                                launcher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            }
                        }catch (e: Exception) {
                            Log.e("AppNav", "Error initiating Google Sign-In", e)
                        }
                    }
                }
            )
        }

        composable(NavigationScreens.LOADINGSCREEN) {
            LoadingScreen()
        }
    }
}