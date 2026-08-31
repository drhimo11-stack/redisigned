package com.shield.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shield.app.accessibility.InstalledAppInfo
import com.shield.app.blocklist.ManagedAppItem
import com.shield.app.ui.MainViewModel
import com.shield.app.ui.components.AppIconBadge
import com.shield.app.ui.components.EmptyState
import com.shield.app.ui.components.EyebrowLabel

@Composable
fun ManagedAppsScreen(viewModel: MainViewModel) {
    val managedApps = viewModel.managedApps.collectAsState().value

    var pickerExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var loadingCandidates by remember { mutableStateOf(false) }

    LaunchedEffect(pickerExpanded) {
        if (pickerExpanded && candidates.isEmpty()) {
            loadingCandidates = true
            candidates = viewModel.listInstallableApps()
            loadingCandidates = false
        }
    }

    val managedPackageNames = managedApps.map { it.packageName }.toSet()
    val filteredCandidates = candidates
        .filter { it.packageName !in managedPackageNames }
        .filter { searchQuery.isBlank() || it.label.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Any browser or downloader added here is scanned and blocked the " +
                "same way as a built-in supported browser \u2014 and this list stays " +
                "editable even while a lock is active.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledTonalButton(
            onClick = { pickerExpanded = !pickerExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (pickerExpanded) Icons.Filled.ExpandLess else Icons.Filled.Add,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (pickerExpanded) "Hide App Picker" else "Add App")
        }

        AnimatedVisibility(
            visible = pickerExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search installed apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    loadingCandidates -> Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Scanning installed apps\u2026",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    filteredCandidates.isEmpty() -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        EmptyState(text = "No matching apps.")
                    }
                    else -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(filteredCandidates, key = { it.packageName }) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        AppIconBadge(packageName = app.packageName)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(app.label, style = MaterialTheme.typography.titleMedium)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (app.isBrowserCapable) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Language,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = if (app.isBrowserCapable) "Browser-capable" else app.packageName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    FilledTonalButton(onClick = {
                                        viewModel.addManagedApp(app.packageName, app.label)
                                        searchQuery = ""
                                    }) {
                                        Text("Add")
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            EyebrowLabel(text = "Managed Apps \u2022 ${managedApps.size}")

            if (managedApps.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    EmptyState(
                        text = "No apps added yet. Add a browser or downloader above to start scanning it.",
                        icon = Icons.Filled.Apps
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(managedApps, key = { it.packageName }) { app ->
                        ManagedAppRow(
                            app = app,
                            onToggle = { viewModel.setManagedAppBlocked(app.packageName, it) },
                            onRemove = { viewModel.removeManagedApp(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagedAppRow(
    app: ManagedAppItem,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    val statusColor = if (app.blocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AppIconBadge(packageName = app.packageName)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(app.appLabel, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (app.autoDetected) "Auto-detected" else "Added manually",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
            }
            Switch(checked = app.blocked, onCheckedChange = onToggle)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
