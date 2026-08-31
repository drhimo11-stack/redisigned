package com.shield.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shield.app.ui.MainViewModel
import com.shield.app.ui.components.EyebrowLabel
import com.shield.app.ui.components.PatternListCard
import com.shield.app.ui.components.SectionCard

@Composable
fun WhitelistScreen(viewModel: MainViewModel) {
    val items = viewModel.whitelistItems.collectAsState().value
    var newPattern by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionCard(
            title = "Add a Trusted Pattern",
            icon = Icons.Filled.VerifiedUser,
            subtitle = "Checked first — a whitelist match skips the blocklist check entirely."
        ) {
            OutlinedTextField(
                value = newPattern,
                onValueChange = { newPattern = it },
                label = { Text("Pattern") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (newPattern.isNotBlank()) {
                        viewModel.addWhitelistPattern(newPattern.trim())
                        newPattern = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add to Whitelist")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EyebrowLabel(text = "Trusted Patterns \u2022 ${items.size}")
            PatternListCard(
                items = items.map { it.pattern },
                onRemove = { viewModel.removeWhitelistPattern(it) },
                emptyText = "Nothing whitelisted yet. Trusted patterns always bypass blocking.",
                accentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        }
    }
}
