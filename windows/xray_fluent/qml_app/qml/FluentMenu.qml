import QtQuick
import QtQuick.Controls.Universal
import "."

Menu {
    id: control
    padding: 5
    margins: 8
    // Glyph shown on the parent item when this menu is used as a submenu.
    property string glyph: ""
    // Reserve the icon column so labels stay aligned when only some items have icons.
    property bool showIcons: false
    // A submenu can also act on a click on its own row (hover still opens it).
    property bool clickable: false
    signal activated()

    // Submenus get the same styled rows as regular items.
    delegate: FluentMenuItem {}

    background: Rectangle {
        color: Theme.flyout
        radius: 8
        border.width: 1
        border.color: Theme.flyoutBorder
    }

    enter: Transition {
        NumberAnimation { property: "opacity"; from: 0.0; to: 1.0; duration: Theme.animations ? 90 : 0 }
    }
    exit: Transition {
        NumberAnimation { property: "opacity"; from: 1.0; to: 0.0; duration: Theme.animations ? 70 : 0 }
    }
}
