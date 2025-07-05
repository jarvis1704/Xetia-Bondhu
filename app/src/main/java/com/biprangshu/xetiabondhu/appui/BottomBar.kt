package com.biprangshu.xetiabondhu.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BottomBar(
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier= modifier,
        actions = {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        //todo redirect to home
                    }
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home"
                    )
                }
                IconButton(
                    onClick = {
                        //todo redirect to history screen
                    }
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home"
                    )
                }
                IconButton(
                    onClick = {
                        //todo redirect to user screen
                    }
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home"
                    )
                }
            }
        }
    )
}