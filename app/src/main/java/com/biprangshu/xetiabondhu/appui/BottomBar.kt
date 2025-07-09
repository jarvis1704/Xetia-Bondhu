package com.biprangshu.xetiabondhu.appui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biprangshu.xetiabondhu.navigation.NavigationScreens
import com.biprangshu.xetiabondhu.utils.selectedScreen

@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onUserClick: () -> Unit
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationBarItem(
                    selected = selectedScreen == NavigationScreens.HOMESCREEN,
                    onClick = onHomeClick,
                    icon = {
                        Icon(
                            imageVector = if (selectedScreen == NavigationScreens.HOMESCREEN) {
                                Icons.Filled.Home
                            } else {
                                Icons.Outlined.Home
                            },
                            contentDescription = "Home",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            fontSize = 12.sp,
                            fontWeight = if (selectedScreen == NavigationScreens.HOMESCREEN) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedScreen == NavigationScreens.HISTORYSCREEN,
                    onClick = onHistoryClick,
                    icon = {
                        Icon(
                            imageVector = if (selectedScreen == NavigationScreens.HISTORYSCREEN) {
                                Icons.Filled.History
                            } else {
                                Icons.Outlined.History
                            },
                            contentDescription = "History",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "History",
                            fontSize = 12.sp,
                            fontWeight = if (selectedScreen == NavigationScreens.HISTORYSCREEN) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = selectedScreen == NavigationScreens.USERDETAILSCREEN,
                    onClick = onUserClick,
                    icon = {
                        Icon(
                            imageVector = if (selectedScreen == NavigationScreens.USERDETAILSCREEN) {
                                Icons.Filled.Person
                            } else {
                                Icons.Outlined.Person
                            },
                            contentDescription = "Profile",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
                            fontSize = 12.sp,
                            fontWeight = if (selectedScreen == NavigationScreens.USERDETAILSCREEN) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    )
}