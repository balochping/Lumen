import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import App 1.0
import "."

// «Переместить» — moves the selected servers into an existing group (picked
// from a list) or into a new one, via App.bulkEditNodes.
Popup {
    id: dlg
    modal: true
    dim: true
    focus: true
    padding: 0
    closePolicy: Popup.CloseOnEscape

    parent: Overlay.overlay
    anchors.centerIn: Overlay.overlay
    width: 440

    property var ids: []
    property int count: 0

    // Existing groups (manual groups, node groups and subscription groups).
    readonly property var groups: {
        var list = (App.groupOptions || []).slice();
        var subs = App.subscriptions || [];
        for (var i = 0; i < subs.length; ++i) {
            var g = String(subs[i].group || subs[i].name || "Default");
            if (list.indexOf(g) < 0) list.push(g);
        }
        return list;
    }
    readonly property var choices: groups.concat([I18n.t("Новая группа…")])
    readonly property bool creating: groupCombo.currentIndex === choices.length - 1
    readonly property string target: creating ? newGroupField.text.trim() : (groups[groupCombo.currentIndex] || "")

    background: Rectangle {
        color: Theme.flyout
        radius: 10
        border.width: 1
        border.color: Theme.flyoutBorder
    }

    Overlay.modal: Rectangle { color: Qt.rgba(0, 0, 0, 0.4) }

    function openFor(nodeIds) {
        ids = nodeIds || []
        count = ids.length
        newGroupField.text = ""
        groupCombo.currentIndex = 0
        open()
    }

    function _apply() {
        if (!dlg.target.length)
            return
        App.bulkEditNodes(ids, {
            "group": dlg.target
        })
        close()
    }

    component FLabel: Text {
        Layout.fillWidth: true
        color: Theme.textMuted
        font.family: Theme.fontFamily
        font.pixelSize: Theme.fontSmall
        wrapMode: Text.WordWrap
    }

    contentItem: ColumnLayout {
        spacing: 10

        Text {
            Layout.fillWidth: true
            Layout.topMargin: 20
            Layout.leftMargin: 20
            Layout.rightMargin: 20
            text: I18n.t("Переместить")
            color: Theme.text
            font.family: Theme.fontFamily
            font.pixelSize: Theme.fontTitle
            font.bold: true
        }

        ColumnLayout {
            Layout.fillWidth: true
            Layout.leftMargin: 20
            Layout.rightMargin: 20
            spacing: 6

            Text {
                Layout.fillWidth: true
                text: I18n.t("Серверов: {count}", { count: dlg.count })
                color: Theme.textMuted
                font.family: Theme.fontFamily
                font.pixelSize: Theme.fontNormal
            }

            FLabel { text: I18n.t("Группа") ; Layout.topMargin: 4 }
            FluentCombo {
                id: groupCombo
                Layout.fillWidth: true
                model: dlg.choices
            }

            FLabel { visible: dlg.creating; text: I18n.t("Имя группы") ; Layout.topMargin: 4 }
            FluentTextField {
                id: newGroupField
                visible: dlg.creating
                Layout.fillWidth: true
                implicitHeight: Theme.controlHeight
                leftPadding: 10
                rightPadding: 10
                topPadding: 0
                bottomPadding: 0
                placeholderText: I18n.t("Например: Мои серверы")
                color: Theme.text
                font.family: Theme.fontFamily
                font.pixelSize: Theme.fontNormal
                selectByMouse: true
                verticalAlignment: TextInput.AlignVCenter
                onAccepted: dlg._apply()
                background: Rectangle {
                    radius: Theme.radiusSmall
                    color: Theme.controlFill
                    border.width: 1
                    border.color: parent.activeFocus ? Theme.accent : Theme.borderSolid
                }
            }
        }

        RowLayout {
            Layout.fillWidth: true
            Layout.margins: 20
            Layout.topMargin: 6
            spacing: 10
            Item { Layout.fillWidth: true }
            AccentButton {
                kind: "ghost"
                text: I18n.t("Отмена")
                onClicked: dlg.close()
            }
            AccentButton {
                kind: "accent"
                text: I18n.t("Переместить")
                enabled: dlg.target.length > 0
                onClicked: dlg._apply()
            }
        }
    }
}
