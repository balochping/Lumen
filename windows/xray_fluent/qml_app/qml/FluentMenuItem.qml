import QtQuick
import QtQuick.Controls.Universal
import "."

MenuItem {
    id: control
    implicitHeight: 32
    leftPadding: 12
    rightPadding: control.subMenu ? 28 : 12

    // Fluent icon glyph; a submenu takes it from its FluentMenu.
    property string glyph: control.subMenu && control.subMenu.glyph !== undefined ? control.subMenu.glyph : ""
    // Keyboard shortcut hint shown on the right, e.g. "Ctrl+V".
    property string hint: ""
    // Destructive action (delete): red text and icon.
    property bool danger: false
    readonly property bool iconColumn: glyph.length > 0 || (control.menu && control.menu.showIcons === true)
    onClicked: {
        if (control.subMenu && control.subMenu.clickable === true) {
            control.subMenu.activated();
            if (control.menu)
                control.menu.dismiss();
        }
    }
    readonly property color ink: !control.enabled ? Theme.textFaint : (danger ? Theme.danger : Theme.text)

    arrow: Text {
        visible: !!control.subMenu
        x: control.width - width - 10
        y: (control.height - height) / 2
        text: ""
        color: control.enabled ? Theme.textMuted : Theme.textFaint
        font.family: "Segoe Fluent Icons"
        font.pixelSize: 10
    }

    contentItem: Item {
        implicitWidth: (control.iconColumn ? 28 : 0) + label.implicitWidth + (hintText.visible ? hintText.implicitWidth + 24 : 0)
        implicitHeight: label.implicitHeight
        Text {
            id: icon
            visible: control.iconColumn
            width: 16
            anchors.verticalCenter: parent.verticalCenter
            horizontalAlignment: Text.AlignHCenter
            text: control.glyph
            color: control.enabled ? (control.danger ? Theme.danger : Theme.textMuted) : Theme.textFaint
            font.family: "Segoe Fluent Icons"
            font.pixelSize: 14
        }
        Text {
            id: label
            anchors.left: parent.left
            anchors.leftMargin: control.iconColumn ? 28 : 0
            anchors.right: hintText.visible ? hintText.left : parent.right
            anchors.rightMargin: hintText.visible ? 16 : 0
            anchors.verticalCenter: parent.verticalCenter
            text: control.text
            color: control.ink
            font.family: Theme.fontFamily
            font.pixelSize: Theme.fontNormal
            verticalAlignment: Text.AlignVCenter
            elide: Text.ElideRight
        }
        Text {
            id: hintText
            visible: control.hint.length > 0
            anchors.right: parent.right
            anchors.verticalCenter: parent.verticalCenter
            text: control.hint
            color: Theme.textFaint
            font.family: Theme.fontFamily
            font.pixelSize: Theme.fontSmall
        }
    }

    background: Rectangle {
        radius: 6
        color: control.highlighted ? Theme.cardHover : "transparent"
        Behavior on color { ColorAnimation { duration: Theme.animations ? 90 : 0 } }
    }
}
