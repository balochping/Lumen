package com.lumen.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ThemeSettingsScreen(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    Column(modifier.fillMaxSize()) {
        LumenScreenHeader(title = s.themeSettings, onBack = onBack, applyStatusBarPadding = true, modifier = Modifier.padding(horizontal = 16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ModeCard(
                    icon = Icons.Filled.Settings,
                    label = s.themeModeSystem,
                    selected = state.themeMode == ThemeMode.SYSTEM,
                    onClick = { onUpdate(state.copy(themeMode = ThemeMode.SYSTEM)) }
                )
            }
            item {
                ModeCard(
                    icon = Icons.Filled.LightMode,
                    label = s.themeModeLight,
                    selected = state.themeMode == ThemeMode.LIGHT,
                    onClick = { onUpdate(state.copy(themeMode = ThemeMode.LIGHT)) }
                )
            }
            item {
                ModeCard(
                    icon = Icons.Filled.DarkMode,
                    label = s.themeModeDark,
                    selected = state.themeMode == ThemeMode.DARK,
                    onClick = { onUpdate(state.copy(themeMode = ThemeMode.DARK)) }
                )
            }
            item { Spacer(Modifier) }

            items(ThemePreset.entries) { preset ->
                PresetCard(
                    preset = preset,
                    selected = state.themePreset == preset && !state.useMaterialYou,
                    onClick = { onUpdate(state.copy(themePreset = preset, useMaterialYou = false)) }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val matYouColor = if (state.useMaterialYou) MaterialTheme.colorScheme.primary else Color(0xFF6B7280)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (state.useMaterialYou) 2.dp else 1.dp,
                                color = if (state.useMaterialYou) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onUpdate(state.copy(useMaterialYou = true)) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, matYouColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.ColorLens, contentDescription = null, tint = matYouColor, modifier = Modifier.size(18.dp))
                                }
                                if (state.useMaterialYou) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF5A9CFF)))
                                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4AC2B6)))
                                Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFF28FAD)))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Material You", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(s.materialYouDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = if (state.useAmoledBlack) 2.dp else 1.dp,
                            color = if (state.useAmoledBlack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onUpdate(state.copy(useAmoledBlack = !state.useAmoledBlack)) }
                        .padding(16.dp)
                ) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DarkMode, contentDescription = null, tint = Color(0xFF00B0FF), modifier = Modifier.size(18.dp))
                            }
                            if (state.useAmoledBlack) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(12.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF1E2433)))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF00B0FF)))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(s.amoledBlack, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(s.amoledBlackDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                    Text(s.dashboardStyle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            item { Spacer(Modifier) }

            item {
                DashboardStyleCard(
                    title = "Default",
                    style = DashboardStyle.DEFAULT,
                    selected = state.dashboardStyle == DashboardStyle.DEFAULT,
                    onClick = { onUpdate(state.copy(dashboardStyle = DashboardStyle.DEFAULT)) }
                )
            }
            item {
                DashboardStyleCard(
                    title = "Slide to connect",
                    style = DashboardStyle.SLIDER,
                    selected = state.dashboardStyle == DashboardStyle.SLIDER,
                    onClick = { onUpdate(state.copy(dashboardStyle = DashboardStyle.SLIDER)) }
                )
            }
            item {
                DashboardStyleCard(
                    title = "Centered button",
                    style = DashboardStyle.CENTERED,
                    selected = state.dashboardStyle == DashboardStyle.CENTERED,
                    onClick = { onUpdate(state.copy(dashboardStyle = DashboardStyle.CENTERED)) }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    val (primary, bg, name, desc) = when (preset) {
        ThemePreset.LIGHT -> listOf(Color(0xFF005AB5), Color(0xFFF3F4F6), "Light", "Clean white surfaces with high contrast blue")
        ThemePreset.DARK -> listOf(Color(0xFFFFB800), Color(0xFF10121A), "Dark", "Default dark theme with warm gold accents")
        ThemePreset.DRACULA -> listOf(Color(0xFFFF79C6), Color(0xFF282A36), "Dracula", "Dark background with vibrant pink and purple")
        ThemePreset.CATPPUCCIN -> listOf(Color(0xFFCBA6F7), Color(0xFF1E1E2E), "Catppuccin", "Soothing pastel theme with lavender hints")
        ThemePreset.NORD -> listOf(Color(0xFF88C0D0), Color(0xFF2E3440), "Nord", "Arctic, north-bluish cold dark theme")
        ThemePreset.GITHUB -> listOf(Color(0xFF58A6FF), Color(0xFF0D1117), "GitHub", "Classic GitHub dark mode aesthetics")
        ThemePreset.GRUVBOX -> listOf(Color(0xFFFABD2F), Color(0xFF282828), "Gruvbox", "Retro groove colors with warm yellow")
        ThemePreset.TOKYO_NIGHT -> listOf(Color(0xFF7AA2F7), Color(0xFF1A1B26), "Tokyo Night", "Dark blue theme inspired by neon lights")
        ThemePreset.MONOKAI -> listOf(Color(0xFFA6E22E), Color(0xFF272822), "Monokai", "Classic editor theme with vibrant green")
        ThemePreset.MATERIAL -> listOf(Color(0xFF80CBC4), Color(0xFF263238), "Material Ocean", "Deep blue-grey surfaces with teal accents")
        ThemePreset.SOLARIZED -> listOf(Color(0xFF2AA198), Color(0xFF002B36), "Solarized", "Solarized Dark Cyan")
        ThemePreset.ROSE_PINE -> listOf(Color(0xFFC4A7E7), Color(0xFF191724), "Rosé Pine", "Soft muted plum with an iris accent")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(48.dp)) {
                Box(Modifier.fillMaxSize().weight(1f).background(bg))
                Box(Modifier.fillMaxSize().weight(0.4f).background(primary))
            }
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(primary))
                    Spacer(Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun DashboardStyleCard(
    title: String,
    style: DashboardStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF13151D))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (style) {
                    DashboardStyle.DEFAULT -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(onSurface.copy(alpha = 0.3f)))
                            Box(Modifier.size(32.dp).clip(CircleShape).background(primary))
                            Box(Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(onSurface.copy(alpha = 0.3f)))
                        }
                    }
                    DashboardStyle.SLIDER -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(onSurface.copy(alpha = 0.3f)))
                            Box(Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(onSurface.copy(alpha = 0.3f)))
                            Box(Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(primary))
                        }
                    }
                    DashboardStyle.CENTERED -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(onSurface.copy(alpha = 0.3f)))
                            Box(Modifier.size(40.dp).clip(CircleShape).background(primary))
                            Box(Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(onSurface.copy(alpha = 0.3f)))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (selected) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}
