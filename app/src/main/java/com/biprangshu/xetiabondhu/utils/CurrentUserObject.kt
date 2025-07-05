package com.biprangshu.xetiabondhu.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

object CurrentUserObject {

    var username: String? by mutableStateOf(null)
    var useremail: String? by mutableStateOf(null)
    var userId: String? by mutableStateOf(null)

}