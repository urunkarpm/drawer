package com.urunkarpm.drawer.core.data.ai

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

enum class TargetCategory(
    val id: String,
    val displayName: String,
    val groupAliases: List<String>
) {
    FINANCE(
        id = "finance",
        displayName = "Finance",
        groupAliases = listOf(
            "finance", "money", "bank", "banking", "wallet",
            "crypto", "invest", "investment", "pay", "payment", "taxes"
        )
    ),
    SOCIAL(
        id = "social",
        displayName = "Social",
        groupAliases = listOf(
            "social", "chat", "messaging", "messages", "messenger",
            "friends", "communication", "connect", "dialer", "contacts"
        )
    ),
    PRODUCTIVITY(
        id = "productivity",
        displayName = "Productivity",
        groupAliases = listOf(
            "work", "productivity", "deep work", "office", "projects",
            "study", "notes", "tasks", "focus", "documents", "docs"
        )
    ),
    MEDIA(
        id = "media",
        displayName = "Media",
        groupAliases = listOf(
            "media", "entertainment", "music", "video", "streaming",
            "photos", "movies", "gallery", "audio", "podcasts"
        )
    ),
    UTILITIES(
        id = "utilities",
        displayName = "Utilities",
        groupAliases = listOf(
            "tool", "tools", "utility", "utilities", "system",
            "toolbox", "settings"
        )
    ),
    GAMES(
        id = "games",
        displayName = "Games",
        groupAliases = listOf(
            "game", "games", "gaming", "play", "arcade"
        )
    ),
    HEALTH(
        id = "health",
        displayName = "Health",
        groupAliases = listOf(
            "health", "fitness", "workout", "wellness", "wellbeing", "diet"
        )
    ),
    SHOPPING(
        id = "shopping",
        displayName = "Shopping",
        groupAliases = listOf(
            "shopping", "shop", "food", "delivery", "store",
            "ecommerce", "e-commerce", "groceries", "grocery", "orders"
        )
    )
}

@Singleton
class AppCategorizerImpl @Inject constructor(
    private val needleEngine: NeedleEngine
) : AppCategorizer {

    private val categorizeTool = ToolDefinition(
        name = "categorize_app",
        description = "Categorize an Android application into a high-level drawer category",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("category") {
                    put("type", "string")
                    put(
                        "description",
                        "Target category: finance, social, productivity, media, utilities, games, health, shopping"
                    )
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("category"))))
        }
    )

    override suspend fun categorizeApps(
        apps: List<AppInfo>,
        existingGroups: List<AppGroup>,
        unassignedOnly: Boolean
    ): List<CategorizationSuggestion> = withContext(Dispatchers.Default) {
        if (existingGroups.isEmpty() || apps.isEmpty()) {
            return@withContext emptyList()
        }

        val candidateApps = if (unassignedOnly) {
            val assignedKeys = existingGroups.flatMap { group ->
                group.items.map { "${it.packageName}/${it.activityName}#${it.userHandleId}" }
            }.toSet()
            val assignedPackages = existingGroups.flatMap { group ->
                group.items.map { it.packageName }
            }.toSet()

            apps.filter { app ->
                app.componentKey !in assignedKeys && app.packageName !in assignedPackages
            }
        } else {
            apps
        }

        val distinctApps = candidateApps.distinctBy { it.componentKey.ifEmpty { it.packageName } }
        val appsByGroup = mutableMapOf<AppGroup, MutableList<AppInfo>>()

        for (app in distinctApps) {
            val category = classifyApp(app) ?: continue
            val matchedGroup = findMatchingGroup(category, existingGroups) ?: continue
            appsByGroup.getOrPut(matchedGroup) { mutableListOf() }.add(app)
        }

        existingGroups.mapNotNull { group ->
            val suggestedApps = appsByGroup[group]
            if (!suggestedApps.isNullOrEmpty()) {
                CategorizationSuggestion(group = group, suggestedApps = suggestedApps)
            } else {
                null
            }
        }
    }

    private suspend fun classifyApp(app: AppInfo): TargetCategory? {
        if (app.isWorkProfile) {
            return TargetCategory.PRODUCTIVITY
        }

        if (needleEngine.modelStatus.value is ModelStatus.Ready) {
            val needleCategory = queryNeedle(app)
            if (needleCategory != null) {
                return needleCategory
            }
        }

        return classifyByMetadata(app)
    }

    private suspend fun queryNeedle(app: AppInfo): TargetCategory? {
        return try {
            val results = needleEngine.predictToolCall(
                query = "Categorize application: ${app.label} (${app.packageName})",
                tools = listOf(categorizeTool)
            )
            val categoryArg = results.firstOrNull { it.toolName == "categorize_app" }
                ?.arguments
                ?.get("category")
                ?.jsonPrimitive
                ?.contentOrNull
            categoryArg?.let { parseTargetCategory(it) }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTargetCategory(value: String): TargetCategory? {
        val normalized = value.lowercase().trim()
        return when {
            normalized.contains("finance") || normalized.contains("money") || normalized.contains("bank") -> TargetCategory.FINANCE
            normalized.contains("social") || normalized.contains("chat") || normalized.contains("messaging") -> TargetCategory.SOCIAL
            normalized.contains("productiv") || normalized.contains("work") || normalized.contains("office") -> TargetCategory.PRODUCTIVITY
            normalized.contains("media") || normalized.contains("music") || normalized.contains("video") || normalized.contains("entertainment") -> TargetCategory.MEDIA
            normalized.contains("util") || normalized.contains("tool") || normalized.contains("system") -> TargetCategory.UTILITIES
            normalized.contains("game") -> TargetCategory.GAMES
            normalized.contains("health") || normalized.contains("fit") -> TargetCategory.HEALTH
            normalized.contains("shop") || normalized.contains("food") -> TargetCategory.SHOPPING
            else -> null
        }
    }

    private fun classifyByMetadata(app: AppInfo): TargetCategory? {
        val pkg = app.packageName.lowercase()
        val label = app.label.lowercase()
        val labelTokens = label.split(" ", "_", "-", ".", "/").filter { it.isNotBlank() }

        // 1. Finance
        if (pkg.contains("splitwise") || pkg.contains("gpay") || pkg.contains("phonepe") ||
            pkg.contains("paytm") || pkg.contains("cred") || pkg.contains("binance") ||
            pkg.contains("coinbase") || pkg.contains("zerodha") || pkg.contains("groww") ||
            pkg.contains("upstox") || pkg.contains("revolut") || pkg.contains("paypal") ||
            pkg.contains("venmo") || pkg.contains("zelle") || pkg.contains("cashapp") ||
            pkg.contains("monzo") || pkg.contains("robinhood") || pkg.contains("etoro") ||
            pkg.contains("kucoin") || pkg.contains("kraken") || pkg.contains("trustwallet") ||
            pkg.contains("metamask") || pkg.contains("paisa") || pkg.contains("bhishi") ||
            pkg.contains("acko") || pkg.contains("saraswat") || pkg.contains("navi") ||
            pkg.contains("jupiter") || pkg.contains("mobikwik") || pkg.contains("freecharge") ||
            pkg.contains("bharatpe") || pkg.contains("indmoney") || pkg.contains("kuvera") ||
            pkg.contains("angelone") || pkg.contains("dhan") || pkg.contains("kite") ||
            pkg.contains("mint") || pkg.contains("ynab") || pkg.contains("quickbooks") ||
            pkg.contains("expensify") || pkg.contains("turbotax") || pkg.contains(".wallet") ||
            pkg.contains("wallet.") || pkg.contains("banking") || pkg.contains(".bank.") ||
            pkg.contains("bank.") || pkg.contains(".pay.") ||
            label.contains("splitwise") || label.contains("groww") || label.contains("bank") ||
            label.contains("finance") || label.contains("wallet") || label.contains("crypto") ||
            label.contains("invest") || label.contains("trading") || label.contains("stocks") ||
            labelTokens.any { it in setOf("pay", "cash", "money", "tax", "taxes", "budget") }
        ) {
            return TargetCategory.FINANCE
        }

        // 2. Social & Communication
        if (pkg.contains("whatsapp") || pkg.contains("telegram") || pkg.contains("signal") ||
            pkg.contains("instagram") || pkg.contains("facebook") || pkg.contains("twitter") ||
            pkg.contains("x.com") || pkg.contains("threads") || pkg.contains("snapchat") ||
            pkg.contains("reddit") || pkg.contains("discord") || pkg.contains("linkedin") ||
            pkg.contains("tiktok") || pkg.contains("wechat") || pkg.contains("viber") ||
            pkg.contains("line") || pkg.contains("kakao") || pkg.contains("skype") ||
            pkg.contains("bluesky") || pkg.contains("mastodon") || pkg.contains("tumblr") ||
            pkg.contains("pinterest") || pkg.contains("clubhouse") || pkg.contains("tinder") ||
            pkg.contains("bumble") || pkg.contains("hinge") || pkg.contains("okcupid") ||
            pkg.contains("badoo") || pkg.contains("grindr") || pkg.contains("meetup") ||
            pkg.contains("messaging") || pkg.contains("messages") || pkg.contains("contacts") ||
            pkg.contains("dialer") ||
            label.contains("chat") || label.contains("social") || label.contains("telegram") ||
            label.contains("reddit") || label.contains("instagram") || label.contains("messenger") ||
            labelTokens.any { it in setOf("message", "messages", "dialer", "phone", "sms", "contacts") }
        ) {
            return TargetCategory.SOCIAL
        }

        // 3. Work / Productivity
        if (pkg.contains("docs") || pkg.contains("sheets") || pkg.contains("slides") ||
            pkg.contains("drive") || pkg.contains("dropbox") || pkg.contains("onedrive") ||
            pkg.contains("slack") || pkg.contains("teams") || pkg.contains("calendar") ||
            pkg.contains("tasks") || pkg.contains("todoist") || pkg.contains("ticktick") ||
            pkg.contains("notion") || pkg.contains("trello") || pkg.contains("jira") ||
            pkg.contains("asana") || pkg.contains("confluence") || pkg.contains("monday") ||
            pkg.contains("clickup") || pkg.contains("outlook") || pkg.contains("wps") ||
            pkg.contains("github") || pkg.contains("gitlab") || pkg.contains("bitbucket") ||
            pkg.contains("claude") || pkg.contains("chatgpt") || pkg.contains("openai") ||
            pkg.contains("kimichat") || pkg.contains("deepseek") || pkg.contains("qwen") ||
            pkg.contains("grok") || pkg.contains("perplexity") || pkg.contains("copilot") ||
            pkg.contains("gemini") || pkg.contains("libreoffice") || pkg.contains("evernote") ||
            pkg.contains("obsidian") || pkg.contains("roam") || pkg.contains("logseq") ||
            pkg.contains("goodnotes") || pkg.contains("onenote") || pkg.contains("simplenote") ||
            pkg.contains("joplin") || pkg.contains("zoom") || pkg.contains("webex") ||
            pkg.contains("gmail") || pkg.contains("protonmail") || pkg.contains("spark") ||
            pkg.contains("camscanner") || pkg.contains("acrobat") || pkg.contains(".mail.") ||
            pkg.contains(".email.") || pkg.contains(".notes.") || pkg.contains(".calendar.") ||
            label.contains("notes") || label.contains("office") || label.contains("todo") ||
            label.contains("docs") || label.contains("teams") || label.contains("chatgpt") ||
            label.contains("claude") || label.contains("workspace") ||
            labelTokens.any { it in setOf("mail", "task", "tasks", "work", "doc", "pdf", "meet", "keep") }
        ) {
            return TargetCategory.PRODUCTIVITY
        }

        // 4. Media & Entertainment
        if (pkg.contains("youtube") || pkg.contains("spotify") || pkg.contains("netflix") ||
            pkg.contains("primevideo") || pkg.contains("prime video") || pkg.contains("hotstar") ||
            pkg.contains("disney") || pkg.contains("hulu") || pkg.contains("peacock") ||
            pkg.contains("crunchyroll") || pkg.contains("camera") || pkg.contains("gallery") ||
            pkg.contains("photos") || pkg.contains("vlc") || pkg.contains("podcast") ||
            pkg.contains("twitch") || pkg.contains("shazam") || pkg.contains("stremio") ||
            pkg.contains("spotiflac") || pkg.contains("fiio") || pkg.contains("soundcloud") ||
            pkg.contains("deezer") || pkg.contains("tidal") || pkg.contains("mxplayer") ||
            pkg.contains("plex") || pkg.contains("kodi") || pkg.contains("audible") ||
            pkg.contains("pocketcasts") || pkg.contains("tunein") || pkg.contains(".music.") ||
            pkg.contains(".video.") || pkg.contains(".player.") || pkg.contains(".radio.") ||
            pkg.contains(".audio.") ||
            label.contains("music") || label.contains("podcast") || label.contains("gallery") ||
            label.contains("photo") || label.contains("video") || label.contains("player") ||
            label.contains("radio") || label.contains("cinema") || label.contains("streaming") ||
            label.contains("spotify") || label.contains("vlc") || label.contains("shazam") ||
            labelTokens.any { it in setOf("audio", "sound", "camera", "movie", "movies", "tv") }
        ) {
            return TargetCategory.MEDIA
        }

        // 5. Utilities & System
        if (pkg.contains("settings") || pkg.contains("calculator") || pkg.contains("clock") ||
            pkg.contains("files") || pkg.contains("filemanager") || pkg.contains("browser") ||
            pkg.contains("chrome") || pkg.contains("firefox") || pkg.contains("edge") ||
            pkg.contains("brave") || pkg.contains("opera") || pkg.contains("vivaldi") ||
            pkg.contains("tor") || pkg.contains("duckduckgo") || pkg.contains("localsend") ||
            pkg.contains("rar") || pkg.contains("7zip") || pkg.contains("termux") ||
            pkg.contains("terminal") || pkg.contains("compass") || pkg.contains("weather") ||
            pkg.contains("authenticator") || pkg.contains("authy") || pkg.contains("bitwarden") ||
            pkg.contains("1password") || pkg.contains("keepass") || pkg.contains("digilocker") ||
            pkg.contains("vending") || pkg.contains("lens") || pkg.contains("fdm") ||
            pkg.contains("cleaner") || pkg.contains("antivirus") || pkg.contains("vpn") ||
            pkg.contains("speedtest") || pkg.contains("flashlight") || pkg.contains("torch") ||
            pkg.contains("recorder") || pkg.contains("scanner") || pkg.contains("toolbox") ||
            label.contains("settings") || label.contains("calculator") || label.contains("clock") ||
            label.contains("files") || label.contains("browser") || label.contains("terminal") ||
            label.contains("utility") || label.contains("utilities") || label.contains("cleaner") ||
            label.contains("vpn") ||
            labelTokens.any { it in setOf("tools", "tool", "timer", "alarm", "stopwatch", "weather", "backup") }
        ) {
            return TargetCategory.UTILITIES
        }

        // 6. Games
        if (pkg.contains("steam") || pkg.contains("playstation") || pkg.contains("xbox") ||
            pkg.contains("epicgames") || pkg.contains("roblox") || pkg.contains("minecraft") ||
            pkg.contains("pubg") || pkg.contains("codm") || pkg.contains("genshin") ||
            pkg.contains("candycrush") || pkg.contains("clashofclans") || pkg.contains("clashroyale") ||
            pkg.contains("retroarch") || pkg.contains("ppsspp") || pkg.contains("emulator") ||
            pkg.contains(".game.") || pkg.contains(".games.") || pkg.contains(".gaming.") ||
            pkg.contains("supercell") || pkg.contains("gameloft") ||
            label.contains("game") || label.contains("games") || label.contains("arcade") ||
            label.contains("puzzle") || label.contains("chess") || label.contains("sudoku") ||
            label.contains("solitaire") || label.contains("simulator")
        ) {
            return TargetCategory.GAMES
        }

        // 7. Health & Fitness
        if (pkg.contains("fitbit") || pkg.contains("garmin") || pkg.contains("strava") ||
            pkg.contains("runkeeper") || pkg.contains("myfitnesspal") || pkg.contains("calm") ||
            pkg.contains("headspace") || pkg.contains("whoop") || pkg.contains("oura") ||
            pkg.contains("withings") || pkg.contains("pedometer") ||
            label.contains("health") || label.contains("fitness") || label.contains("workout") ||
            label.contains("wellness") || label.contains("wellbeing") || label.contains("meditation") ||
            label.contains("yoga") || label.contains("step counter") || label.contains("calorie") ||
            labelTokens.any { it in setOf("gym", "run", "running", "sleep", "diet") }
        ) {
            return TargetCategory.HEALTH
        }

        // 8. Shopping & Food
        if (pkg.contains("amazon") || pkg.contains("ebay") || pkg.contains("flipkart") ||
            pkg.contains("walmart") || pkg.contains("target") || pkg.contains("aliexpress") ||
            pkg.contains("temu") || pkg.contains("shein") || pkg.contains("etsy") ||
            pkg.contains("myntra") || pkg.contains("ajio") || pkg.contains("meesho") ||
            pkg.contains("zomato") || pkg.contains("swiggy") || pkg.contains("ubereats") ||
            pkg.contains("doordash") || pkg.contains("instacart") || pkg.contains("blinkit") ||
            pkg.contains("zepto") || pkg.contains("bigbasket") || pkg.contains("grofers") ||
            pkg.contains("deliveroo") || pkg.contains("postmates") || pkg.contains("grubhub") ||
            label.contains("shopping") || label.contains("grocery") || label.contains("groceries") ||
            label.contains("food delivery") || label.contains("store") || label.contains("market") ||
            labelTokens.any { it in setOf("shop", "cart", "order", "deals") }
        ) {
            return TargetCategory.SHOPPING
        }

        // Fallback: None
        return null
    }

    private fun findMatchingGroup(
        category: TargetCategory,
        existingGroups: List<AppGroup>
    ): AppGroup? {
        var bestGroup: AppGroup? = null
        var bestScore = 0

        for (group in existingGroups) {
            val score = calculateGroupScore(category, group)
            if (score > bestScore) {
                bestScore = score
                bestGroup = group
            }
        }

        return bestGroup
    }

    private fun calculateGroupScore(category: TargetCategory, group: AppGroup): Int {
        val normName = group.name.lowercase().trim()
        val normId = group.id.lowercase().trim()

        // 1. Exact match with category id or displayName (score: 100)
        if (normName == category.id || normName == category.displayName.lowercase()) return 100
        if (normId == category.id || normId == "group_${category.id}" || normId == "${category.id}_group") return 95

        // 2. Exact match with any alias (score: 90)
        for (alias in category.groupAliases) {
            if (normName == alias) return 90
            if (normId == alias || normId == "group_$alias" || normId == "${alias}_group") return 85
        }

        // 3. Name contains alias (score: 75)
        for (alias in category.groupAliases) {
            if (normName.contains(alias)) return 75
        }

        // 4. ID contains alias (score: 60)
        for (alias in category.groupAliases) {
            if (normId.contains(alias)) return 60
        }

        return 0
    }
}
