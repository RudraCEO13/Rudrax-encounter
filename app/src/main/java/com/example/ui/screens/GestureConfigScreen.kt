package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.GameActionTarget
import com.example.model.GestureMappingRule
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassPanel
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@Composable
fun GestureConfigScreen(
    rules: List<GestureMappingRule>,
    activeActionLabel: String,
    onUpdateRule: (GestureMappingRule) -> Unit,
    onResetDefaults: () -> Unit,
    onTestGesture: (GestureType, HandSide) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedHand by remember { mutableStateOf(HandSide.RIGHT) }
    val filteredRules = remember(rules, selectedHand) {
        rules.filter { it.handSide == selectedHand }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("gesture_config_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Gesture Mapping Studio",
                subtitle = "Configure Left & Right hand movements independently",
                actionLabel = "Reset",
                onAction = onResetDefaults
            )
        }

        item {
            GlassPanel(accentColor = ElectricCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedHand == HandSide.RIGHT,
                            onClick = { selectedHand = HandSide.RIGHT },
                            label = { Text("Right Hand") },
                            modifier = Modifier.testTag("tab_right_hand")
                        )
                        FilterChip(
                            selected = selectedHand == HandSide.LEFT,
                            onClick = { selectedHand = HandSide.LEFT },
                            label = { Text("Left Hand") },
                            modifier = Modifier.testTag("tab_left_hand")
                        )
                    }
                    StatusPill(
                        label = activeActionLabel,
                        color = if (selectedHand == HandSide.RIGHT) ElectricCyan else CyberPurple
                    )
                }
            }
        }

        items(filteredRules, key = { it.id }) { rule ->
            var dropdownExpanded by remember { mutableStateOf(false) }
            val accent = if (rule.handSide == HandSide.RIGHT) ElectricCyan else CyberPurple
            GlassPanel(
                highlighted = rule.enabled,
                accentColor = accent,
                modifier = Modifier.testTag("rule_${rule.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${rule.handSide.name} • ${rule.gesture.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhite
                        )
                        Text(
                            text = "SDK Event: ${rule.customSdkEvent} • Key: ${rule.actionTarget.keyCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolSlate
                        )
                    }
                    IconButton(onClick = { onTestGesture(rule.gesture, rule.handSide) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Test ${rule.gesture.displayName}", tint = accent)
                    }
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { onUpdateRule(rule.copy(enabled = it)) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Action:", style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
                    Box {
                        OutlinedButton(onClick = { dropdownExpanded = true }) {
                            Text(rule.actionTarget.label, color = accent)
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            GameActionTarget.entries.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text("${target.label} (${target.keyCode})") },
                                    onClick = {
                                        dropdownExpanded = false
                                        onUpdateRule(rule.copy(actionTarget = target))
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sensitivity: ${"%.2f".format(rule.sensitivity)}x • Deadzone: ${"%.2f".format(rule.deadzone)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolSlate
                )
                Slider(
                    value = rule.sensitivity,
                    onValueChange = { onUpdateRule(rule.copy(sensitivity = it)) },
                    valueRange = 0.4f..2.0f
                )
            }
        }

        item {
            TextButton(onClick = onResetDefaults, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Restore Default Left & Right Hand Mappings")
            }
        }
    }
}
