package com.lumen.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.ui.components.ConnectionSliderBar
import com.lumen.ui.components.ConnectionState
import com.lumen.ui.components.CountryFlagIcon
import com.lumen.ui.components.LivePingCard
import com.lumen.ui.components.SubscriptionDetailsDialog
import com.lumen.ui.components.SubscriptionProviderCard
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    connectionState: ConnectionState,
    nodes: List<NodeUiModel>,
    subscriptions: List<SubscriptionUiModel>,
    serverGroups: List<ServerGroupUiModel> = emptyList(),
    speedStatsEnabled: Boolean = true,
    downloadSpeed: Long = 0L,
    uploadSpeed: Long = 0L,
    pingingNodeIds: Set<String> = emptySet(),
    dashboardStyle: DashboardStyle = DashboardStyle.DEFAULT,
    connectedPing: String? = null,
    isCheckingPing: Boolean = false,
    onCheckPing: () -> Unit = {},
    onToggleConnection: () -> Unit,
    onSelectNode: (NodeUiModel) -> Unit,
    onImportClipboard: () -> Unit,
    onImportFile: () -> Unit,
    onImportQr: () -> Unit,
    onAddManualNode: () -> Unit,
    onRefreshSubscription: (String) -> Unit = {},
    onDeleteSubscription: (String) -> Unit = {},
    onPingGroup: (String?) -> Unit = {},
    onEditNode: (NodeUiModel) -> Unit = {},
    onPingNode: (NodeUiModel) -> Unit = {},
    onCopyNodeLink: (NodeUiModel) -> Unit = {},
    onExportQrCode: (NodeUiModel) -> Unit = {},
    onDeleteNode: (NodeUiModel) -> Unit = {},
    onPingNodes: (List<NodeUiModel>) -> Unit = {},
    onExportNodesText: (Set<String>) -> String = { "" },
    onExportSubscriptionText: (String?) -> String = { "" },
    onCopyText: (String) -> Unit = {},
    onShareText: (String) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val goldColor = Color(0xFFFFB800)
    var showImportMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortKey by rememberUiPreference(SERVERS_SORT_PREF, ServerSort.DEFAULT.name)
    val sort = remember(sortKey) { serverSortOf(sortKey) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedNodeIds by remember { mutableStateOf(setOf<String>()) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedNodeIds = emptySet()
    }

    var showDonateDialog by remember { mutableStateOf(false) }
    var showUsdtDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    val tick = rememberHapticTick()
    val scanQr = rememberQrScanRequest(onImportQr)
    var propertiesSub by remember { mutableStateOf<SubscriptionUiModel?>(null) }

    val groups = remember(nodes, subscriptions, serverGroups, strings.manual, sort) {
        val comparator = serverSortComparator(sort)
        val byBucket = nodes.groupBy { it.groupId ?: it.subscriptionId }
        buildList {
            add(HomeServerGroup(GROUP_MANUAL, strings.groupDefault, byBucket[null].orEmpty().sortedWith(comparator)))
            serverGroups.forEach { custom ->
                add(
                    HomeServerGroup(
                        id = custom.id,
                        title = custom.name,
                        nodes = byBucket[custom.id].orEmpty().sortedWith(comparator),
                        isCustom = true
                    )
                )
            }
            subscriptions.forEach { subscription ->
                val subscriptionNodes = byBucket[subscription.id].orEmpty()
                if (subscriptionNodes.isNotEmpty()) {
                    add(HomeServerGroup(subscription.id, subscription.name, subscriptionNodes.sortedWith(comparator), true, subscription))
                }
            }
        }
    }
    val groupIds = remember(groups) { groups.map { it.id } }
    var expandedGroups by remember(groupIds) { mutableStateOf(groupIds.toSet()) }

    val serversGroupPref by rememberUiPreference("servers_last_group", GROUP_ALL)
    val activeGroupId = remember(nodes) {
        nodes.firstOrNull { it.isSelected }?.let { it.groupId ?: it.subscriptionId ?: GROUP_MANUAL }
    }
    val visibleGroups = remember(groups, serversGroupPref, activeGroupId) {
        if (serversGroupPref.isBlank() || serversGroupPref == GROUP_ALL) {
            groups.filter { it.id == activeGroupId }.ifEmpty { groups }
        } else {
            groups.filter { it.id == serversGroupPref }
                .ifEmpty { groups.filter { it.id == activeGroupId } }
                .ifEmpty { groups }
        }
    }

    // پس‌زمینه مشکی مات اولد (Matte Black)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08090D))
    ) {
        if (isSelectionMode) {
            SelectionTopBar(
                selectedCount = selectedNodeIds.size,
                onCancel = {
                    isSelectionMode = false
                    selectedNodeIds = emptySet()
                },
                onExportSelected = {
                    val text = onExportNodesText(selectedNodeIds)
                    if (text.isNotBlank()) onShareText(text)
                },
                onPingSelected = {
                    val selectedNodes = nodes.filter { it.id in selectedNodeIds }
                    if (selectedNodes.isNotEmpty()) onPingNodes(selectedNodes)
                }
            )
        } else {
            LumenScreenHeader(
                title = "بلوچ پینگ",
                subtitle = "v${LumenVersion.appVersion}",
                stackSubtitleOnCompact = false,
                modifier = Modifier.padding(horizontal = 16.dp),
                actions = {
                    IconButton(onClick = { uriHandler.openUri("https://github.com/krambovic/Lumen") }) {
                        Icon(Icons.Filled.Code, contentDescription = "GitHub", tint = goldColor)
                    }
                    Box {
                        IconButton(onClick = { showDonateDialog = true }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Donate", tint = goldColor)
                        }
                        LumenMenu(
                            expanded = showDonateDialog,
                            onDismissRequest = { showDonateDialog = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("DonationAlerts") },
                                onClick = {
                                    uriHandler.openUri("https://www.donationalerts.com/r/studiobebraedition")
                                    showDonateDialog = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("USDT TRC20") },
                                onClick = {
                                    showUsdtDialog = true
                                    showDonateDialog = false
                                }
                            )
                        }
                    }
                    if (showUsdtDialog) {
                        com.lumen.ui.components.LumenDialog(
                            title = "USDT TRC20",
                            onDismissRequest = { showUsdtDialog = false },
                            confirmText = "Скопировать",
                            onConfirm = {
                                clipboard.setText(AnnotatedString("TWHsuUDru4pXBcGpfeKfzymbkfajyFnb2s"))
                                showUsdtDialog = false
                            },
                            dismissText = "Закрыть",
                            onDismiss = { showUsdtDialog = false }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TWHsuUDru4pXBcGpfeKfzymbkfajyFnb2s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            )
        }

        val listState = rememberLazyListState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    bottom = if (dashboardStyle == DashboardStyle.SLIDER && !isSelectionMode) {
                        142.dp
                    } else {
                        20.dp
                    }
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "header_spacer") { Spacer(Modifier.height(4.dp)) }

                if (!isSelectionMode && dashboardStyle != DashboardStyle.SLIDER) {
                    item(key = "hero_connect") {
                        Column {
                            val heroNode = nodes.firstOrNull { it.isSelected } ?: nodes.firstOrNull()
                            DashboardHero(
                                connectionState = connectionState,
                                style = dashboardStyle,
                                speedStatsEnabled = speedStatsEnabled,
                                downSpeed = downloadSpeed,
                                upSpeed = uploadSpeed,
                                serverName = heroNode?.name,
                                serverCountryCode = heroNode?.countryCode,
                                serverProtocol = heroNode?.displayProtocol ?: heroNode?.protocol,
                                connectedPing = connectedPing,
                                isCheckingPing = isCheckingPing,
                                onCheckPing = onCheckPing,
                                onToggleConnection = {
                                    tick(HapticFeedbackType.LongPress)
                                    onToggleConnection()
                                }
                            )
                            Spacer(Modifier.height(12.dp))

                            // بخش انتخاب بازی
                            GameBoostSection(
                                selectedGameId = "pubg"
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                // کارت پینگ لایو با قاب زرد/طلایی گیمینگ
                if (!isSelectionMode) {
                    item(key = "live_ping_card") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.2.dp, goldColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                        ) {
                            LivePingCard(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (groups.isNotEmpty() && !isSelectionMode) {
                    item(key = "sort_and_ping_row") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.sortBy,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8A92A6)
                                )
                                Spacer(Modifier.width(6.dp))
                                Box {
                                    Text(
                                        text = serverSortLabel(sort, strings),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = goldColor,
                                        modifier = Modifier.clickable { showSortMenu = true }
                                    )
                                    LumenMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        Column {
                                            serverSortOptions(strings).forEach { (value, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                                                    trailingIcon = if (sort == value) {
                                                        { Icon(Icons.Filled.Check, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp)) }
                                                    } else null,
                                                    onClick = {
                                                        sortKey = value.name
                                                        showSortMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (groups.isEmpty()) {
                    item(key = "empty_servers_card") {
                        LumenCard {
                            Text(strings.noServers, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    visibleGroups.forEachIndexed { groupIndex, group ->
                        val isExpanded = group.id in expandedGroups

                        if (groupIndex > 0) {
                            item(key = "group_gap_${group.id}") { Spacer(Modifier.height(8.dp)) }
                        }

                        item(key = "group_header_${group.id}") {
                            SubscriptionHeaderTile(
                                group = group,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedGroups = if (isExpanded) expandedGroups - group.id else expandedGroups + group.id
                                },
                                onRefreshSubscription = { onRefreshSubscription(group.id) },
                                onDeleteSubscription = { onDeleteSubscription(group.id) },
                                onPingGroup = {
                                    if (group.isCustom) {
                                        onPingNodes(group.nodes)
                                    } else {
                                        onPingGroup(if (group.isSubscription) group.id else null)
                                    }
                                },
                                onShowProperties = { sub -> propertiesSub = sub },
                                onExportAll = { subId ->
                                    val text = if (group.isCustom) {
                                        onExportNodesText(group.nodes.mapTo(mutableSetOf()) { it.id })
                                    } else {
                                        onExportSubscriptionText(subId)
                                    }
                                    if (text.isNotBlank()) onShareText(text)
                                }
                            )
                        }

                        if (group.isSubscription && group.subscription != null && isExpanded) {
                            val subscription = group.subscription
                            item(key = "group_info_${group.id}") {
                                SubscriptionInfoBar(
                                    sub = subscription,
                                    pullUp = 0.dp,
                                    roundedBottom = false
                                )
                            }
                            item(key = "group_provider_${group.id}") {
                                SubscriptionProviderCard(
                                    sub = subscription,
                                    onOpenUrl = onOpenUrl
                                )
                            }
                        }

                        if (isExpanded) {
                            if (group.nodes.isEmpty()) {
                                item(key = "empty_group_${group.id}") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                                            .background(Color(0xFF12141D))
                                            .border(1.dp, goldColor.copy(alpha = 0.35f), RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.noServers,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(
                                    count = group.nodes.size,
                                    key = { index -> "node_${group.id}_${group.nodes[index].id}" }
                                ) { index ->
                                    val node = group.nodes[index]
                                    val isLast = index == group.nodes.lastIndex
                                    val railShape = if (isLast) {
                                        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                                    } else {
                                        RoundedCornerShape(0.dp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(railShape)
                                            .background(Color(0xFF101218))
                                            .padding(
                                                start = 8.dp,
                                                end = 8.dp,
                                                top = 4.dp,
                                                bottom = if (isLast) 10.dp else 4.dp
                                            )
                                    ) {
                                        ServerTileRow(
                                            node = node,
                                            isSelectionMode = isSelectionMode,
                                            isNodeSelected = node.id in selectedNodeIds,
                                            isPinging = node.id in pingingNodeIds,
                                            modern = true,
                                            onClick = {
                                                if (isSelectionMode) {
                                                    val newSet = if (node.id in selectedNodeIds) selectedNodeIds - node.id else selectedNodeIds + node.id
                                                    selectedNodeIds = newSet
                                                    if (newSet.isEmpty()) isSelectionMode = false
                                                } else {
                                                    onSelectNode(node)
                                                }
                                            },
                                            onLongClick = {
                                                tick(HapticFeedbackType.LongPress)
                                                isSelectionMode = true
                                                selectedNodeIds = setOf(node.id)
                                            },
                                            onEditNode = { onEditNode(node) },
                                            onPingNode = { onPingNode(node) },
                                            onCopyLink = { onCopyNodeLink(node) },
                                            onExportQr = { onExportQrCode(node) },
                                            onDeleteNode = { onDeleteNode(node) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val showImportActions = serversGroupPref.isBlank() ||
                    serversGroupPref == GROUP_ALL ||
                    serversGroupPref == GROUP_MANUAL ||
                    visibleGroups.any { !it.isSubscription && !it.isCustom }
                if (!isSelectionMode && showImportActions) {
                    item(key = "import_action_buttons") {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.2.dp, goldColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                        .clickable { showImportMenu = true },
                                    color = Color(0xFF12141D),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = goldColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = strings.add,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = goldColor
                                        )
                                    }
                                }

                                LumenMenu(
                                    expanded = showImportMenu,
                                    onDismissRequest = { showImportMenu = false }
                                ) {
                                    Column {
                                        DropdownMenuItem(
                                            text = { Text(strings.importQrCode, color = MaterialTheme.colorScheme.onSurface) },
                                            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showImportMenu = false
                                                tick(HapticFeedbackType.LongPress)
                                                scanQr()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.importFromFile, color = MaterialTheme.colorScheme.onSurface) },
                                            trailingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showImportMenu = false
                                                tick(HapticFeedbackType.LongPress)
                                                onImportFile()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.importManually, color = MaterialTheme.colorScheme.onSurface) },
                                            trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showImportMenu = false
                                                tick(HapticFeedbackType.LongPress)
                                                onAddManualNode()
                                            }
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.2.dp, goldColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        tick(HapticFeedbackType.LongPress)
                                        onImportClipboard()
                                    },
                                color = Color(0xFF12141D),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = goldColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.paste,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = goldColor
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            if (!isSelectionMode && dashboardStyle == DashboardStyle.SLIDER) {
                val heroNode = nodes.firstOrNull { it.isSelected } ?: nodes.firstOrNull()
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xFF08090D))
                        .padding(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 2.dp)
                ) {
                    DashboardHero(
                        connectionState = connectionState,
                        style = DashboardStyle.SLIDER,
                        speedStatsEnabled = speedStatsEnabled,
                        downSpeed = downloadSpeed,
                        upSpeed = uploadSpeed,
                        serverName = heroNode?.name,
                        serverCountryCode = heroNode?.countryCode,
                        serverProtocol = heroNode?.displayProtocol ?: heroNode?.protocol,
                        connectedPing = connectedPing,
                        isCheckingPing = isCheckingPing,
                        onCheckPing = onCheckPing,
                        onToggleConnection = {
                            tick(HapticFeedbackType.LongPress)
                            onToggleConnection()
                        }
                    )
                }
            }
        }
    }

    propertiesSub?.let { sub ->
        SubscriptionDetailsDialog(
            sub = sub,
            onDismiss = { propertiesSub = null },
            onCopyUrl = onCopyText,
            onOpenUrl = onOpenUrl
        )
    }
}

@Composable
internal fun SelectionTopBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onExportSelected: () -> Unit,
    onPingSelected: () -> Unit,
    onMoveSelected: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val goldColor = Color(0xFFFFB800)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$selectedCount ${strings.selected}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onMoveSelected != null) {
                IconButton(onClick = onMoveSelected) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = strings.moveToGroup,
                        tint = goldColor
                    )
                }
            }
            IconButton(onClick = onExportSelected) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = strings.exportSelected,
                    tint = goldColor
                )
            }
            IconButton(onClick = onPingSelected) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = strings.ping,
                    tint = goldColor
                )
            }
        }
    }
}

@Composable
fun SubscriptionHeaderTile(
    group: HomeServerGroup,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onPingGroup: () -> Unit = {},
    onShowProperties: (SubscriptionUiModel) -> Unit = {},
    onEditSubscription: ((SubscriptionUiModel) -> Unit)? = null,
    onExportAll: (String?) -> Unit = {},
    onRenameGroup: (() -> Unit)? = null,
    onDeleteGroup: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    var showMenu by remember { mutableStateOf(false) }
    val tileShape = if (isExpanded || group.isSubscription) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    } else {
        RoundedCornerShape(18.dp)
    }
    val goldColor = Color(0xFFFFB800)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tileShape)
            .border(1.2.dp, goldColor.copy(alpha = 0.45f), tileShape),
        color = Color(0xFF12141D),
        shape = tileShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (group.isSubscription) "${group.title} (${group.nodes.size})" else group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (group.isSubscription) "${strings.serverCountLabel(group.nodes.size)} | ${strings.autoUpdateOneHour}" else strings.serverCountLabel(group.nodes.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A92A6),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPingGroup,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NetworkCheck,
                        contentDescription = "Ping",
                        tint = goldColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (group.isSubscription) {
                    IconButton(
                        onClick = onRefreshSubscription,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = goldColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = Color(0xFF8A92A6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LumenMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        Column {
                            if (group.isSubscription) {
                                if (onEditSubscription != null) {
                                    DropdownMenuItem(
                                        text = { Text(strings.edit, color = MaterialTheme.colorScheme.onSurface) },
                                        trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            showMenu = false
                                            group.subscription?.let(onEditSubscription)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(strings.subscriptionProperties, color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { Icon(Icons.Filled.Info, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showMenu = false
                                        group.subscription?.let { onShowProperties(it) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.exportAll, color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onExportAll(group.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.deleteSubscription, color = MaterialTheme.colorScheme.error) },
                                    trailingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteSubscription()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text(strings.exportAll, color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onExportAll(if (group.isCustom) group.id else null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.ping, color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onPingGroup()
                                    }
                                )
                                if (group.isCustom && onRenameGroup != null) {
                                    DropdownMenuItem(
                                        text = { Text(strings.renameGroup, color = MaterialTheme.colorScheme.onSurface) },
                                        trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = goldColor, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            showMenu = false
                                            onRenameGroup()
                                        }
                                    )
                                }
                                if (group.isCustom && onDeleteGroup != null) {
                                    DropdownMenuItem(
                                        text = { Text(strings.deleteGroup, color = MaterialTheme.colorScheme.error) },
                                        trailingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                                        onClick = {
                                            showMenu = false
                                            onDeleteGroup()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionInfoBar(
    sub: SubscriptionUiModel,
    pullUp: androidx.compose.ui.unit.Dp = 6.dp,
    roundedBottom: Boolean = true,
    extraBottom: androidx.compose.ui.unit.Dp = 0.dp
) {
    val strings = LocalStrings.current
    val goldColor = Color(0xFFFFB800)
    val infoShape = if (roundedBottom) {
        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = -pullUp)
            .clip(infoShape)
            .background(Color(0xFF101218))
            .border(1.2.dp, goldColor.copy(alpha = 0.35f), infoShape)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp + extraBottom)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = sub.trafficSummary ?: "— / ∞",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = sub.expiryDaysLeft?.let { "$it ${strings.daysRemaining}" } ?: strings.expiresUnlimited,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8A92A6),
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { sub.trafficRatio ?: 0f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = goldColor,
            trackColor = goldColor.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append(strings.serverCountLabel(sub.nodeCount))
                sub.updateIntervalHours?.let { append("  |  \u21bb ${it}h") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A92A6),
            fontSize = 11.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ServerTileRow(
    node: NodeUiModel,
    isSelectionMode: Boolean,
    isNodeSelected: Boolean,
    isPinging: Boolean = false,
    supportingText: String? = null,
    modern: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditNode: () -> Unit,
    onPingNode: () -> Unit,
    onCopyLink: () -> Unit,
    onExportQr: () -> Unit,
    onDeleteNode: () -> Unit,
    onMoveToGroup: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val selected = node.isSelected
    val goldColor = Color(0xFFFFB800)
    val rowBg = when {
        isSelectionMode && isNodeSelected -> goldColor.copy(alpha = 0.22f)
        selected -> goldColor.copy(alpha = 0.16f)
        else -> Color(0xFF12141D)
    }
    var showActionMenu by remember { mutableStateOf(false) }
    val tileShape = RoundedCornerShape(if (modern) 18.dp else 12.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tileShape)
            .border(
                if (modern && selected) 2.dp else 1.2.dp,
                if (selected) goldColor else goldColor.copy(alpha = 0.35f),
                tileShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = rowBg,
        shape = tileShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (modern) 12.dp else 14.dp, vertical = if (modern) 13.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (modern) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(goldColor.copy(alpha = if (selected) 1f else 0f))
                )
                Spacer(Modifier.width(10.dp))
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = isNodeSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = goldColor,
                        uncheckedColor = Color(0xFF8A92A6)
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            CountryFlagIcon(
                countryCode = node.countryCode,
                width = 34.dp,
                height = 23.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) goldColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (modern) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeColor = protocolColor(node.protocol)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = node.displayProtocol.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (node.isAutoNode && node.displayProtocol.equals("AUTO", true)) {
                                strings.autoNodeDescriptionLabel
                            } else if (node.isAutoNode && node.displayProtocol.endsWith("/WARP", true)) {
                                "WARP"
                            } else {
                                "${node.server}:${node.port}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A92A6),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = node.displayProtocol,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A92A6),
                        fontSize = 11.sp
                    )
                }
                supportingText?.takeIf { it.isNotBlank() }?.let { detail ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = goldColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isPinging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = goldColor
                )
                Spacer(Modifier.width(8.dp))
            } else {
                node.pingMs?.let { ping ->
                    if (modern) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(pingColor(ping).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (ping >= 0) "$ping ms" else "—",
                                style = MaterialTheme.typography.bodySmall,
                                color = pingColor(ping),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = if (ping >= 0) "$ping ms" else "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = pingColor(ping),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(
                        onClick = { showActionMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null,
                            tint = if (selected) goldColor else Color(0xFF8A92A6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    LumenMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        Column {
                            DropdownMenuItem(
                                text = { Text(strings.edit, color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showActionMenu = false
                                    onEditNode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.copyLink, color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = { Icon(Icons.Filled.Share, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showActionMenu = false
                                    onCopyLink()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.exportQrCode, color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showActionMenu = false
                                    onExportQr()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.ping, color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = { Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showActionMenu = false
                                    onPingNode()
                                }
                            )
                            if (onMoveToGroup != null) {
                                DropdownMenuItem(
                                    text = { Text(strings.moveToGroup, color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, tint = goldColor, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showActionMenu = false
                                        onMoveToGroup()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                                trailingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showActionMenu = false
                                    onDeleteNode()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatHeroSpeed(bytesPerSecond: Long): String {
    val kb = bytesPerSecond / 1024.0
    return when {
        kb < 1.0 -> "0 KB/s"
        kb < 1024.0 -> String.format(java.util.Locale.US, "%.1f KB/s", kb)
        else -> String.format(java.util.Locale.US, "%.2f MB/s", kb / 1024.0)
    }
}

private fun formatHeroDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, secs)
    }
}

private fun heroServerLabel(countryCode: String?, protocol: String?, fallbackName: String?): String {
    val region = countryCode
        ?.takeIf { it.length == 2 }
        ?.let { code ->
            java.util.Locale("", code.uppercase(java.util.Locale.US))
                .getDisplayCountry(java.util.Locale.getDefault())
                .takeIf { it.isNotBlank() }
        }
        ?: fallbackName?.trim()?.takeIf { it.isNotBlank() }
    return region?.takeIf { it.isNotBlank() } ?: "\u2014"
}

@Composable
private fun HeroPingTile(
    connectedPing: String?,
    isChecking: Boolean,
    onCheckPing: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val s = LocalStrings.current
    val shape = RoundedCornerShape(if (compact) 13.dp else 18.dp)
    val goldColor = Color(0xFFFFB800)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF12141D))
            .border(1.2.dp, goldColor.copy(alpha = 0.55f), shape)
            .clickable(enabled = !isChecking, onClick = onCheckPing)
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 5.dp else 12.dp
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = s.checkPing,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A92A6),
                fontSize = if (compact) 9.sp else 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.NetworkCheck,
                contentDescription = s.checkPing,
                tint = goldColor,
                modifier = Modifier.size(if (compact) 10.dp else 12.dp)
            )
        }
        Spacer(Modifier.height(if (compact) 1.dp else 3.dp))
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (compact) 12.dp else 16.dp),
                strokeWidth = 2.dp,
                color = goldColor
            )
        } else {
            Text(
                text = if (connectedPing.isNullOrBlank()) "—" else connectedPing,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (connectedPing.isNullOrBlank()) {
                    Color(0xFF8A92A6)
                } else {
                    goldColor
                },
                fontSize = if (compact) 12.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardHero(
    connectionState: ConnectionState,
    style: DashboardStyle = DashboardStyle.DEFAULT,
    speedStatsEnabled: Boolean,
    downSpeed: Long,
    upSpeed: Long,
    serverName: String?,
    serverCountryCode: String?,
    serverProtocol: String?,
    connectedPing: String? = null,
    isCheckingPing: Boolean = false,
    onCheckPing: () -> Unit = {},
    onToggleConnection: () -> Unit
) {
    val s = LocalStrings.current
    val goldColor = Color(0xFFFFB800)
    val connected = connectionState == ConnectionState.Connected
    val connecting = connectionState == ConnectionState.Connecting

    var history by remember { mutableStateOf(listOf<Float>()) }
    var sessionSeconds by remember { mutableStateOf(0L) }

    val heroContext = androidx.compose.ui.platform.LocalContext.current
    val heroPrefs = remember {
        heroContext.getSharedPreferences("lumen_prefs", android.content.Context.MODE_PRIVATE)
    }

    LaunchedEffect(connected) {
        if (!connected) {
            sessionSeconds = 0L
            return@LaunchedEffect
        }
        while (true) {
            val startedAt = heroPrefs.getLong("session_started_at", 0L)
            sessionSeconds = if (startedAt > 0L) {
                ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
            delay(1_000)
        }
    }

    val currentDownSpeed by rememberUpdatedState(downSpeed)
    LaunchedEffect(connected, speedStatsEnabled) {
        if (!connected || !speedStatsEnabled) {
            history = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            history = (history + currentDownSpeed.toFloat()).takeLast(36)
            delay(1_000)
        }
    }

    // حالت سنتر / وسط‌چین (دکمه بزرگ طلایی کنسولی وسط صفحه)
    if (style != DashboardStyle.DEFAULT) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (style == DashboardStyle.SLIDER) 2.dp else 4.dp,
                    bottom = if (style == DashboardStyle.SLIDER) 2.dp else 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (style == DashboardStyle.CENTERED) {
                // رآکتور طلایی کنسولی با ابعاد ۱۹۰dp
                GamingConnectReactor(
                    connectionState = connectionState,
                    onToggleConnection = onToggleConnection,
                    modifier = Modifier.size(190.dp)
                )

                Spacer(Modifier.height(10.dp))

                // کپسول وضعیت اتصال با دور طلایی
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF12141D))
                        .border(1.2.dp, goldColor.copy(alpha = 0.60f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (connected) Color(0xFF00E676) else if (connecting) goldColor else Color(0xFFFF9800))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (connected) "اتصال فعال است" else if (connecting) "در حال اتصال..." else "اتصال قطع است",
                            color = if (connected) Color(0xFF00E676) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (style == DashboardStyle.SLIDER) 6.dp else 10.dp)
            ) {
                HeroStatTile(
                    label = s.serverLabel,
                    value = heroServerLabel(serverCountryCode, serverProtocol, serverName),
                    modifier = Modifier.weight(1f),
                    compact = style == DashboardStyle.SLIDER
                )
                HeroStatTile(
                    label = s.sessionLabel,
                    value = formatHeroDuration(sessionSeconds),
                    modifier = Modifier.weight(1f),
                    compact = style == DashboardStyle.SLIDER
                )
                HeroPingTile(
                    connectedPing = connectedPing,
                    isChecking = isCheckingPing,
                    onCheckPing = onCheckPing,
                    modifier = Modifier.weight(0.72f),
                    compact = style == DashboardStyle.SLIDER
                )
            }
            if (style == DashboardStyle.SLIDER) {
                Spacer(Modifier.height(6.dp))
                ConnectionSliderBar(
                    connectionState = connectionState,
                    onToggleConnection = onToggleConnection,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    // حالت پیش‌فرض (کنار هم)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (speedStatsEnabled) Arrangement.Start else Arrangement.Center
        ) {
            GamingConnectReactor(
                connectionState = connectionState,
                onToggleConnection = onToggleConnection,
                modifier = Modifier.size(136.dp)
            )

            if (speedStatsEnabled) {
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.speedLabel.uppercase(java.util.Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A92A6),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatHeroSpeed(downSpeed),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        val points = history
                        if (points.size >= 2) {
                            val maxValue = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
                            val stepX = size.width / (points.size - 1)
                            val path = Path()
                            points.forEachIndexed { index, value ->
                                val x = index * stepX
                                val y = size.height - (value / maxValue) * size.height * 0.9f
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = goldColor,
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )
                        } else {
                            drawLine(
                                color = goldColor.copy(alpha = 0.25f),
                                start = Offset(0f, size.height - 2f),
                                end = Offset(size.width, size.height - 2f),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "\u2193 " + formatHeroSpeed(downSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A92A6)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "\u2191 " + formatHeroSpeed(upSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A92A6)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeroStatTile(
                label = s.serverLabel,
                value = heroServerLabel(serverCountryCode, serverProtocol, serverName),
                modifier = Modifier.weight(1f)
            )
            HeroStatTile(
                label = s.sessionLabel,
                value = formatHeroDuration(sessionSeconds),
                modifier = Modifier.weight(1f)
            )
            HeroPingTile(
                connectedPing = connectedPing,
                isChecking = isCheckingPing,
                onCheckPing = onCheckPing,
                modifier = Modifier.weight(0.72f)
            )
        }
    }
}

@Composable
private fun HeroStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(if (compact) 13.dp else 18.dp)
    val goldColor = Color(0xFFFFB800)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF12141D))
            .border(1.2.dp, goldColor.copy(alpha = 0.55f), shape)
            .padding(
                horizontal = if (compact) 10.dp else 14.dp,
                vertical = if (compact) 5.dp else 12.dp
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A92A6),
            fontSize = if (compact) 9.sp else 11.sp
        )
        Spacer(Modifier.height(if (compact) 1.dp else 3.dp))
        Text(
            text = value,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 12.sp else 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== CYBER GAMING REACTOR CORE (ULTRA VISIBLE) ====================
@Composable
private fun GamingConnectReactor(
    connectionState: ConnectionState,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = connectionState == ConnectionState.Connected
    val connecting = connectionState == ConnectionState.Connecting
    val isError = connectionState == ConnectionState.Error

    val goldColor = Color(0xFFFFB800)
    val goldDark = Color(0xFFFF8C00)

    val transition = rememberInfiniteTransition(label = "GamingReactorTransition")

    val hudRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (connecting) 1800 else if (connected) 5500 else 14000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "hudRotation"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleConnection
            ),
        contentAlignment = Alignment.Center
    ) {
        // ۱. رادار بیرونی و رینگ‌های مکانیکی طلایی کاملاً واضح
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // هاله نور طلایی در پس‌زمینه
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        goldColor.copy(alpha = if (connected) 0.35f else 0.18f),
                        goldDark.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.05f
                ),
                radius = radius * 1.05f,
                center = center
            )

            // ۴ قطعه رینگ ضخیم طلایی در حال چرخش
            rotate(hudRotation, pivot = center) {
                for (i in 0 until 4) {
                    drawArc(
                        color = goldColor.copy(alpha = if (connected) 0.95f else 0.75f),
                        startAngle = i * 90f + 14f,
                        sweepAngle = 56f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // خطوط تیک مکانیکی هاد
                val tickCount = 12
                val tickLen = 5.dp.toPx()
                val tickR = radius - 4.dp.toPx()
                for (i in 0 until tickCount) {
                    val rad = Math.toRadians((i * 30.0))
                    val x1 = (center.x + (tickR - tickLen) * cos(rad)).toFloat()
                    val y1 = (center.y + (tickR - tickLen) * sin(rad)).toFloat()
                    val x2 = (center.x + tickR * cos(rad)).toFloat()
                    val y2 = (center.y + tickR * sin(rad)).toFloat()
                    drawLine(
                        color = goldColor.copy(alpha = 0.65f),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // ۲. دیسک متالیک سه‌بعدی با حاشیه طلایی درخشان
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF282D3D),
                            Color(0xFF1B1E2B),
                            Color(0xFF11131C)
                        )
                    )
                )
                .border(
                    width = 3.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            goldColor,
                            goldDark.copy(alpha = 0.35f),
                            goldColor,
                            goldDark.copy(alpha = 0.35f),
                            goldColor
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isError) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize(0.46f)
                )
            } else {
                // ۳. نماد پاور درشت، طلایی و برجسته
                Canvas(modifier = Modifier.fillMaxSize(0.48f)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.40f
                    val strokeW = 4.8.dp.toPx()

                    // کمان ۲۹۰ درجه با شکاف بالا
                    drawArc(
                        color = goldColor,
                        startAngle = -55f,
                        sweepAngle = 290f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // خط عمودی مرکز
                    drawLine(
                        color = goldColor,
                        start = Offset(cx, cy - r * 1.15f),
                        end = Offset(cx, cy - r * 0.05f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ==================== GAME BOOST / SELECTION SECTION ====================
data class BoostGame(
    val id: String,
    val name: String,
    val isHot: Boolean = false
)

@Composable
fun GameBoostSection(
    modifier: Modifier = Modifier,
    selectedGameId: String = "pubg",
    onGameSelect: (String) -> Unit = {}
) {
    val games = remember {
        listOf(
            BoostGame("pubg", "PUBG Mobile", isHot = true),
            BoostGame("cod", "Call of Duty"),
            BoostGame("freefire", "Free Fire"),
            BoostGame("valorant", "Valorant"),
            BoostGame("genshin", "Genshin")
        )
    }

    var activeGame by remember { mutableStateOf(selectedGameId) }
    val goldColor = Color(0xFFFFB800)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF12141D))
            .border(1.2.dp, goldColor.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "انتخاب بازی",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "همه بازی‌ها ›",
                color = Color(0xFF8A92A6),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(games.size) { index ->
                val game = games[index]
                val isSelected = game.id == activeGame

                val borderModifier = if (isSelected) {
                    Modifier.border(2.dp, goldColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.2.dp, goldColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                }

                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(124.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(borderModifier)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isSelected) {
                                    listOf(Color(0xFF262010), Color(0xFF14161F))
                                } else {
                                    listOf(Color(0xFF161924), Color(0xFF0F1118))
                                }
                            )
                        )
                        .clickable {
                            activeGame = game.id
                            onGameSelect(game.id)
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            if (game.isHot) {
                                Text(
                                    text = "HOT",
                                    color = goldColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) goldColor.copy(alpha = 0.15f) else Color(0xFF1C202E)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = game.name.take(2).uppercase(),
                                color = if (isSelected) goldColor else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = game.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "بهینه شده",
                                color = Color(0xFF00E676),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
