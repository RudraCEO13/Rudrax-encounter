package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CustomGameProject
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@Composable
fun GameStudioScreen(
    games: List<CustomGameProject>,
    selectedGameId: String?,
    onUploadGameUris: (List<Uri>) -> Unit,
    onInstallSdkDemo: () -> Unit,
    onSelectGame: (CustomGameProject) -> Unit,
    onUpdateGameConfig: (CustomGameProject, String, String, Float) -> Unit,
    onDeleteGame: (CustomGameProject) -> Unit,
    onOpenPreview: (CustomGameProject) -> Unit,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) onUploadGameUris(uris)
    }

    val selectedGame = remember(games, selectedGameId) {
        games.firstOrNull { it.id == selectedGameId } ?: games.firstOrNull()
    }

    var configTitle by remember(selectedGame?.id) { mutableStateOf(selectedGame?.title.orEmpty()) }
    var configOrientation by remember(selectedGame?.id) { mutableStateOf(selectedGame?.targetOrientation ?: "Landscape") }
    var configSensitivity by remember(selectedGame?.id) { mutableFloatStateOf(selectedGame?.sensitivityMultiplier ?: 1.0f) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_studio_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                title = "Custom Game Studio",
                subtitle = "Upload HTML, CSS, JS, ZIP archives, or Web3D game assets — preserved verbatim"
            )
        }

        item {
            GlassPanel(highlighted = true, accentColor = ElectricCyan) {
                Text(
                    text = "Import Custom Game Project",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Detects index.html entry point automatically, mounts original assets in our sandboxed runtime, and injects window.MotionX SDK.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/html",
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "text/javascript",
                                    "application/javascript",
                                    "text/css",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = PitchBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_game_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Upload HTML / ZIP", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onInstallSdkDemo,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("install_sdk_demo_button")
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("SDK Arena Demo")
                    }
                }
            }
        }

        if (games.isEmpty()) {
            item {
                GlassPanel(accentColor = CyberPurple) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Custom Games Imported", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text(
                        "Upload your own HTML5 / WebGL / Three.js game or ZIP project above, or install the MotionX SDK Cyber Arena demo to test real hand controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                }
            }
        } else {
            item {
                SectionHeader(title = "Uploaded Projects (${games.size})")
            }

            items(games, key = { it.id }) { game ->
                val isSelected = selectedGame?.id == game.id
                GlassPanel(
                    highlighted = isSelected,
                    accentColor = if (isSelected) ElectricCyan else NeonBlue,
                    onClick = { onSelectGame(game) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("game_card_${game.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(game.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                StatusPill(
                                    label = game.sourceType,
                                    color = if (game.isBuiltInTemplate) CyberPurple else EmeraldSignal
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Entry: ${game.entryPoint} • ${game.files.size} preserved files",
                                style = MaterialTheme.typography.labelMedium,
                                color = CoolSlate
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onOpenPreview(game) },
                                modifier = Modifier.testTag("launch_game_${game.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Launch ${game.title}", tint = ElectricCyan)
                            }
                            IconButton(onClick = { onDeleteGame(game) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${game.title}", tint = CoolSlate)
                            }
                        }
                    }

                    if (isSelected && game.files.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Project Structure:",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        game.files.take(6).forEach { file ->
                            Text(
                                text = "• ${file.relativePath} (${file.sizeBytes} B)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CoolSlate
                            )
                        }
                        if (game.files.size > 6) {
                            Text(
                                text = "+ ${game.files.size - 6} more files preserved",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberPurple
                            )
                        }
                    }
                }
            }
        }

        if (selectedGame != null) {
            item {
                GlassPanel(accentColor = CyberPurple) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SettingsInputComponent, contentDescription = null, tint = CyberPurple)
                        Text("Game Configuration", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = configTitle,
                        onValueChange = { configTitle = it },
                        label = { Text("Project Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Landscape", "Portrait", "TV Fullscreen").forEach { mode ->
                            FilterChip(
                                selected = configOrientation == mode,
                                onClick = { configOrientation = mode },
                                label = { Text(mode) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Motion Control Sensitivity: ${"%.2f".format(configSensitivity)}x",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Slider(
                        value = configSensitivity,
                        onValueChange = { configSensitivity = it },
                        valueRange = 0.4f..2.2f
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                onUpdateGameConfig(selectedGame, configTitle, configOrientation, configSensitivity)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Config")
                        }
                        Button(
                            onClick = { onOpenPreview(selectedGame) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play in Preview", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
