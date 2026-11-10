package com.urunkarpm.drawer.feature.home.action

import java.util.regex.Pattern

sealed interface VoiceAction {
    data class AddAppToDrawer(val app: String, val drawer: String) : VoiceAction
    data class RemoveAppFromDrawer(val app: String, val drawer: String) : VoiceAction
    data class MoveAppBetweenDrawers(val app: String, val sourceDrawer: String, val targetDrawer: String) : VoiceAction
    data class AddAppToDock(val app: String) : VoiceAction
    data class RemoveAppFromDock(val app: String) : VoiceAction
    data class CreateDrawer(val name: String) : VoiceAction
    data class DeleteDrawer(val name: String) : VoiceAction
    data class RenameDrawer(val oldName: String, val newName: String) : VoiceAction
}

// ponytail: direct regex voice action parser resolving drawer/dock organization commands; ceiling: English action verbs; upgrade path: ML Kit text entity extraction.
object VoiceActionParser {

    private val addDrawerPattern = Pattern.compile("(?i)^(?:add|put|place|move)\\s+(.+?)\\s+(?:to|in|into|on)\\s+(?:the\\s+)?(?:drawer\\s+|category\\s+|group\\s+)?(.+?)(?:\\s+drawer|\\s+category|\\s+group)?$")
    private val pinDockPattern = Pattern.compile("(?i)^pin\\s+(.+?)\\s+(?:to|in|into|on)\\s+(?:the\\s+)?dock$")
    private val removeDockPattern = Pattern.compile("(?i)^(?:unpin|remove)\\s+(.+?)\\s+(?:from|off)\\s+(?:the\\s+)?dock$")
    private val removeDrawerPattern = Pattern.compile("(?i)^remove\\s+(.+?)\\s+(?:from|out of)\\s+(?:the\\s+)?(.+?)(?:\\s+drawer|\\s+category)?$")
    private val moveBetweenPattern = Pattern.compile("(?i)^(?:move|transfer)\\s+(.+?)\\s+(?:from)\\s+(.+?)\\s+(?:to|into)\\s+(.+?)$")
    private val createDrawerPattern = Pattern.compile("(?i)^(?:create|add|new|make)\\s+(?:a\\s+)?(?:new\\s+)?(?:drawer|category|group)\\s+(?:named|called\\s+)?(.+?)$")
    private val deleteDrawerPattern = Pattern.compile("(?i)^(?:delete|remove)\\s+(?:the\\s+)?(?:drawer|category|group)\\s+(.+?)$")
    private val renameDrawerPattern = Pattern.compile("(?i)^rename\\s+(?:the\\s+)?(?:drawer|category|group\\s+)?(.+?)\\s+to\\s+(.+?)$")

    fun parse(query: String): VoiceAction? {
        val clean = query.trim()
            .trim('"', '\'', '`', '“', '”')
            .trimEnd('.', '?', '!', ',', ';')
            .trim()
        if (clean.isBlank()) return null

        val pinDockMatch = pinDockPattern.matcher(clean)
        if (pinDockMatch.find()) {
            val app = cleanTarget(pinDockMatch.group(1))
            if (app.isNotBlank()) return VoiceAction.AddAppToDock(app)
        }

        val removeDockMatch = removeDockPattern.matcher(clean)
        if (removeDockMatch.find()) {
            val app = cleanTarget(removeDockMatch.group(1))
            if (app.isNotBlank()) return VoiceAction.RemoveAppFromDock(app)
        }

        val moveBetweenMatch = moveBetweenPattern.matcher(clean)
        if (moveBetweenMatch.find()) {
            val app = cleanTarget(moveBetweenMatch.group(1))
            val src = cleanTarget(moveBetweenMatch.group(2))
            val dst = cleanTarget(moveBetweenMatch.group(3))
            if (app.isNotBlank() && src.isNotBlank() && dst.isNotBlank()) {
                return VoiceAction.MoveAppBetweenDrawers(app, src, dst)
            }
        }

        val removeDrawerMatch = removeDrawerPattern.matcher(clean)
        if (removeDrawerMatch.find()) {
            val app = cleanTarget(removeDrawerMatch.group(1))
            val drawer = cleanTarget(removeDrawerMatch.group(2))
            if (app.isNotBlank() && drawer.isNotBlank()) {
                if (drawer.equals("dock", ignoreCase = true)) return VoiceAction.RemoveAppFromDock(app)
                return VoiceAction.RemoveAppFromDrawer(app, drawer)
            }
        }

        val createDrawerMatch = createDrawerPattern.matcher(clean)
        if (createDrawerMatch.find()) {
            val name = cleanTarget(createDrawerMatch.group(1))
            if (name.isNotBlank()) return VoiceAction.CreateDrawer(name)
        }

        val deleteDrawerMatch = deleteDrawerPattern.matcher(clean)
        if (deleteDrawerMatch.find()) {
            val name = cleanTarget(deleteDrawerMatch.group(1))
            if (name.isNotBlank()) return VoiceAction.DeleteDrawer(name)
        }

        val renameDrawerMatch = renameDrawerPattern.matcher(clean)
        if (renameDrawerMatch.find()) {
            val oldName = cleanTarget(renameDrawerMatch.group(1))
            val newName = cleanTarget(renameDrawerMatch.group(2))
            if (oldName.isNotBlank() && newName.isNotBlank()) {
                return VoiceAction.RenameDrawer(oldName, newName)
            }
        }

        val addDrawerMatch = addDrawerPattern.matcher(clean)
        if (addDrawerMatch.find()) {
            val app = cleanTarget(addDrawerMatch.group(1))
            val drawer = cleanTarget(addDrawerMatch.group(2))
            if (app.isNotBlank() && drawer.isNotBlank()) {
                if (drawer.equals("dock", ignoreCase = true)) return VoiceAction.AddAppToDock(app)
                return VoiceAction.AddAppToDrawer(app, drawer)
            }
        }

        return null
    }

    private fun cleanTarget(text: String?): String {
        if (text == null) return ""
        return text.trim()
            .replace(Regex("(?i)^(?:the\\s+app\\s+|app\\s+|the\\s+)"), "")
            .trim('"', '\'', '`', '“', '”')
            .trim()
    }
}
