package com.shield.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shield.app.ui.MainViewModel
import com.shield.app.ui.components.EyebrowLabel
import com.shield.app.ui.components.PatternListCard
import com.shield.app.ui.components.SectionCard

@Composable
fun BlocklistScreen(viewModel: MainViewModel) {
    val items = viewModel.blocklistItems.collectAsState().value
    var newPattern by remember { mutableStateOf("") }

    // Informational only — Keyword already falls back to matching a bad
    // pattern as plain text rather than crashing, so this never blocks
    // adding it, it just tells the user what will actually happen.
    val isValidRegex by remember(newPattern) {
        derivedStateOf {
            if (newPattern.isBlank()) true
            else try {
                Regex(newPattern)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionCard(
            title = "Add a Blocked Pattern",
            icon = Icons.Filled.Block,
            subtitle = "Regex or plain text, matched against any visible page text."
        ) {
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                label = { Text("Pattern") },
                singleLine = true,
                isError = !isValidRegex,
                modifier = Modifier.fillMaxWidth()
            )
            AnimatedVisibility(visible = !isValidRegex) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Not valid regex — will be matched as plain text instead.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = {
                    if (newPattern.isNotBlank()) {
                        viewModel.addBlockPattern(newPattern.trim())
                        newPattern = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Blocklist")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EyebrowLabel(text = "Blocked Patterns \u2022 ${items.size}")
            PatternListCard(
                items = items.map { it.pattern },
                onRemove = { viewModel.removeBlockPattern(it) },
                emptyText = "No patterns blocked yet. Anything you add here is checked on every page."
            )
        }
    }
}
