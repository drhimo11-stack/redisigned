package com.shield.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shield.app.ui.MainViewModel
import com.shield.app.ui.components.EyebrowLabel
import com.shield.app.ui.components.SectionCard

private val PRESET_DAYS = listOf(1, 3, 7, 30, 90)

@Composable
fun LockSetupScreen(viewModel: MainViewModel) {
    val isLocked = viewModel.isLocked.collectAsState().value
    val remainingText = viewModel.lockRemainingText.collectAsState().value
    var customDays by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val statusColor = if (isLocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.LockClock else Icons.Filled.LockOpen,
                        contentDescription = null,
                        tint = statusColor
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isLocked) "Locked" else "Not Locked",
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor
                    )
                    Text(
                        text = if (isLocked) "$remainingText remaining \u2022 settings can't be changed"
                        else "Start a lock below to prevent changes for a fixed period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EyebrowLabel(text = "Quick Presets")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_DAYS.forEach { days ->
                    OutlinedButton(onClick = { confirming = days }) {
                        Text("${days}d")
                    }
                }
            }
        }

        SectionCard(title = "Custom Duration", subtitle = "1\u2013365 days") {
            OutlinedTextField(
                value = customDays,
                onValueChange = { input -> customDays = input.filter { it.isDigit() } },
                label = { Text("Days") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val days = customDays.toIntOrNull()?.coerceIn(1, 365)
                    if (days != null) confirming = days
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Custom Lock")
            }
        }

        AnimatedVisibility(
            visible = confirming != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val days = confirming
            if (days != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Lock the app for $days day${if (days == 1) "" else "s"}?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "This cannot be undone early.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 4.dp, start = 34.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                viewModel.startLock(days)
                                confirming = null
                            }) {
                                Text("Confirm")
                            }
                            OutlinedButton(onClick = { confirming = null }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}
