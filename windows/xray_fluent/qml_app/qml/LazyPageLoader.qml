import QtQuick

Item {
    id: root

    property bool current: false
    property bool loaded: false
    property bool loadAsynchronously: true
    property bool preloadRequested: false
    property Component pageComponent
    property int slide: 6
    readonly property bool ready: pageLoader.status === Loader.Ready

    readonly property bool laidOut: current || opacity > 0

    x: 0
    y: 0
    // Warm pages at their real viewport size (a zero-size preload leaves
    // virtual lists unlaid-out when the page is opened right away), but only
    // the shown page follows the window: hidden pages keep their last size,
    // so a window resize or the rail animation relays out one page, not all.
    Component.onCompleted: {
        if (current || preloadRequested)
            loaded = true
    }
    // Until its page exists the loader is empty and following the window is
    // free; that way a background load still happens at the real size.
    readonly property bool tracksWindow: laidOut || pageLoader.status !== Loader.Ready
    Binding on width {
        when: root.tracksWindow && root.parent !== null
        value: root.parent ? root.parent.width : 0
        restoreMode: Binding.RestoreNone
    }
    Binding on height {
        when: root.tracksWindow && root.parent !== null
        value: root.parent ? root.parent.height : 0
        restoreMode: Binding.RestoreNone
    }
    visible: laidOut
    opacity: current ? 1 : 0

    onCurrentChanged: {
        if (current)
            loaded = true
    }
    onPreloadRequestedChanged: {
        if (preloadRequested)
            loaded = true
    }
    Behavior on opacity {
        NumberAnimation {
            duration: Theme.animations ? 220 : 0
            easing.type: Theme.easeStandard
        }
    }

    transform: Translate {
        y: root.current ? 0 : root.slide
        Behavior on y {
            NumberAnimation {
                duration: Theme.animations ? 260 : 0
                easing.type: Theme.easeEmphasized
            }
        }
    }

    Text {
        anchors.centerIn: parent
        width: Math.max(0, Math.min(parent.width - 24, 380))
        visible: root.current && !root.ready
        text: pageLoader.status === Loader.Error ? I18n.t("Не удалось открыть раздел") : I18n.t("Загрузка раздела…")
        textFormat: Text.PlainText
        horizontalAlignment: Text.AlignHCenter
        wrapMode: Text.WordWrap
        color: Theme.textMuted
        font.family: Theme.fontFamily
        font.pixelSize: Theme.fontNormal
    }

    Loader {
        id: pageLoader
        anchors.fill: parent
        active: root.loaded
        // Background pages remain lazy/asynchronous. Opening a page promotes
        // that one load to completion instead of exposing an empty surface.
        asynchronous: root.loadAsynchronously && !root.current
        sourceComponent: root.pageComponent
    }
}
