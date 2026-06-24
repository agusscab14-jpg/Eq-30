package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EqProfile
import com.example.ui.components.VerticalEqSlider
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqScreen(viewModel: EqViewModel) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val currentLevels by viewModel.currentLevels.collectAsStateWithLifecycle()
    val currentFrequencies by viewModel.currentFrequencies.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var showDbDialog by remember { mutableStateOf<Int?>(null) }
    var dbInput by remember { mutableStateOf("") }

    var showFreqDialog by remember { mutableStateOf<Int?>(null) }
    var freqInput by remember { mutableStateOf("") }

    LaunchedEffect(profiles) {
        viewModel.createDefaultProfilesIfNeeded(profiles)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSaveDialog = true },
                containerColor = NeonPink
            ) {
                Icon(Icons.Filled.Add, "Save Profile", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Profile Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedProfile?.name ?: "Custom",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sound Profile") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name, color = Color.White) },
                                onClick = {
                                    viewModel.selectProfile(profile)
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (profile.isCustom) {
                                        IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                                            Icon(Icons.Filled.Delete, "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Profile Info
            Text(
                text = if (selectedProfile == null) "Customized" else selectedProfile!!.name,
                color = NeonPink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 30-Band EQ Layout
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(currentFrequencies) { index, freq ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(48.dp)
                    ) {
                        Text(
                            text = formatDb(currentLevels[index]),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clickable {
                                    dbInput = ((currentLevels[index] * 10f).roundToInt() / 10f).toString()
                                    showDbDialog = index
                                }
                        )
                        VerticalEqSlider(
                            value = currentLevels[index],
                            onValueChange = { viewModel.updateBand(index, it) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        Text(
                            text = formatFrequency(freq),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable {
                                    freqInput = freq.roundToInt().toString()
                                    showFreqDialog = index
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Profile") },
                text = {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Profile Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newProfileName.isNotBlank()) {
                            viewModel.saveCurrentAsProfile(newProfileName)
                        }
                        showSaveDialog = false
                        newProfileName = ""
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDbDialog != null) {
            val idx = showDbDialog!!
            AlertDialog(
                onDismissRequest = { showDbDialog = null },
                title = { Text("Set Level (dB)") },
                text = {
                    OutlinedTextField(
                        value = dbInput,
                        onValueChange = { dbInput = it },
                        label = { Text("Value (-15 to +15)") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val level = dbInput.toFloatOrNull()
                        if (level != null) {
                            viewModel.updateBand(idx, level)
                        }
                        showDbDialog = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDbDialog = null }) { Text("Cancel") }
                }
            )
        }

        if (showFreqDialog != null) {
            val idx = showFreqDialog!!
            AlertDialog(
                onDismissRequest = { showFreqDialog = null },
                title = { Text("Set Frequency (Hz)") },
                text = {
                    OutlinedTextField(
                        value = freqInput,
                        onValueChange = { freqInput = it },
                        label = { Text("Value (20 to 20000)") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val f = freqInput.toFloatOrNull()
                        if (f != null) {
                            viewModel.updateFrequency(idx, f)
                        }
                        showFreqDialog = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showFreqDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

private fun formatDb(level: Float): String {
    val rounded = (level * 10f).roundToInt() / 10f
    return if (rounded > 0) "+$rounded dB" else "$rounded dB"
}

private fun formatFrequency(freq: Float): String {
    return if (freq >= 1000f) {
        val k = freq / 1000f
        val formatted = ((k * 100f).roundToInt() / 100f).toString()
        if (formatted.endsWith(".0")) "${formatted.substringBefore(".")}k" else "${formatted}k"
    } else {
        freq.roundToInt().toString()
    }
}
