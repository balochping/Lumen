package com.lumen.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
    import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.lumen.ui.components.*

// Shared slow-out curve for settings transitions and card entrance.
private val PremiumEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// حذف کامل روسی و چینی - فقط فارسی و انگلیسی ماندند
private val LANGUAGES = listOf("fa", "en")
private enum class SettingsPage { HUB, SUBSCRIPTIONS, TRAFFIC, DNS, PING, APP, THEME, UPDATES }

private fun languageLabel(code: String): String = when (code) {
    "fa" -> "فارسی"
    else -> "English"
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit,
    onLanguageChange: (String) -> Unit,
    onOpenRouting: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenCommunity: () -> Unit,
    updateChecked: Boolean = false,
    updateIsChecking: Boolean = false,
    updateLatestVersion: String? = null,
    updateReleaseTag: String? = null,
    updateAvailable: Boolean = false,
    updateError: String? = null,
    updateIsDownloading: Boolean = false,
    updateDownloadProgress: Int? = null,
    onCheckUpdates: (Boolean) -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    resetToHubSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var page by rememberSaveable { mutableStateOf(SettingsPage.HUB) }

    BackHandler(enabled = page != SettingsPage.HUB) {
        page = SettingsPage.HUB
    }

    LaunchedEffect(resetToHubSignal) {
        if (resetToHubSignal > 0) {
            page = SettingsPage.HUB
        }
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            val dir = if (targetState != SettingsPage.HUB) 1 else -1
            (slideInHorizontally(tween(320, easing = PremiumEasing)) { dir * it / 6 } +
                fadeIn(tween(260, easing = PremiumEasing)) +
                scaleIn(tween(320, easing = PremiumEasing), initialScale = 0.98f))
                .togetherWith(
                    slideOutHorizontally(tween(320, easing = PremiumEasing)) { -dir * it / 6 } +
                        fadeOut(tween(180)) +
                        scaleOut(tween(320, easing = PremiumEasing), targetScale = 0.98f)
                )
        },
        label = "settings_page_transition"
    ) { currentPage ->
        if (currentPage == SettingsPage.THEME) {
            ThemeSettingsScreen(
                state = state,
                onUpdate = onUpdate,
                onBack = { page = SettingsPage.HUB },
                modifier = modifier
            )
            return@AnimatedContent
        }
        Column(
            modifier.fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            when (currentPage) {
                SettingsPage.HUB -> SettingsHub(
                    onTheme = { page = SettingsPage.THEME },
                    onSubscriptions = { page = SettingsPage.SUBSCRIPTIONS },
                    onTraffic = { page = SettingsPage.TRAFFIC },
                    onDns = { page = SettingsPage.DNS },
                    onPing = { page = SettingsPage.PING },
                    onApp = { page = SettingsPage.APP },
                    onUpdates = { page = SettingsPage.UPDATES },
                    onRouting = onOpenRouting,
                    onLogs = onOpenLogs,
                    onCommunity = onOpenCommunity
                )
                SettingsPage.SUBSCRIPTIONS -> {
                    LumenScreenHeader(title = s.subscriptionSettings, onBack = { page = SettingsPage.HUB }, applyStatusBarPadding = false)
                    SubscriptionSettings(state, onUpdate)
                }
                SettingsPage.TRAFFIC -> {
                    LumenScreenHeader(title = s.trafficSettings, onBack = { page = SettingsPage.HUB }, applyStatusBarPadding = false)
                    TrafficSettings(state, onUpdate)
                }
                SettingsPage.DNS -> {
                    LumenScreenHeader(title = s.dnsSettings, onBack = { page = SettingsPage.HUB }, applyStatusBarPadding = false)
                    DnsSettings(state, onUpdate)
                }
                SettingsPage.PING -> {
                    LumenScreenHeader(title = s.pingSettings, onBack = { page = SettingsPage.HUB }, applyStatusBarPadding = false)
                    PingSettings(state, onUpdate)
                }
                SettingsPage.APP -> {
                    LumenScreenHeader(title = s.appSettings, onBack = { page = SettingsPage.HUB }, applyStatusBarPadding = false)
                    AppSettings(state, onUpdate, onLanguageChange)
                }
                SettingsPage.UPDATES -> {
                    LaunchedEffect(updateChecked) {
                        if (!updateChecked) onCheckUpdates(false)
                    }
                    LumenScreenHeader(title = s.updates, onBack = { page = SettingsPage.HUB }, applyStatusBarPadding = false)
                    UpdateSettings(
                        state = state,
                        onUpdate = onUpdate,
                        checked = updateChecked,
                        isChecking = updateIsChecking,
                        latestVersion = updateLatestVersion,
                        releaseTag = updateReleaseTag,
                        updateAvailable = updateAvailable,
                        error = updateError,
                        isDownloading = updateIsDownloading,
                        downloadProgress = updateDownloadProgress,
                        onCheck = { onCheckUpdates(true) },
                        onInstall = onInstallUpdate
                    )
                }
                SettingsPage.THEME -> Unit
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsHub(
    onTheme: () -> Unit,
    onSubscriptions: () -> Unit,
    onTraffic: () -> Unit,
    onDns: () -> Unit,
    onPing: () -> Unit,
    onApp: () -> Unit,
    onUpdates: () -> Unit,
    onRouting: () -> Unit,
    onLogs: () -> Unit,
    onCommunity: () -> Unit
) {
    val s = LocalStrings.current
    LumenScreenHeader(title = s.settings, applyStatusBarPadding = false)
    Spacer(Modifier.height(8.dp))
    SectionHeader(s.categoryAppearance)
    SettingsCard {
        SettingsMenuRow(Icons.Filled.Palette, s.themeSettings, onTheme)
        SettingsDivider()
        SettingsMenuRow(Icons.Filled.Settings, s.appSettings, onApp)
    }
    SectionHeader(s.categoryConnection)
    SettingsCard {
        SettingsMenuRow(Icons.AutoMirrored.Filled.Send, s.trafficSettings, onTraffic)
        SettingsDivider()
        SettingsMenuRow(Icons.Filled.Dns, s.dnsSettings, onDns)
        SettingsDivider()
        SettingsMenuRow(Icons.Filled.Speed, s.pingSettings, onPing)
    }
    SectionHeader(s.categoryTunnel)
    SettingsCard {
        SettingsMenuRow(Icons.AutoMirrored.Filled.List, s.routing, onRouting)
    }
    SectionHeader(s.categoryProviders)
    SettingsCard {
        SettingsMenuRow(Icons.Filled.CloudDownload, s.subscriptionSettings, onSubscriptions)
    }
    SectionHeader(s.categoryOther)
    SettingsCard {
        SettingsMenuRow(Icons.Filled.Menu, s.logs, onLogs)
        SettingsDivider()
        SettingsMenuRow(Icons.Filled.CloudDownload, s.updates, onUpdates)
    }
    SectionHeader(s.infoSection)
    SettingsCard {
        InfoRow(s.version, LumenVersion.appVersion)
        SettingsDivider()
        InfoRow("sing-box extended", LumenVersion.ENGINE)
    }
    Spacer(Modifier.height(18.dp))
    SettingsCard {
        SettingsMenuRow(Icons.Filled.Person, s.community, onCommunity)
    }
}

@Composable
private fun UpdateSettings(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit,
    checked: Boolean,
    isChecking: Boolean,
    latestVersion: String?,
    releaseTag: String?,
    updateAvailable: Boolean,
    error: String?,
    isDownloading: Boolean,
    downloadProgress: Int?,
    onCheck: () -> Unit,
    onInstall: () -> Unit
) {
    val s = LocalStrings.current
    SectionHeader(s.infoSection)
    SettingsCard {
        InfoRow(s.currentVersion, LumenVersion.appVersion)
        if (!latestVersion.isNullOrBlank()) {
            SettingsDivider()
            InfoRow(s.latestVersion, latestVersion)
        }
        if (!releaseTag.isNullOrBlank()) {
            SettingsDivider()
            InfoRow(s.androidReleaseTag, releaseTag)
        }
    }
    Spacer(Modifier.height(12.dp))
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            s.autoCheckUpdates,
            s.autoCheckUpdatesDesc,
            state.autoCheckUpdates
        ) { onUpdate(state.copy(autoCheckUpdates = it)) }
        Spacer(Modifier.height(4.dp))
    }
    Spacer(Modifier.height(12.dp))
    val status = when {
        isChecking -> s.checkingUpdates
        isDownloading -> buildString {
            append(s.downloadingUpdate)
            downloadProgress?.let { append(" $it%") }
        }
        !error.isNullOrBlank() -> "${s.updateCheckFailed}: $error"
        checked && updateAvailable -> s.updateAvailable
        checked -> s.upToDate
        else -> s.updatesDesc
    }
    Text(
        text = status,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error.isNullOrBlank()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = onCheck,
        enabled = !isChecking && !isDownloading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isChecking) s.checkingUpdates else s.checkUpdates)
    }
    if (updateAvailable) {
        Spacer(Modifier.height(8.dp))
        if (isDownloading) {
            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { downloadProgress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onInstall,
            enabled = !isChecking && !isDownloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isDownloading) {
                    buildString {
                        append(s.downloadingUpdate)
                        downloadProgress?.let { append(" $it%") }
                    }
                } else {
                    s.downloadUpdate
                }
            )
        }
    }
}

@Composable
private fun SubscriptionSettings(state: SettingsUiState, onUpdate: (SettingsUiState) -> Unit) {
    val s = LocalStrings.current
    SectionHeader(s.requestParameters)
    SettingsCard {
        Spacer(Modifier.height(10.dp))
        LumenDropdown(
            label = "User-Agent",
            options = listOf(
                "Happ/2.18.3/Windows/2606241603601",
                "Lumen-Subscription/Android-${LumenVersion.appVersion}",
                "SFA/1.11.0",
                "clash.meta",
                "v2rayNG/1.10.31"
            ),
            selected = state.subscriptionUserAgent,
            onSelected = { onUpdate(state.copy(subscriptionUserAgent = it)) }
        )
        ToggleRow(
            s.sendHwid,
            s.sendHwidDesc,
            state.subscriptionSendHwid
        ) { onUpdate(state.copy(subscriptionSendHwid = it)) }
        if (state.subscriptionSendHwid) {
            TextSettingField("HWID", state.subscriptionHwid) {
                onUpdate(state.copy(subscriptionHwid = it.take(256).replace("\r", "").replace("\n", "")))
            }
        }
        ToggleRow(
            s.subscriptionUseProxyTun,
            s.subscriptionUseProxyTunDesc,
            state.subscriptionUseProxyTun
        ) { onUpdate(state.copy(subscriptionUseProxyTun = it)) }
        ToggleRow(
            s.subscriptionAllowHttp,
            s.subscriptionAllowHttpDesc,
            state.subscriptionAllowHttp
        ) { onUpdate(state.copy(subscriptionAllowHttp = it)) }
        Spacer(Modifier.height(6.dp))
    }
    SectionHeader(s.subscriptionAutoUpdate)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            s.subscriptionAutoUpdate,
            s.subscriptionAutoUpdateDesc,
            state.subscriptionAutoUpdateMinutes > 0
        ) { onUpdate(state.copy(subscriptionAutoUpdateMinutes = if (it) 240 else 0)) }
        if (state.subscriptionAutoUpdateMinutes > 0) {
            NumberField(s.subscriptionAutoUpdateInterval, state.subscriptionAutoUpdateMinutes) {
                onUpdate(state.copy(subscriptionAutoUpdateMinutes = it.coerceIn(15, 1440)))
            }
        }
        TextSettingField(s.subscriptionIncludeRegexLabel, state.subscriptionIncludeRegex) {
            onUpdate(state.copy(subscriptionIncludeRegex = it.take(512)))
        }
        TextSettingField(s.subscriptionExcludeRegexLabel, state.subscriptionExcludeRegex) {
            onUpdate(state.copy(subscriptionExcludeRegex = it.take(512)))
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.subscriptionConverter)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            s.subscriptionConverter,
            s.subscriptionConverterDesc,
            state.subscriptionConverterEnabled
        ) { onUpdate(state.copy(subscriptionConverterEnabled = it)) }
        if (state.subscriptionConverterEnabled) {
            TextSettingField(s.subscriptionConverterUrlLabel, state.subscriptionConverterUrl) {
                onUpdate(state.copy(subscriptionConverterUrl = it.take(512)))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.subscriptionProfile)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(
            s.allowOverrides,
            s.allowOverridesDesc,
            state.allowSubscriptionOverrides
        ) { onUpdate(state.copy(allowSubscriptionOverrides = it)) }
        Spacer(Modifier.height(4.dp))
    }
}

private val DNS_MODES = listOf("automatic", "android", "secure", "json")

private fun dnsModeLabel(mode: String, s: LumenStrings): String = when (mode) {
    "android" -> s.dnsModeAndroid
    "secure" -> s.dnsModeSecure
    "json" -> s.dnsModeJson
    else -> s.dnsModeAuto
}

private fun dnsModeHint(mode: String, s: LumenStrings): String = when (mode) {
    "android" -> s.dnsModeAndroidHint
    "secure" -> s.dnsModeSecureHint
    "json" -> s.dnsModeJsonHint
    else -> s.dnsModeAutoHint
}

@Composable
private fun DnsSettings(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit
) {
    val s = LocalStrings.current
    SectionHeader(s.dnsModeSection)
    SettingsCard {
        Spacer(Modifier.height(12.dp))
        DNS_MODES.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { mode ->
                    DnsModeChip(
                        label = dnsModeLabel(mode, s),
                        selected = state.dnsMode == mode,
                        modifier = Modifier.weight(1f)
                    ) { onUpdate(state.copy(dnsMode = mode)) }
                }
            }
        }
        Text(
            dnsModeHint(state.dnsMode, s),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp)
        )
    }

    if (state.dnsMode == "json") {
        SectionHeader(s.dnsModeJson)
        SettingsCard {
            Spacer(Modifier.height(8.dp))
            TextAreaSettingField(s.dnsCustomJsonLabel, state.dnsCustomJson) {
                onUpdate(state.copy(dnsCustomJson = it.take(65_536)))
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    SectionHeader(s.dnsDirectSection)
    SettingsCard {
        Spacer(Modifier.height(8.dp))
        TextAreaSettingField(s.dnsServersLabel, state.dnsDirectServers) {
            onUpdate(state.copy(dnsDirectServers = it.take(2048)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LumenDropdown(
                label = s.dnsTypeLabel,
                options = listOf("udp", "tcp", "tls", "https"),
                selected = state.dnsDirectType,
                onSelected = { onUpdate(state.copy(dnsDirectType = it)) },
                modifier = Modifier.weight(1f)
            )
            LumenDropdown(
                label = s.dnsStrategyLabel,
                options = listOf("prefer_ipv4", "prefer_ipv6", "ipv4_only", "ipv6_only"),
                selected = state.dnsDirectStrategy,
                onSelected = { onUpdate(state.copy(dnsDirectStrategy = it)) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    SectionHeader(s.dnsProxySection)
    SettingsCard {
        Spacer(Modifier.height(8.dp))
        TextAreaSettingField(s.dnsServersLabel, state.dnsProxyServers) {
            onUpdate(state.copy(dnsProxyServers = it.take(2048)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LumenDropdown(
                label = s.dnsTypeLabel,
                options = listOf("udp", "tcp", "tls", "https"),
                selected = state.dnsProxyType,
                onSelected = { onUpdate(state.copy(dnsProxyType = it)) },
                modifier = Modifier.weight(1f)
            )
            LumenDropdown(
                label = s.dnsStrategyLabel,
                options = listOf("prefer_ipv4", "prefer_ipv6", "ipv4_only", "ipv6_only"),
                selected = state.dnsProxyStrategy,
                onSelected = { onUpdate(state.copy(dnsProxyStrategy = it)) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(4.dp))
        SettingsDivider()
        ToggleRow(s.dnsIpv4Only, s.dnsIpv4OnlyDesc, state.dnsProxyIpv4Only) {
            onUpdate(state.copy(dnsProxyIpv4Only = it))
        }
        Spacer(Modifier.height(4.dp))
    }

    SectionHeader(s.dnsBehaviorSection)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(s.dnsHijack, s.dnsHijackDesc, state.dnsHijackEnabled) {
            onUpdate(state.copy(dnsHijackEnabled = it))
        }
        SettingsDivider()
        ToggleRow(s.dnsFakeIp, s.dnsFakeIpDesc, state.dnsFakeIpEnabled) {
            onUpdate(state.copy(dnsFakeIpEnabled = it))
        }
        SettingsDivider()
        ToggleRow(s.dnsParallel, s.dnsParallelDesc, state.dnsParallelQuery) {
            onUpdate(state.copy(dnsParallelQuery = it))
        }
        SettingsDivider()
        ToggleRow(s.dnsOptimistic, s.dnsOptimisticDesc, state.dnsOptimisticCache) {
            onUpdate(state.copy(dnsOptimisticCache = it))
        }
        SettingsDivider()
        ToggleRow(s.dnsGeoCheck, s.dnsGeoCheckDesc, state.dnsGeoCheck) {
            onUpdate(state.copy(dnsGeoCheck = it))
        }
        Spacer(Modifier.height(4.dp))
    }

    SectionHeader(s.dnsHostsSection)
    SettingsCard {
        Spacer(Modifier.height(8.dp))
        TextAreaSettingField(s.dnsHostsLabel, state.dnsHosts) { onUpdate(state.copy(dnsHosts = it.take(4096))) }
        SettingsDivider()
        ToggleRow(s.dnsOverride, s.dnsOverrideDesc, state.dnsOverrideEnabled) {
            onUpdate(state.copy(dnsOverrideEnabled = it))
        }
        if (state.dnsOverrideEnabled) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1.4f)) {
                    TextSettingField(s.dnsHostname, state.dnsOverrideHostname) {
                        onUpdate(state.copy(dnsOverrideHostname = it.take(253)))
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextSettingField(s.dnsIpv4, state.dnsOverrideIpv4) {
                        onUpdate(state.copy(dnsOverrideIpv4 = it.take(15)))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DnsModeChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TrafficSettings(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit
) {
    val s = LocalStrings.current
    SectionHeader(s.connection)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow("Multiplex (MUX)", s.muxDescription, state.muxEnabled) {
            onUpdate(state.copy(muxEnabled = it))
        }
        if (state.muxEnabled) {
            NumberField(s.muxConcurrency, state.muxConcurrency) {
                onUpdate(state.copy(muxConcurrency = it.coerceIn(1, 1024)))
            }
            NumberField(s.muxMinStreamsLabel, state.multiplexMinStreams) {
                onUpdate(state.copy(multiplexMinStreams = it.coerceIn(0, 1024)))
            }
            LumenDropdown(
                label = s.muxProtocolLabel,
                options = MULTIPLEX_PROTOCOLS,
                selected = state.multiplexProtocol,
                onSelected = { onUpdate(state.copy(multiplexProtocol = it)) }
            )
            ToggleRow(s.muxPadding, s.muxPaddingDesc, state.multiplexPadding) {
                onUpdate(state.copy(multiplexPadding = it))
            }
            ToggleRow(s.muxBrutal, s.muxBrutalDesc, state.multiplexBrutalEnabled) {
                onUpdate(state.copy(multiplexBrutalEnabled = it))
            }
            if (state.multiplexBrutalEnabled) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        NumberField(s.muxBrutalUpLabel, state.multiplexBrutalUpMbps) {
                            onUpdate(state.copy(multiplexBrutalUpMbps = it.coerceIn(0, 10000)))
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        NumberField(s.muxBrutalDownLabel, state.multiplexBrutalDownMbps) {
                            onUpdate(state.copy(multiplexBrutalDownMbps = it.coerceIn(0, 10000)))
                        }
                    }
                }
            }
        }
        ToggleRow(s.tlsFragmentation, s.tlsDescription, state.fragmentEnabled) {
            onUpdate(state.copy(fragmentEnabled = it))
        }
        NumberField(s.tunnelMtu, state.mtu) { onUpdate(state.copy(mtu = it.coerceIn(1280, 9000))) }
        ToggleRow(s.preferIpv6, s.preferIpv6Description, state.preferIpv6) {
            onUpdate(state.copy(preferIpv6 = it))
        }
        ToggleRow(s.blockQuic, s.blockQuicDescription, state.blockQuic) {
            onUpdate(state.copy(blockQuic = it))
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.outboundSection)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(s.tcpFastOpen, s.tcpFastOpenDesc, state.outboundTcpFastOpen) {
            onUpdate(state.copy(outboundTcpFastOpen = it))
        }
        SettingsDivider()
        ToggleRow(s.tcpMultiPath, s.tcpMultiPathDesc, state.outboundTcpMultiPath) {
            onUpdate(state.copy(outboundTcpMultiPath = it))
        }
        SettingsDivider()
        ToggleRow(s.udpFragmentLabel, s.udpFragmentDesc, state.outboundUdpFragment) {
            onUpdate(state.copy(outboundUdpFragment = it))
        }
        SettingsDivider()
        ToggleRow(s.udpOverTcpLabel, s.udpOverTcpDesc, state.udpOverTcp) {
            onUpdate(state.copy(udpOverTcp = it))
        }
        NumberField(s.connectTimeoutLabel, state.outboundConnectTimeoutSeconds) {
            onUpdate(state.copy(outboundConnectTimeoutSeconds = it.coerceIn(0, 600)))
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Socks5CredentialRow(label: String, value: String) {
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value.ifBlank { "\u2014" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(
            onClick = {
                clipboard.setText(AnnotatedString(value))
                Toast.makeText(context, s.copied, Toast.LENGTH_SHORT).show()
            },
            enabled = value.isNotBlank()
        ) {
            Text(s.copyAction)
        }
    }
}

@Composable
private fun PingSettings(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit
) {
    val s = LocalStrings.current
    SectionHeader(s.ping)
    SettingsCard {
        Spacer(Modifier.height(10.dp))
        LumenDropdown(
            label = s.pingTypeLabel,
            options = PING_TYPES,
            selected = state.pingType,
            onSelected = { onUpdate(state.copy(pingType = it)) },
            optionLabel = {
                when (it) {
                    "tcping" -> "TCPing"
                    "icmp" -> "ICMP"
                    "http" -> "HTTP GET"
                    "real" -> "Real HTTP"
                    else -> it
                }
            }
        )
        if (state.pingType == "real" || state.pingType == "http") {
            TextSettingField(s.pingUrlLabel, state.pingUrl) { onUpdate(state.copy(pingUrl = it.take(256))) }
            LumenDropdown(
                label = s.pingUrlPresets,
                options = PING_URL_PRESETS,
                selected = state.pingUrl.takeIf { it in PING_URL_PRESETS } ?: PING_URL_PRESETS.first(),
                onSelected = { onUpdate(state.copy(pingUrl = it)) }
            )
        }
        NumberField(s.pingTimeoutLabel, state.pingTimeoutMs) {
            onUpdate(state.copy(pingTimeoutMs = it.coerceIn(500, 20000)))
        }
        NumberField(s.pingConcurrencyLabel, state.pingConcurrency) {
            onUpdate(state.copy(pingConcurrency = it.coerceIn(1, 32)))
        }
        NumberField(s.pingAttemptsLabel, state.pingAttempts) {
            onUpdate(state.copy(pingAttempts = it.coerceIn(1, 10)))
        }
        if (state.pingAttempts > 1) {
            LumenDropdown(
                label = s.pingAggregateLabel,
                options = PING_AGGREGATES,
                selected = state.pingAggregate,
                onSelected = { onUpdate(state.copy(pingAggregate = it)) },
                optionLabel = {
                    when (it) {
                        "avg" -> s.aggregateAvg
                        "median" -> s.aggregateMedian
                        else -> s.aggregateMin
                    }
                }
            )
            NumberField(s.pingRetryDelayLabel, state.pingRetryDelayMs) {
                onUpdate(state.copy(pingRetryDelayMs = it.coerceIn(0, 5000)))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
    SectionHeader(s.pingThresholds)
    SettingsCard {
        Spacer(Modifier.height(10.dp))
        NumberField(s.pingGoodLabel, state.pingGoodMs) {
            onUpdate(state.copy(pingGoodMs = it.coerceIn(10, 2000)))
        }
        NumberField(s.pingFairLabel, state.pingFairMs) {
            onUpdate(state.copy(pingFairMs = it.coerceIn(state.pingGoodMs + 10, 5000)))
        }
        Spacer(Modifier.height(6.dp))
    }
    SectionHeader(s.behavior)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(s.pingAutoOnOpen, s.pingAutoOnOpenDesc, state.pingAutoOnOpen) {
            onUpdate(state.copy(pingAutoOnOpen = it))
        }
        SettingsDivider()
        ToggleRow(
            s.pingAutoDeleteUnreachable,
            s.pingAutoDeleteUnreachableDesc,
            state.pingAutoDeleteUnreachable
        ) { onUpdate(state.copy(pingAutoDeleteUnreachable = it)) }
        if (state.pingAutoDeleteUnreachable) {
            NumberField(
                s.pingAutoDeleteThresholdLabel,
                state.pingAutoDeleteThresholdMs
            ) {
                onUpdate(state.copy(pingAutoDeleteThresholdMs = it.coerceIn(0, 100)))
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = {
                onUpdate(
                    state.copy(
                        pingType = "http",
                        pingTimeoutMs = 2000,
                        pingConcurrency = 16,
                        pingUrl = PING_URL_PRESETS.first(),
                        pingAttempts = 1,
                        pingAggregate = "min",
                        pingRetryDelayMs = 200,
                        pingGoodMs = 150,
                        pingFairMs = 300,
                        pingAutoOnOpen = false,
                        pingAutoDeleteUnreachable = false,
                        pingAutoDeleteThresholdMs = 1
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(s.resetToDefaults)
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.autoSelect)
    SettingsCard {
        Spacer(Modifier.height(10.dp))
        LumenDropdown(
            label = s.autoSelectUrl,
            options = listOf(
                "https://www.gstatic.com/generate_204",
                "https://cp.cloudflare.com/generate_204",
                "https://www.google.com/generate_204",
                "https://connectivitycheck.platform.hicloud.com/generate_204"
            ),
            selected = state.urlTestUrl,
            onSelected = { onUpdate(state.copy(urlTestUrl = it)) }
        )
        NumberField(s.checkInterval, state.urlTestIntervalMinutes) {
            onUpdate(state.copy(urlTestIntervalMinutes = it.coerceIn(1, 1440)))
        }
        NumberField(s.toleranceMs, state.urlTestToleranceMs) {
            onUpdate(state.copy(urlTestToleranceMs = it.coerceIn(0, 5000)))
        }
        NumberField(s.urlTestIdleTimeoutLabel, state.urlTestIdleTimeoutMinutes) {
            onUpdate(state.copy(urlTestIdleTimeoutMinutes = it.coerceIn(0, 1440)))
        }
        SettingsDivider()
        ToggleRow(s.urlTestInterrupt, s.urlTestInterruptDesc, state.urlTestInterruptExistConnections) {
            onUpdate(state.copy(urlTestInterruptExistConnections = it))
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun AppSettings(
    state: SettingsUiState,
    onUpdate: (SettingsUiState) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val s = LocalStrings.current

    SectionHeader(s.behavior)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(s.vibration, s.vibrationDesc, state.hapticsEnabled) {
            onUpdate(state.copy(hapticsEnabled = it))
        }
        SettingsDivider()
        ToggleRow(s.telemetryEnabled, s.telemetryEnabledDesc, state.telemetryEnabled) {
            onUpdate(state.copy(telemetryEnabled = it))
        }
        SettingsDivider()
        ToggleRow(s.loggingEnabled, s.loggingEnabledDesc, state.loggingEnabled) {
            onUpdate(state.copy(loggingEnabled = it))
        }
        SettingsDivider()
        ToggleRow(s.autoReconnectNetwork, s.autoReconnectNetworkDesc, state.reconnectOnNetworkChange) {
            onUpdate(state.copy(reconnectOnNetworkChange = it))
        }
        SettingsDivider()
        ToggleRow(s.validateProxyDataPath, s.validateProxyDataPathDesc, state.validateProxyDataPath) {
            onUpdate(state.copy(validateProxyDataPath = it))
        }
        SettingsDivider()
        ToggleRow(s.autoConnect, s.autoConnectDescription, state.autoConnectOnBoot) {
            onUpdate(state.copy(autoConnectOnBoot = it))
        }
        SettingsDivider()
        ToggleRow(s.speedStats, s.speedStatsDesc, state.enableSpeedStats) {
            onUpdate(state.copy(enableSpeedStats = it))
        }
        SettingsDivider()
        ToggleRow(s.showNotification, s.showNotificationDesc, state.showNotification) {
            onUpdate(state.copy(showNotification = it))
        }
        if (state.showNotification) {
            SettingsDivider()
            ToggleRow(s.notificationSpeed, s.notificationSpeedDesc, state.showNotificationSpeed) {
                onUpdate(state.copy(showNotificationSpeed = it))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.localProxy)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(s.proxyOnly, s.proxyOnlyDesc, state.proxyOnly) {
            onUpdate(
                state.copy(
                    proxyOnly = it,
                    localInboundEnabled = if (it) true else state.localInboundEnabled
                )
            )
        }
        SettingsDivider()
        ToggleRow(
            s.localInbound,
            s.localInboundDescription,
            state.localInboundEnabled,
            enabled = !state.proxyOnly
        ) {
            onUpdate(state.copy(localInboundEnabled = it))
        }
        if (state.localInboundEnabled) {
            SettingsDivider()
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    NumberField(s.socksPort, state.localSocksPort) {
                        onUpdate(state.copy(localSocksPort = it.coerceIn(1024, 65535)))
                    }
                }
                Box(Modifier.weight(1f)) {
                    NumberField(s.httpPort, state.localHttpPort) {
                        onUpdate(state.copy(localHttpPort = it.coerceIn(1024, 65535)))
                    }
                }
            }
            SettingsDivider()
            ToggleRow(s.allowLan, s.allowLanDescription, state.lanSharingEnabled) {
                onUpdate(state.copy(lanSharingEnabled = it))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.socks5Auth)
    SettingsCard {
        Spacer(Modifier.height(4.dp))
        ToggleRow(s.socks5Auth, s.socks5AuthDesc, state.socks5AuthEnabled) { enabled ->
            onUpdate(
                state.copy(
                    socks5AuthEnabled = enabled,
                    socks5Username = state.socks5Username.ifEmpty { generateSocks5Username() },
                    socks5Password = state.socks5Password.ifEmpty { generateSocks5Password() }
                )
            )
        }
        if (state.socks5AuthEnabled) {
            SettingsDivider()
            Socks5CredentialsEditor(
                username = state.socks5Username,
                password = state.socks5Password,
                onSave = { username, password ->
                    onUpdate(state.copy(socks5Username = username, socks5Password = password))
                }
            )
        }
        Spacer(Modifier.height(4.dp))
    }
    SectionHeader(s.language)
    LumenDropdown(
        label = "",
        options = LANGUAGES,
        selected = state.language.ifBlank { "fa" }, 
        onSelected = {
            onUpdate(state.copy(language = it))
            onLanguageChange(it)
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        optionLabel = { languageLabel(it) }
    )
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(340, easing = PremiumEasing),
        label = "settings_card_appear"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear
                translationY = (1f - appear) * 24f
                scaleX = 0.98f + 0.02f * appear
                scaleY = scaleX
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), shape)
            .padding(horizontal = 14.dp)
    ) { content() }
}

@Composable
private fun SettingsMenuRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SettingsDivider() {
    Spacer(
        Modifier.fillMaxWidth().height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }
        LumenSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value, focused) { if (!focused) text = value.toString() }
    OutlinedTextField(
        value = text,
        onValueChange = { input -> text = input },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { text.toIntOrNull()?.let(onChange) }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { focusState ->
                if (focused && !focusState.isFocused) text.toIntOrNull()?.let(onChange)
                focused = focusState.isFocused
            }
    )
}

@Composable
private fun TextSettingField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun TextAreaSettingField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = 3,
        maxLines = 6,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
