package com.unciv.ui.screens.devconsole

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.KeyCharAndCode
import com.unciv.ui.components.input.keyShortcuts
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.worldscreen.WorldScreen


class DevConsolePopup(val screen: WorldScreen) : Popup(screen) {
    companion object {
        val history = ArrayList<String>()
    }
    private var currentHistoryEntry = history.size

    private val textField = TextField("", BaseScreen.skin)
    private val responseLabel = "".toLabel(Color.RED)

    private val commandRoot = ConsoleCommandRoot()
    internal val gameInfo = screen.gameInfo

    init {
        add(textField).width(stageToShowOn.width / 2).row()
        textField.keyShortcuts.add(Input.Keys.ENTER, ::onEnter)

        // Without this, console popup will always contain a `
        textField.addAction(Actions.delay(0.05f, Actions.run { textField.text = "" }))

        add(responseLabel)

        open(true)

        keyShortcuts.add(KeyCharAndCode.ESC) { close() }

        keyShortcuts.add(KeyCharAndCode.TAB) {
            val textToAdd = getAutocomplete()
            textField.appendText(textToAdd)
        }

        if (history.isNotEmpty()) {
            keyShortcuts.add(Input.Keys.UP) {
                if (currentHistoryEntry > 0) currentHistoryEntry--
                textField.text = history[currentHistoryEntry]
                textField.cursorPosition = textField.text.length
            }
            keyShortcuts.add(Input.Keys.DOWN) {
                if (currentHistoryEntry == history.size) currentHistoryEntry--
                if (currentHistoryEntry < history.lastIndex) currentHistoryEntry++
                textField.text = history[currentHistoryEntry]
                textField.cursorPosition = textField.text.length
            }
        }

        screen.stage.keyboardFocus = textField
    }

    private fun onEnter() {
        val handleCommandResponse = handleCommand()
        if (handleCommandResponse.isOK) {
            screen.shouldUpdate = true
            history.add(textField.text)
            close()
            return
        }
        responseLabel.setText(handleCommandResponse.message)
        responseLabel.style.fontColor = handleCommandResponse.color
    }

    private fun getParams(text: String) = text.split(" ").filter { it.isNotEmpty() }.map { it.lowercase() }

    private fun handleCommand(): DevConsoleResponse {
        val params = getParams(textField.text)
        return commandRoot.handle(this, params)
    }

    private fun getAutocomplete(): String {
        val params = getParams(textField.text)
        return commandRoot.autocomplete(params) ?: ""
    }

    internal fun getCivByName(name: String) = gameInfo.civilizations.firstOrNull { it.civName.toCliInput() == name }

    internal fun getSelectedUnit(): MapUnit? {
        val selectedTile = screen.mapHolder.selectedTile ?: return null
        if (selectedTile.getFirstUnit() == null) return null
        val units = selectedTile.getUnits().toList()
        val selectedUnit = screen.bottomUnitTable.selectedUnit
        return if (selectedUnit != null && selectedUnit.getTile() == selectedTile) selectedUnit
        else units.first()
    }

}
