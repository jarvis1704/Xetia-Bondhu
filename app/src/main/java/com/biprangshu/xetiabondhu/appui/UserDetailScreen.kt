package com.biprangshu.xetiabondhu.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.biprangshu.xetiabondhu.utils.CurrentUserObject

@Composable
fun UserDetailScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Current user name: ${CurrentUserObject.username}"
        )
        Text(
            text = "Current user email: ${CurrentUserObject.useremail}"
        )
        Text(
            text = "Current userid: ${CurrentUserObject.userId}"
        )
    }
}