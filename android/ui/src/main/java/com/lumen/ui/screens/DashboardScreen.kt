package com.baloch.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baloch.ui.components.ConnectionSliderBar
import com.baloch.ui.components.ConnectionState
import com.baloch.ui.components.CountryFlagIcon
import com.baloch.ui.components.LivePingCard
import com.baloch.ui.components.SubscriptionDetailsDialog
import com.baloch.ui.components.SubscriptionProviderCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin

private val posterMemoryCache = mutableMapOf<String, ImageBitmap>()

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07080B))
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
            BalochScreenHeader(
                title = strings.balochPing,
                subtitle = "v${BalochVersion.appVersion}",
                stackSubtitleOnCompact = false,
                modifier = Modifier.padding(horizontal = 16.dp)
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
                    bottom = if (dashboardStyle == DashboardStyle.SLIDER && !isSelectionMode) 142.dp else 20.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "header_spacer") { Spacer(Modifier.height(4.dp)) }

                if (!isSelectionMode && dashboardStyle != DashboardStyle.SLIDER) {
                    item(key = "hero_connect") {
                        Column {
                            val heroNode = nodes.firstOrNull { it.isSelected } ?: nodes.firstOrNull()
                            DashboardHeroOriginal(
                                connectionState = connectionState,
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
                            Spacer(Modifier.height(14.dp))

                            GameBoostSection(selectedGameId = "pubg")
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                if (!isSelectionMode) {
                    item(key = "live_ping_card") {
                        LivePingCard(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
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
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(Modifier.width(6.dp))
                                Box {
                                    Text(
                                        text = serverSortLabel(sort, strings),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.clickable { showSortMenu = true }
                                    )
                                    BalochMenu(
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
                        BalochCard {
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
                                            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                            .background(Color(0xFF10121A))
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
                                        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                    } else {
                                        RoundedCornerShape(0.dp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(railShape)
                                            .background(Color(0xFF10121A))
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
                                        .border(1.dp, Color(0xFF1F2433), RoundedCornerShape(14.dp))
                                        .clickable { showImportMenu = true },
                                    color = Color(0xFF11141D),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 11.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = strings.add,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                BalochMenu(
                                    expanded = showImportMenu,
                                    onDismissRequest = { showImportMenu = false }
                                ) {
                                    Column {
                                        DropdownMenuItem(
                                            text = { Text(strings.importQrCode, color = MaterialTheme.colorScheme.onSurface) },
                                            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showImportMenu = false
                                                tick(HapticFeedbackType.LongPress)
                                                scanQr()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.importFromFile, color = MaterialTheme.colorScheme.onSurface) },
                                            trailingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
                                            onClick = {
                                                showImportMenu = false
                                                tick(HapticFeedbackType.LongPress)
                                                onImportFile()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.importManually, color = MaterialTheme.colorScheme.onSurface) },
                                            trailingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) },
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
                                    .border(1.dp, Color(0xFF1F2433), RoundedCornerShape(14.dp))
                                    .clickable {
                                        tick(HapticFeedbackType.LongPress)
                                        onImportClipboard()
                                    },
                                color = Color(0xFF11141D),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 11.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.paste,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
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
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
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
                    Icon(Icons.Filled.Folder, contentDescription = strings.moveToGroup, tint = goldColor)
                }
            }
            IconButton(onClick = onExportSelected) {
                Icon(Icons.Filled.Share, contentDescription = strings.exportSelected, tint = goldColor)
            }
            IconButton(onClick = onPingSelected) {
                Icon(Icons.Filled.PlayArrow, contentDescription = strings.ping, tint = goldColor)
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
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    } else {
        RoundedCornerShape(16.dp)
    }
    val goldColor = Color(0xFFFFB800)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tileShape)
            .border(1.dp, Color(0xFF1B1E2B), tileShape),
        color = Color(0xFF11141D),
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
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
                        color = Color(0xFF757D8A),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPingGroup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.NetworkCheck, contentDescription = "Ping", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                if (group.isSubscription) {
                    IconButton(onClick = onRefreshSubscription, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color(0xFF757D8A), modifier = Modifier.size(20.dp))
                    }

                    BalochMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
        RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(0.dp)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = -pullUp)
            .clip(infoShape)
            .background(Color(0xFF0D0F17))
            .border(1.dp, Color(0xFF1B1E2B), infoShape)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp + extraBottom)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = goldColor, modifier = Modifier.size(16.dp))
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
                color = Color(0xFF757D8A),
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
            color = Color(0xFF757D8A),
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
        isSelectionMode && isNodeSelected -> goldColor.copy(alpha = 0.16f)
        selected -> Color(0xFF141926)
        else -> Color(0xFF10121A)
    }
    var showActionMenu by remember { mutableStateOf(false) }
    val tileShape = RoundedCornerShape(14.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(tileShape)
            .border(
                1.dp,
                if (selected) goldColor.copy(alpha = 0.7f) else Color(0xFF1B1E2B),
                tileShape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = rowBg,
        shape = tileShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (modern) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (selected) goldColor else Color.Transparent)
                )
                Spacer(Modifier.width(10.dp))
            }
            if (isSelectionMode) {
                Checkbox(
                    checked = isNodeSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = goldColor,
                        uncheckedColor = Color(0xFF757D8A)
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            CountryFlagIcon(countryCode = node.countryCode, width = 34.dp, height = 23.dp)
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
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = protocolColor(node.protocol)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = node.displayProtocol.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontSize = 9.sp,
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
                        color = Color(0xFF757D8A),
                        fontSize = 11.sp,
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
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pingColor(ping).copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (ping >= 0) "$ping ms" else "—",
                            style = MaterialTheme.typography.bodySmall,
                            color = pingColor(ping),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showActionMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color(0xFF757D8A), modifier = Modifier.size(20.dp))
                    }
                    BalochMenu(expanded = showActionMenu, onDismissRequest = { showActionMenu = false }) {
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
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val goldColor = Color(0xFFFFB800)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10121A))
            .border(1.dp, Color(0xFF1B1E2B), RoundedCornerShape(14.dp))
            .clickable(enabled = !isChecking, onClick = onCheckPing)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = s.checkPing, style = MaterialTheme.typography.labelSmall, color = Color(0xFF757D8A), fontSize = 10.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
        Spacer(Modifier.height(3.dp))
        if (isChecking) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = goldColor)
        } else {
            Text(
                text = if (connectedPing.isNullOrBlank()) "—" else connectedPing,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (connectedPing.isNullOrBlank()) Color(0xFF757D8A) else goldColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun HeroStatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF10121A))
            .border(1.dp, Color(0xFF1B1E2B), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF757D8A), fontSize = 10.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==================== DASHBOARD HERO (ORIGINAL LUMEN LAYOUT: BUTTON LEFT, SPEED RIGHT) ====================
@Composable
private fun DashboardHeroOriginal(
    connectionState: ConnectionState,
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

    var sessionSeconds by remember { mutableStateOf(0L) }
    var history by remember { mutableStateOf(listOf<Float>()) }
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
            sessionSeconds = if (startedAt > 0L) ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L) else 0L
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

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            OriginalBalochConnectButton(
                connectionState = connectionState,
                onToggleConnection = onToggleConnection,
                modifier = Modifier.size(136.dp)
            )

            if (speedStatsEnabled) {
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatHeroSpeed(downSpeed),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
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
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                            )
                        } else {
                            drawLine(
                                color = Color(0xFF1E2333),
                                start = Offset(0f, size.height - 2f),
                                end = Offset(size.width, size.height - 2f),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↓ " + formatHeroSpeed(downSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connected) goldColor else Color(0xFF6B7280),
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "↑ " + formatHeroSpeed(upSpeed),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                modifier = Modifier.weight(0.75f)
            )
        }
    }
}

// ==================== ORIGINAL LUMEN CONNECT BUTTON (CLASSIC & CLEAN) ====================
@Composable
private fun OriginalBalochConnectButton(
    connectionState: ConnectionState,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = connectionState == ConnectionState.Connected
    val connecting = connectionState == ConnectionState.Connecting
    val isError = connectionState == ConnectionState.Error

    val goldColor = Color(0xFFFFB800)

    val transition = rememberInfiniteTransition(label = "OriginalBtnAnim")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (connecting) 1500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 4.dp.toPx()

            drawCircle(
                color = if (connected) goldColor.copy(alpha = 0.25f) else Color(0xFF1B2030),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            if (connected || connecting) {
                rotate(if (connecting) rotation else 0f, pivot = center) {
                    drawArc(
                        color = goldColor,
                        startAngle = -90f,
                        sweepAngle = if (connecting) 90f else 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(0.78f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (connected) {
                            listOf(Color(0xFF262010), Color(0xFF141724))
                        } else {
                            listOf(Color(0xFF171A26), Color(0xFF0F111A))
                        }
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = if (connected) goldColor else Color(0xFF1F2436),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isError) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxSize(0.45f)
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize(0.46f)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.42f
                    val strokeW = 4.dp.toPx()

                    drawArc(
                        color = if (connected) goldColor else Color(0xFF8A93A6),
                        startAngle = -55f,
                        sweepAngle = 290f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    drawLine(
                        color = if (connected) goldColor else Color(0xFF8A93A6),
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

// ==================== GAME BOOST SECTION ====================
data class BoostGame(
    val id: String,
    val name: String,
    val posterUrl: String,
    val isHot: Boolean = false
)

@Composable
fun GameBoostSection(
    modifier: Modifier = Modifier,
    selectedGameId: String = "pubg",
    onGameSelect: (String) -> Unit = {}
) {
    val strings = LocalStrings.current
    val games = remember {
        listOf(
            BoostGame(
                id = "pubg",
                name = "PUBG Mobile",
                posterUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/578080/library_600x900.jpg",
                isHot = true
            ),
            BoostGame(
                id = "cod",
                name = "Call of Duty",
                posterUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/1938090/library_600x900.jpg"
            ),
            BoostGame(
                id = "efootball",
                name = "eFootball",
                posterUrl = "https://cdn.cloudflare.steamstatic.com/steam/apps/1665460/library_600x900.jpg"
            ),
            BoostGame(
                id = "valorant",
                name = "Valorant",
                posterUrl = "https://cdn1.epicgames.com/offer/cbd5b3d310a54b12bf3fe8c41994174f/EGS_VALORANT_RiotGames_S2_1200x1600-b2f707f43db4af1658b1d4ed83134914"
            ),
            BoostGame(
                id = "genshin",
                name = "Genshin",
                posterUrl = "https://cdn1.epicgames.com/salesEvent/salesEvent/EGS_GenshinImpact_miHoYoLimited_S2_1200x1600-c12cd2583aeab61e766ed2fb5257376f"
            )
        )
    }

    var activeGame by remember { mutableStateOf(selectedGameId) }
    val goldColor = Color(0xFFFFB800)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0F1118))
            .border(1.dp, Color(0xFF1E212D), RoundedCornerShape(18.dp))
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
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strings.gameSelect,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = strings.allGames,
                color = Color(0xFF6B7280),
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(games.size) { index ->
                val game = games[index]
                val isSelected = game.id == activeGame

                GamePosterCard(
                    game = game,
                    isSelected = isSelected,
                    goldColor = goldColor,
                    onClick = {
                        activeGame = game.id
                        onGameSelect(game.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun GamePosterCard(
    game: BoostGame,
    isSelected: Boolean,
    goldColor: Color,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    var posterBitmap by remember { mutableStateOf(posterMemoryCache[game.id]) }
    var isLoading by remember { mutableStateOf(posterBitmap == null) }

    LaunchedEffect(game.id) {
        if (posterMemoryCache.containsKey(game.id)) {
            posterBitmap = posterMemoryCache[game.id]
            isLoading = false
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(game.posterUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    connectTimeout = 10000
                    readTimeout = 10000
                    instanceFollowRedirects = true
                }
                if (conn.responseCode in 200..299) {
                    val bytes = conn.inputStream.readBytes()
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (decoded != null) {
                        val img = decoded.asImageBitmap()
                        posterMemoryCache[game.id] = img
                        posterBitmap = img
                    }
                }
            } catch (_: Exception) {} finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .width(104.dp)
            .height(138.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131520))
            .border(
                width = if (isSelected) 2.2.dp else 1.dp,
                color = if (isSelected) goldColor else Color(0xFF1F2433),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (posterBitmap != null) {
            Image(
                bitmap = posterBitmap!!,
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF10121A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = goldColor.copy(alpha = 0.6f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF242A38), Color(0xFF0F1118))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = goldColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x35000000),
                        Color.Transparent,
                        Color(0xCC07080B),
                        Color(0xF507080B)
                    )
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                if (game.isHot) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(goldColor)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "HOT",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = game.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = strings.optimized,
                        color = Color(0xFF00E676),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
