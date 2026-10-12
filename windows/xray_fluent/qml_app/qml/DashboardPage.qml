import QtQuick
import QtQuick.Controls.Universal
import QtQuick.Layouts
import App 1.0
import "."

//   • Подключение   — state, selected server, start/stop, network mode, live speed.
//   • Серверы       — group picker, search, quick connect and the filtered server list.
//   • Задержка/Сессия — RTT history bars, session totals and peak.
//   • Трафик приложений — top processes with relative bars.
//   • Маршрутизация — default and custom routing presets.
FluentScroll {
    id: page
    roundedClip: false
    readonly property bool twoColumns: width >= 960
    readonly property int gap: 16
    property string quickHint: ""
    property var rttHistory: []
    property real sessionPeak: 0
    property real appMax: 1

    // ---- formatting helpers (match dashboard_page.py) ----------------
    function fmtSpeed(bps) {
        if (bps < 1024) return Math.round(bps) + " B/s";
        if (bps < 1048576) return (bps / 1024).toFixed(2) + " KB/s";
        if (bps < 1073741824) return (bps / 1048576).toFixed(2) + " MB/s";
        return (bps / 1073741824).toFixed(2) + " GB/s";
    }
    function fmtBytes(b) {
        if (b < 1024) return Math.round(b) + " B";
        if (b < 1048576) return (b / 1024).toFixed(1) + " KB";
        if (b < 1073741824) return (b / 1048576).toFixed(1) + " MB";
        return (b / 1073741824).toFixed(2) + " GB";
    }
    function valuePart(text) { var i = text.lastIndexOf(" "); return i > 0 ? text.slice(0, i) : text; }
    function unitPart(text) { var i = text.lastIndexOf(" "); return i > 0 ? text.slice(i + 1) : ""; }
    function processLabel(value) {
        var name = String(value || "").replace(/\\/g, "/").split("/").pop();
        return name || I18n.t("Приложение");
    }
    function stateTitle() {
        if (App.transitionBusy)
            return App.transitionTargetConnected ? I18n.t("Подключение…") : I18n.t("Отключение…");
        return App.connected ? I18n.t("Подключено") : I18n.t("Ожидание");
    }
    function modeKey() {
        return App.tunMode ? "tun" : (App.proxyEnabled ? "system" : "local");
    }
    function modeTitle() {
        var key = page.modeKey();
        return key === "tun" ? "VPN (TUN)" : (key === "system" ? I18n.t("Системный прокси") : I18n.t("Локальный прокси"));
    }
    function setMode(key) {
        if (App.transitionBusy || key === page.modeKey())
            return;
        if (key === "tun") {
            App.setTun(true);
        } else if (key === "system") {
            App.setProxy(true);
        } else {
            if (App.tunMode) App.setTun(false);
            if (App.proxyEnabled) App.setProxy(false);
        }
    }
    function routingPresetIndex() {
        var items = App.routingPresetOptions;
        for (var i = 0; i < items.length; i++) {
            if (items[i].id === App.activeRoutingPresetId)
                return i;
        }
        return Math.max(0, Math.min(1, items.length - 1));
    }
    function connectionServerName() {
        return App.selectedNodeName.length > 0 ? App.selectedNodeName : I18n.t("Сервер не выбран");
    }
    function quickConnect() {
        var nodeId = App.quickConnectNodeId();
        if (!nodeId) {
            page.quickHint = I18n.t("Нет серверов с измеренным пингом — проверьте пинг на странице «Серверы»");
            return;
        }
        page.quickHint = "";
        App.selectNode(nodeId);
        if (!App.connected && !App.transitionBusy)
            App.toggleConnection();
    }
    // ---- multi-selection (Ctrl+A / Ctrl+click) for the context menu ----
    property var selection: ({})
    readonly property int selCount: Object.keys(selection).length
    function clearSelection() { if (page.selCount > 0) page.selection = ({}); }
    onVisibleChanged: if (!visible) page.clearSelection()
    function selectAllServers() {
        var ids = App.dashboardNodeIds();
        var o = {};
        for (var i = 0; i < ids.length; i++) if (ids[i]) o[ids[i]] = true;
        page.selection = o;
    }
    function toggleSelected(nodeId) {
        var o = Object.assign({}, page.selection);
        if (o[nodeId]) delete o[nodeId]; else o[nodeId] = true;
        page.selection = o;
    }
    // Targets of a menu action: the whole selection when the clicked row is part of it.
    function menuTargets() {
        if (page.selCount > 1 && page.selection[page.menuNodeId]) return Object.keys(page.selection);
        return page.menuNodeId ? [page.menuNodeId] : [];
    }
    readonly property bool menuMulti: ctxMenu.opened && page.selCount > 1 && !!page.selection[page.menuNodeId]
    function countText(single, multi) {
        return page.menuMulti ? I18n.t(multi, { count: page.selCount }) : I18n.t(single);
    }
    readonly property var groupModel: {
        var groups = (App.groupOptions || []).slice();
        var subs = App.subscriptions || [];
        for (var i = 0; i < subs.length; ++i) {
            var group = String(subs[i].group || subs[i].name || "Default");
            if (groups.indexOf(group) < 0) groups.push(group);
        }
        return [I18n.t("Все группы")].concat(groups);
    }
    function groupIndex() {
        var idx = page.groupModel.indexOf(App.nodeFilterGroup);
        return idx < 0 ? 0 : idx;
    }
    function selectGroup(index) {
        page.clearSelection();
        App.setNodeFilter(index <= 0 ? "" : page.groupModel[index], "");
    }
    function serverCaption() {
        var parts = [];
        if (App.selectedNodeProtocol.length > 0) parts.push(App.selectedNodeProtocol);
        if (App.selectedNodeSource.length > 0) parts.push(App.selectedNodeSource);
        return parts.join(" · ");
    }
    property string menuNodeId: ""

    component DashboardFlag: Item {
        id: flagBox
        readonly property bool hasSource: App.selectedNodeFlagSource.length > 0
        readonly property bool hasEmoji: App.selectedNodeFlag.length > 0
        readonly property bool imageReady: hasSource && flagImg.status === Image.Ready
        implicitWidth: (hasSource || hasEmoji) ? 32 : 0
        implicitHeight: 24
        visible: implicitWidth > 0
        clip: true

        Image {
            id: flagImg
            objectName: "dashboardFlagImage"
            anchors.fill: parent
            z: 1
            visible: flagBox.imageReady
            source: App.selectedNodeFlagSource
            fillMode: Image.PreserveAspectFit
            sourceSize.width: 96
            sourceSize.height: 72
            smooth: true
            mipmap: true
            asynchronous: true
            cache: true
        }

        Text {
            anchors.centerIn: parent
            visible: flagBox.hasEmoji
            text: App.selectedNodeFlag
            font.pixelSize: 22
            font.family: "Segoe UI Emoji"
            renderType: Text.NativeRendering
        }

        Rectangle {
            anchors.fill: parent
            visible: flagBox.hasSource || flagBox.hasEmoji
            color: "transparent"
            radius: 3
            antialiasing: true
            border.width: 1
            border.color: Qt.rgba(0, 0, 0, 0.2)
        }
    }

    component RowFlag: Item {
        id: rowFlag
        property string source: ""
        property string emoji: ""
        property var colors: []
        property string orient: ""
        readonly property bool imageReady: source.length > 0 && rowImg.status === Image.Ready
        readonly property bool hasStripes: !imageReady && colors && colors.length > 0 && (orient === "h" || orient === "v")
        implicitWidth: 22
        implicitHeight: 16
        clip: true
        Image {
            id: rowImg
            anchors.fill: parent
            visible: rowFlag.imageReady
            source: rowFlag.source
            fillMode: Image.PreserveAspectFit
            sourceSize.width: 66
            sourceSize.height: 48
            asynchronous: true
            cache: true
        }
        Grid {
            anchors.fill: parent
            visible: rowFlag.hasStripes
            rows: rowFlag.orient === "h" ? Math.max(1, rowFlag.colors.length) : 1
            columns: rowFlag.orient === "v" ? Math.max(1, rowFlag.colors.length) : 1
            Repeater {
                model: rowFlag.hasStripes ? rowFlag.colors : []
                delegate: Rectangle {
                    required property string modelData
                    width: rowFlag.orient === "v" ? rowFlag.width / Math.max(1, rowFlag.colors.length) : rowFlag.width
                    height: rowFlag.orient === "h" ? rowFlag.height / Math.max(1, rowFlag.colors.length) : rowFlag.height
                    color: modelData
                }
            }
        }
        Text {
            anchors.centerIn: parent
            visible: !rowFlag.imageReady && !rowFlag.hasStripes && rowFlag.emoji.length > 0
            text: rowFlag.emoji
            font.pixelSize: 15
            font.family: "Segoe UI Emoji"
            renderType: Text.NativeRendering
        }
        Rectangle {
            anchors.fill: parent
            visible: rowFlag.imageReady || rowFlag.hasStripes || rowFlag.emoji.length > 0
            color: "transparent"
            radius: 2
            border.width: 1
            border.color: Qt.rgba(0, 0, 0, 0.2)
        }
    }

    component SectionTitle: Text {
        color: Theme.text
        font.family: Theme.fontFamily
        font.pixelSize: Theme.fontStrong
        font.weight: Font.DemiBold
        textFormat: Text.PlainText
    }

    component BigValue: Row {
        id: bigValue
        property string value: ""
        property string unit: ""
        property int pixelSize: Theme.fontHero
        spacing: 6
        Text {
            id: bigNumber
            text: bigValue.value
            color: Theme.text
            font.family: Theme.fontFamily
            font.pixelSize: bigValue.pixelSize
            font.weight: Font.DemiBold
        }
        Text {
            anchors.baseline: bigNumber.baseline
            text: bigValue.unit
            color: Theme.textMuted
            font.family: Theme.fontFamily
            font.pixelSize: Theme.fontSmall + 1
        }
    }

    component ModeButton: Rectangle {
        id: modeBtn
        property string key: ""
        property string label: ""
        readonly property bool active: page.modeKey() === key
        implicitWidth: modeText.implicitWidth + 24
        implicitHeight: 28
        radius: 4
        color: active ? Theme.accentSoft : (modeHover.hovered ? Theme.cardHover : "transparent")
        Accessible.role: Accessible.Button
        Accessible.name: label
        Text {
            id: modeText
            anchors.centerIn: parent
            text: modeBtn.label
            color: modeBtn.active ? Theme.text : Theme.textMuted
            font.family: Theme.fontFamily
            font.pixelSize: Theme.fontSmall + 1
            font.weight: modeBtn.active ? Font.DemiBold : Font.Normal
        }
        HoverHandler { id: modeHover; cursorShape: Qt.PointingHandCursor }
        TapHandler { enabled: !App.transitionBusy; onTapped: page.setMode(modeBtn.key) }
    }

    ColumnLayout {
        width: page.width
        height: page.twoColumns ? Math.max(page.height, 620) : implicitHeight

        spacing: page.gap

        // ===== Connection hero =====
        Card {
            Layout.fillWidth: true
            padding: 20
            hoverable: false
            RowLayout {
                width: parent.width
                spacing: 24
                ColumnLayout {
                    Layout.fillWidth: true
                    spacing: 12
                    RowLayout {
                        spacing: 14
                        StatusRing {
                            active: App.connected
                            busy: App.transitionBusy
                            targetActive: App.transitionConnecting
                            Layout.preferredWidth: 52
                            Layout.preferredHeight: 52
                        }
                        ColumnLayout {
                            Layout.fillWidth: true
                            spacing: 2
                            Text {
                                text: page.stateTitle() + " · " + page.modeTitle()
                                color: App.connected ? Theme.success : Theme.textMuted
                                font.family: Theme.fontFamily
                                font.pixelSize: Theme.fontSmall
                            }
                            RowLayout {
                                spacing: 10
                                DashboardFlag {
                                    Layout.preferredWidth: visible ? 32 : 0
                                    Layout.preferredHeight: 24
                                }
                                Text {
                                    Layout.fillWidth: true
                                    text: page.connectionServerName()
                                    color: Theme.text
                                    font.family: Theme.fontFamily
                                    font.pixelSize: Theme.fontTitle + 2
                                    font.weight: Font.DemiBold
                                    elide: Text.ElideRight
                                }
                            }
                            Text {
                                Layout.fillWidth: true
                                visible: text.length > 0
                                text: page.serverCaption()
                                textFormat: Text.PlainText
                                elide: Text.ElideRight
                                color: Theme.textMuted
                                font.family: Theme.fontFamily
                                font.pixelSize: Theme.fontSmall
                            }
                        }
                    }
                    RowLayout {
                        spacing: 10
                        AccentButton {
                            kind: App.connected ? "ghost" : "accent"
                            enabled: !App.transitionBusy
                            glyph: App.connected ? "" : ""  // Solid pause / Play
                            text: App.connected ? I18n.t("Отключить") : I18n.t("Подключить")
                            Layout.preferredWidth: 180
                            onClicked: App.toggleConnection()
                        }
                        Rectangle {
                            implicitWidth: modeRow.implicitWidth + 4
                            implicitHeight: 32
                            radius: 6
                            color: Theme.dark ? Qt.rgba(0, 0, 0, 0.18) : Qt.rgba(0, 0, 0, 0.04)
                            border.width: 1
                            border.color: Theme.borderSolid
                            opacity: App.transitionBusy ? 0.6 : 1
                            Row {
                                id: modeRow
                                anchors.centerIn: parent
                                spacing: 2
                                ModeButton { key: "local"; label: I18n.t("Локальный") }
                                ModeButton { key: "system"; label: I18n.t("Сист. прокси") }
                                ModeButton { key: "tun"; label: "VPN (TUN)" }
                            }
                        }
                        AccentButton {
                            kind: "ghost"
                            glyph: ""
                            text: ""
                            iconOnly: true
                            tip: I18n.t("Справка")
                            Layout.preferredWidth: 32
                            onClicked: modeHelp.open()
                        }
                    }
                }
                RowLayout {
                    visible: page.width >= 760
                    spacing: 36
                    Layout.rightMargin: 8
                    ColumnLayout {
                        spacing: 2
                        Text { text: "↓ " + I18n.t("Загрузка"); color: Theme.textMuted; font.family: Theme.fontFamily; font.pixelSize: Theme.fontSmall }
                        BigValue { id: downValue; value: "0"; unit: "B/s"; pixelSize: Theme.fontTitle }
                    }
                    ColumnLayout {
                        spacing: 2
                        Text { text: "↑ " + I18n.t("Выгрузка"); color: Theme.textMuted; font.family: Theme.fontFamily; font.pixelSize: Theme.fontSmall }
                        BigValue { id: upValue; value: "0"; unit: "B/s"; pixelSize: Theme.fontTitle }
                    }
                }
            }
        }

        Dialog {
            id: modeHelp
            anchors.centerIn: Overlay.overlay
            width: 460
            padding: 0
            modal: true
            closePolicy: Popup.CloseOnEscape | Popup.CloseOnPressOutside
            background: Rectangle {
                color: Theme.flyout
                border.width: 1
                border.color: Theme.flyoutBorder
                radius: 10
            }
            Overlay.modal: Rectangle { color: Qt.rgba(0, 0, 0, 0.45) }
            header: Text {
                text: I18n.t("VPN (TUN) или системный прокси")
                color: Theme.text
                font.family: Theme.fontFamily
                font.pixelSize: Theme.fontStrong
                font.weight: Font.DemiBold
                wrapMode: Text.WordWrap
                leftPadding: 20; rightPadding: 20
                topPadding: 18; bottomPadding: 6
            }
            contentItem: ColumnLayout {
                spacing: 10
                Text {
                    text: I18n.t("Системный прокси")
                    color: Theme.text; font.family: Theme.fontFamily
                    font.pixelSize: Theme.fontStrong; font.weight: Font.DemiBold
                    leftPadding: 20; rightPadding: 20
                }
                Text {
                    Layout.fillWidth: true
                    text: I18n.t("Работает с браузерами, мессенджерами и приложениями, которые используют настройки прокси Windows. Обычно запускается быстрее, но чаще не действует в играх и программах без поддержки прокси.")
                    color: Theme.textMuted; font.family: Theme.fontFamily
                    font.pixelSize: Theme.fontNormal; wrapMode: Text.WordWrap
                    lineHeight: 1.25; leftPadding: 20; rightPadding: 20
                }
                Rectangle { Layout.fillWidth: true; Layout.leftMargin: 20; Layout.rightMargin: 20; height: 1; color: Theme.divider }
                Text {
                    text: "VPN (TUN)"
                    color: Theme.text; font.family: Theme.fontFamily
                    font.pixelSize: Theme.fontStrong; font.weight: Font.DemiBold
                    leftPadding: 20; rightPadding: 20
                }
                Text {
                    Layout.fillWidth: true
                    text: I18n.t("Перехватывает трафик почти всех приложений, включая игры, и применяет правила маршрутизации на уровне системы. Требует прав администратора и запускается немного дольше.")
                    color: Theme.textMuted; font.family: Theme.fontFamily
                    font.pixelSize: Theme.fontNormal; wrapMode: Text.WordWrap
                    lineHeight: 1.25; leftPadding: 20; rightPadding: 20
                }
            }
            footer: RowLayout {
                spacing: 0
                Item { Layout.fillWidth: true }
                AccentButton {
                    kind: "accent"
                    text: I18n.t("Понятно")
                    onClicked: modeHelp.close()
                    Layout.rightMargin: 20
                    Layout.topMargin: 6
                    Layout.bottomMargin: 18
                }
            }
        }

        GridLayout {
            Layout.fillWidth: true
            Layout.fillHeight: page.twoColumns
            columns: page.twoColumns ? 2 : 1
            columnSpacing: page.gap
            rowSpacing: page.gap

            // ===== Servers =====
            Card {
                id: serversCard
                Layout.preferredWidth: page.twoColumns ? 420 : -1
                Layout.fillWidth: !page.twoColumns
                Layout.fillHeight: page.twoColumns
                Layout.preferredHeight: page.twoColumns ? -1 : 420
                Layout.minimumHeight: 300
                padding: 12
                hoverable: false
                ColumnLayout {
                    width: parent.width
                    height: parent.height
                    spacing: 10
                    SectionTitle { text: I18n.t("Серверы"); Layout.leftMargin: 4 }
                    // Group tabs: underline marks the active group, like the Fluent pivot.
                    ListView {
                        id: groupTabs
                        objectName: "dashboardGroupTabs"
                        Layout.fillWidth: true
                        Layout.preferredHeight: 34
                        orientation: ListView.Horizontal
                        spacing: 4
                        clip: true
                        boundsBehavior: Flickable.StopAtBounds
                        model: page.groupModel
                        currentIndex: page.groupIndex()
                        onCurrentIndexChanged: positionViewAtIndex(currentIndex, ListView.Contain)
                        // Mouse wheel scrolls the tab strip sideways; without overflow the page scrolls.
                        WheelHandler {
                            acceptedDevices: PointerDevice.Mouse | PointerDevice.TouchPad
                            onWheel: function(event) {
                                var overflow = groupTabs.contentWidth - groupTabs.width;
                                if (overflow <= 0) { event.accepted = false; return; }
                                var delta = event.angleDelta.x !== 0 ? event.angleDelta.x : event.angleDelta.y;
                                var minX = groupTabs.originX;
                                groupTabs.contentX = Math.max(minX, Math.min(minX + overflow, groupTabs.contentX - delta));
                            }
                        }
                        delegate: Item {
                            id: tab
                            required property int index
                            required property string modelData
                            readonly property bool active: index === page.groupIndex()
                            width: tabText.implicitWidth + 20
                            height: groupTabs.height
                            Accessible.role: Accessible.PageTab
                            Accessible.name: tabText.text
                            Text {
                                id: tabText
                                anchors.centerIn: parent
                                anchors.verticalCenterOffset: -2
                                text: tab.index === 0 ? I18n.t("Все") : tab.modelData
                                color: tab.active ? Theme.text : (tabHover.hovered ? Theme.text : Theme.textMuted)
                                font.family: Theme.fontFamily
                                font.pixelSize: Theme.fontNormal
                                font.weight: tab.active ? Font.DemiBold : Font.Normal
                            }
                            Rectangle {
                                anchors.bottom: parent.bottom
                                anchors.bottomMargin: 2
                                anchors.horizontalCenter: parent.horizontalCenter
                                width: tab.active ? Math.max(16, tabText.implicitWidth - 4) : 0
                                height: 3
                                radius: 1.5
                                color: Theme.accent
                                Behavior on width { NumberAnimation { duration: Theme.animFast * 2; easing.type: Easing.OutCubic } }
                            }
                            HoverHandler { id: tabHover; cursorShape: Qt.PointingHandCursor }
                            TapHandler { onTapped: page.selectGroup(tab.index) }
                        }
                    }
                    Rectangle { Layout.fillWidth: true; Layout.topMargin: -10; height: 1; color: Theme.divider }
                    SearchField {
                        id: serverSearch
                        Layout.fillWidth: true
                        placeholderText: I18n.t("Поиск по имени, протоколу, стране")
                        onTextChanged: { page.clearSelection(); App.setDashboardNodeFilter(text); }
                    }
                    ListView {
                        id: serverList
                        objectName: "dashboardServerList"
                        Layout.fillWidth: true
                        Layout.fillHeight: true
                        clip: true
                        spacing: 2
                        model: App.dashboardNodeModel
                        boundsBehavior: Flickable.StopAtBounds
                        ScrollBar.vertical: FluentScrollBar { }
                        header: Item {
                            width: ListView.view ? ListView.view.width : 0
                            height: quickRow.height + (page.quickHint.length > 0 ? quickHintText.implicitHeight + 6 : 0) + 2
                            Rectangle {
                                id: quickRow
                                width: parent.width - 10
                                height: 44
                                radius: Theme.radiusSmall
                                color: quickHover.hovered ? Theme.cardHover : "transparent"
                                Text {
                                    x: 10
                                    width: 22
                                    anchors.verticalCenter: parent.verticalCenter
                                    horizontalAlignment: Text.AlignHCenter
                                    text: ""
                                    color: Theme.accent
                                    font.family: "Segoe Fluent Icons"
                                    font.pixelSize: 16
                                }
                                Column {
                                    x: 44
                                    width: parent.width - 54
                                    anchors.verticalCenter: parent.verticalCenter
                                    Text { text: I18n.t("Быстрое подключение"); color: Theme.text; font.family: Theme.fontFamily; font.pixelSize: Theme.fontNormal }
                                    Text { text: I18n.t("Лучший пинг в группе"); color: Theme.textMuted; font.family: Theme.fontFamily; font.pixelSize: Theme.fontSmall }
                                }
                                HoverHandler { id: quickHover; cursorShape: Qt.PointingHandCursor }
                                TapHandler { enabled: !App.transitionBusy; onTapped: page.quickConnect() }
                            }
                            Text {
                                id: quickHintText
                                anchors.top: quickRow.bottom
                                anchors.topMargin: 4
                                width: parent.width - 20
                                x: 10
                                visible: page.quickHint.length > 0
                                text: page.quickHint
                                color: Theme.warning
                                wrapMode: Text.WordWrap
                                font.family: Theme.fontFamily
                                font.pixelSize: Theme.fontSmall
                            }
                        }
                        delegate: Item {
                            id: serverRow
                            required property string nodeId
                            required property string name
                            required property string scheme
                            required property string protocol
                            required property string group
                            required property string country
                            required property int ping
                            required property bool selected
                            required property bool runtimeSupported
                            required property string flagSource
                            required property string flagEmoji
                            required property var flagColors
                            required property string flagOrient
                            readonly property bool picked: !!page.selection[nodeId]
                            width: ListView.view ? ListView.view.width : 0
                            height: 44
                            Rectangle {
                                anchors.fill: parent
                                anchors.rightMargin: 10
                                radius: Theme.radiusSmall
                                color: serverRow.selected ? Theme.accentSoft
                                     : serverRow.picked ? Qt.rgba(Theme.accent.r, Theme.accent.g, Theme.accent.b, 0.12)
                                     : (rowHover.hovered || (ctxMenu.opened && page.menuNodeId === serverRow.nodeId) ? Theme.cardHover : "transparent")
                                opacity: serverRow.runtimeSupported ? 1 : 0.55
                                Rectangle {
                                    visible: serverRow.selected
                                    x: 0
                                    anchors.verticalCenter: parent.verticalCenter
                                    width: 3
                                    height: 16
                                    radius: 2
                                    color: Theme.accent
                                }
                                RowLayout {
                                    anchors.fill: parent
                                    anchors.leftMargin: 10
                                    anchors.rightMargin: 10
                                    spacing: 12
                                    RowFlag {
                                        source: serverRow.flagSource
                                        emoji: serverRow.flagEmoji
                                        colors: serverRow.flagColors
                                        orient: serverRow.flagOrient
                                        Layout.preferredWidth: 22
                                        Layout.preferredHeight: 16
                                    }
                                    ColumnLayout {
                                        Layout.fillWidth: true
                                        spacing: 0
                                        Text {
                                            Layout.fillWidth: true
                                            text: serverRow.name
                                            textFormat: Text.PlainText
                                            elide: Text.ElideRight
                                            color: Theme.text
                                            font.family: Theme.fontFamily
                                            font.pixelSize: Theme.fontNormal
                                        }
                                        Text {
                                            Layout.fillWidth: true
                                            text: [serverRow.protocol, serverRow.group].filter(function(v) { return v && v.length > 0; }).join(" · ")
                                            textFormat: Text.PlainText
                                            elide: Text.ElideRight
                                            color: Theme.textMuted
                                            font.family: Theme.fontFamily
                                            font.pixelSize: Theme.fontSmall
                                        }
                                    }
                                    Text {
                                        text: serverRow.ping < 0 ? "—" : serverRow.ping + " ms"
                                        color: Theme.pingColor(serverRow.ping)
                                        font.family: Theme.fontFamily
                                        font.pixelSize: Theme.fontSmall
                                    }
                                }
                            }
                            HoverHandler { id: rowHover; cursorShape: Qt.PointingHandCursor }
                            TapHandler {
                                acceptedModifiers: Qt.NoModifier
                                onTapped: { page.clearSelection(); serverList.forceActiveFocus(); App.selectNode(serverRow.nodeId); }
                            }
                            TapHandler {
                                acceptedModifiers: Qt.ControlModifier
                                onTapped: { serverList.forceActiveFocus(); page.toggleSelected(serverRow.nodeId); }
                            }
                            TapHandler {
                                acceptedButtons: Qt.RightButton
                                onTapped: {
                                    page.menuNodeId = serverRow.nodeId;
                                    ctxMenu.popup();
                                }
                            }
                        }
                        Text {
                            anchors.centerIn: parent
                            visible: serverList.count === 0
                            text: I18n.t("Серверов нет — импортируйте ссылку или подписку")
                            color: Theme.textFaint
                            font.family: Theme.fontFamily
                            font.pixelSize: Theme.fontSmall
                        }
                    }
                }
            }

            // ===== Right column =====
            ColumnLayout {
                Layout.fillWidth: true
                Layout.fillHeight: page.twoColumns
                spacing: page.gap

                GridLayout {
                    Layout.fillWidth: true
                    columns: page.width >= 720 ? 2 : 1
                    columnSpacing: page.gap
                    rowSpacing: page.gap

                    Card {
                        Layout.fillWidth: true
                        Layout.preferredWidth: 1
                        padding: 12
                        hoverable: false
                        ColumnLayout {
                            width: parent.width
                            spacing: 4
                            SectionTitle { text: I18n.t("Задержка") }
                            BigValue {
                                value: App.connected && App.latencyMs >= 0 ? String(App.latencyMs) : "--"
                                unit: "ms"
                                pixelSize: Theme.fontTitle
                            }
                            Item {
                                Layout.fillWidth: true
                                Layout.preferredHeight: 18
                                Row {
                                    anchors.fill: parent
                                    spacing: 4
                                    Repeater {
                                        model: 16
                                        delegate: Item {
                                            required property int index
                                            readonly property var hist: page.rttHistory
                                            readonly property int slot: hist.length - 16 + index
                                            readonly property real valueMs: slot >= 0 ? hist[slot] : -1
                                            readonly property real maxMs: Math.max.apply(null, hist.concat([1]))
                                            width: (parent.width - 15 * 4) / 16
                                            height: parent.height
                                            Rectangle {
                                                anchors.bottom: parent.bottom
                                                width: parent.width
                                                radius: 2
                                                height: parent.valueMs > 0 ? Math.max(4, parent.height * (0.25 + 0.75 * parent.valueMs / parent.maxMs)) : 4
                                                color: parent.valueMs > 0 ? Theme.accent : Theme.divider
                                                opacity: parent.valueMs > 0 ? (parent.index === 15 ? 1 : 0.45) : 1
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card {
                        Layout.fillWidth: true
                        Layout.preferredWidth: 1
                        padding: 12
                        hoverable: false
                        ColumnLayout {
                            width: parent.width
                            spacing: 4
                            SectionTitle { text: I18n.t("Сессия") }
                            BigValue {
                                readonly property string total: page.fmtBytes(App.sessionTrafficTotal)
                                value: page.valuePart(total)
                                unit: page.unitPart(total)
                                pixelSize: Theme.fontTitle
                            }
                            Text {
                                text: "↓ " + page.fmtBytes(App.sessionDownloadTotal) + " · ↑ " + page.fmtBytes(App.sessionUploadTotal)
                                    + "  ·  " + I18n.t("Пик: ") + page.fmtSpeed(page.sessionPeak)
                                color: Theme.textMuted
                                font.family: Theme.fontFamily
                                font.pixelSize: Theme.fontSmall
                            }
                        }
                    }
                }

                // ===== App traffic =====
                Card {
                    id: procCard
                    Layout.fillWidth: true
                    Layout.fillHeight: page.twoColumns
                    Layout.minimumHeight: 150
                    padding: 16
                    hoverable: false
                    ColumnLayout {
                        width: parent.width
                        height: parent.height
                        spacing: 10
                        SectionTitle { text: I18n.t("Трафик приложений") }
                        ListView {
                            id: procRepeater
                            objectName: "dashboardProcessTraffic"
                            Layout.fillWidth: true
                            Layout.fillHeight: page.twoColumns
                            Layout.preferredHeight: page.twoColumns ? -1 : Math.min(count, 6) * 30
                            model: App.processModel
                            clip: true
                            spacing: 4
                            boundsBehavior: Flickable.StopAtBounds
                            ScrollBar.vertical: FluentScrollBar { }
                            delegate: Item {
                                id: processRow
                                required property int index
                                required property string name
                                required property real total
                                required property real proxyBytes
                                required property real directBytes
                                readonly property string displayName: page.processLabel(name)
                                width: ListView.view ? ListView.view.width : 0
                                height: 26
                                onTotalChanged: if (total > page.appMax) page.appMax = total
                                Component.onCompleted: if (total > page.appMax) page.appMax = total
                                RowLayout {
                                    anchors.fill: parent
                                    anchors.rightMargin: 8
                                    spacing: 14
                                    Text {
                                        Layout.preferredWidth: 150
                                        text: processRow.displayName
                                        textFormat: Text.PlainText
                                        elide: Text.ElideRight
                                        color: Theme.text
                                        font.family: Theme.fontFamily
                                        font.pixelSize: Theme.fontSmall + 1
                                    }
                                    Rectangle {
                                        Layout.fillWidth: true
                                        Layout.preferredHeight: 6
                                        radius: 3
                                        color: Theme.dark ? Qt.rgba(0, 0, 0, 0.22) : Qt.rgba(0, 0, 0, 0.06)
                                        Rectangle {
                                            height: parent.height
                                            radius: 3
                                            width: parent.width * Math.min(1, processRow.total / Math.max(1, page.appMax))
                                            color: Theme.accent
                                            Behavior on width { NumberAnimation { duration: Theme.animFast } }
                                        }
                                    }
                                    Text {
                                        Layout.preferredWidth: 80
                                        horizontalAlignment: Text.AlignRight
                                        text: page.fmtBytes(processRow.total)
                                        color: Theme.textMuted
                                        font.family: Theme.fontFamily
                                        font.pixelSize: Theme.fontSmall + 1
                                    }
                                }
                            }
                        }
                        Text {
                            visible: procRepeater.count === 0
                            Layout.fillWidth: true
                            text: I18n.t("Статистика появится, когда приложения начнут передавать данные")
                            textFormat: Text.PlainText
                            color: Theme.textFaint
                            wrapMode: Text.WordWrap
                            font.family: Theme.fontFamily
                            font.pixelSize: Theme.fontSmall
                        }
                        Item { visible: procRepeater.count === 0 && page.twoColumns; Layout.fillHeight: true }
                    }
                }

                // ===== Routing =====
                Card {
                    Layout.fillWidth: true
                    padding: 14
                    hoverable: false
                    RowLayout {
                        width: parent.width
                        spacing: 14
                        SectionTitle { text: I18n.t("Маршрутизация"); Layout.fillWidth: true }
                        FluentCombo {
                            Layout.preferredWidth: 260
                            textRole: "name"
                            model: App.routingPresetOptions
                            currentIndex: page.routingPresetIndex()
                            onActivated: {
                                var item = App.routingPresetOptions[currentIndex];
                                if (item) App.applyRoutingPresetOption(item.id);
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- server context menu (same actions as Серверы, minus speed test) ----
    FluentMenu {
        id: ctxMenu
        objectName: "dashboardServerMenu"
        width: 270

        component Ctx: FluentMenuItem {}
        component Sep: FluentMenuSeparator {}

        Ctx {
            text: I18n.t("Установить как активный")
            enabled: !page.menuMulti
            onTriggered: App.selectNode(page.menuNodeId)
        }
        Sep {}
        Ctx {
            text: I18n.t("Редактировать")
            enabled: !page.menuMulti
            onTriggered: editDialog.openFor(page.menuNodeId)
        }
        Ctx {
            text: I18n.t("Копировать ссылку")
            enabled: !page.menuMulti
            onTriggered: App.copyNodeLink(page.menuNodeId)
        }
        Ctx {
            text: I18n.t("Экспорт в QR-код")
            enabled: !page.menuMulti
            onTriggered: App.showNodeQr(page.menuNodeId)
        }
        Sep {}
        Ctx {
            text: page.countText("Пинг — способ из настроек", "Пинг — способ из настроек ({count})")
            onTriggered: App.pingNodes(page.menuTargets())
        }
        Ctx {
            text: page.countText("Тест TCP ping", "Тест TCP ping ({count})")
            onTriggered: App.tcpingNodes(page.menuTargets())
        }
        Ctx {
            text: page.countText("Тест HTTP GET", "Тест HTTP GET ({count})")
            onTriggered: App.httpGetNodes(page.menuTargets())
        }
        Ctx {
            text: page.countText("Тест реальной задержки", "Тест реальной задержки ({count})")
            onTriggered: App.realDelayNodes(page.menuTargets())
        }
        Sep {}
        Ctx {
            text: I18n.t("В начало списка")
            enabled: !page.menuMulti
            onTriggered: App.reorderNode(page.menuNodeId, "top")
        }
        Ctx {
            text: I18n.t("В конец списка")
            enabled: !page.menuMulti
            onTriggered: App.reorderNode(page.menuNodeId, "bottom")
        }
        Sep {}
        Ctx {
            text: page.countText("Удалить", "Удалить ({count})")
            onTriggered: App.deleteNodes(page.menuTargets())
        }
    }

    NodeEditDialog { id: editDialog }

    readonly property bool _kbReady: page.visible && page.enabled && !serverSearch.input.activeFocus
        && !editDialog.opened && !ctxMenu.opened && !modeHelp.opened
    Shortcut {
        sequences: [ StandardKey.SelectAll ]
        enabled: page._kbReady && serverList.count > 0
        autoRepeat: false
        onActivated: page.selectAllServers()
    }
    Shortcut {
        sequences: [ StandardKey.Cancel ]
        enabled: page._kbReady && page.selCount > 0
        autoRepeat: false
        onActivated: page.clearSelection()
    }

    Component.onCompleted: App.setProcessTrafficSort("total", false)

    // ---- live metric updates -----------------------------------------
    Connections {
        target: App
        // Any left click in the window drops the multi-selection, except
        // Ctrl+click (toggles rows) and clicks inside the open context menu.
        function onPointerPressed(button, modifiers) {
            if (page.selCount === 0 || ctxMenu.opened || button !== Qt.LeftButton)
                return;
            if (modifiers & Qt.ControlModifier)
                return;
            page.clearSelection();
        }
        function onMetricsChanged() {
            var down = App.trafficAvailable ? page.fmtSpeed(App.downBps) : "—";
            var up = App.trafficAvailable ? page.fmtSpeed(App.upBps) : "—";
            downValue.value = page.valuePart(down); downValue.unit = page.unitPart(down);
            upValue.value = page.valuePart(up); upValue.unit = page.unitPart(up);
            if (App.trafficAvailable)
                page.sessionPeak = Math.max(page.sessionPeak, App.downBps, App.upBps);
            if (App.connected && App.latencyMs > 0) {
                var hist = page.rttHistory.slice(-15);
                hist.push(App.latencyMs);
                page.rttHistory = hist;
            }
        }
        function onConnectedChanged() {
            if (!App.connected) {
                downValue.value = "0"; downValue.unit = "B/s";
                upValue.value = "0"; upValue.unit = "B/s";
                page.sessionPeak = 0;
                page.rttHistory = [];
                page.appMax = 1;
            }
        }
    }
}
