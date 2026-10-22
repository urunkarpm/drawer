package com.urunkarpm.drawer.core.data.ai

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.needle.NeedleEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
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
            "crypto", "invest", "investment", "pay", "payment", "taxes", "budget", "upi"
        )
    ),
    SOCIAL(
        id = "social",
        displayName = "Social",
        groupAliases = listOf(
            "social", "chat", "messaging", "messages", "messenger",
            "friends", "communication", "connect", "dialer", "contacts", "dating"
        )
    ),
    PRODUCTIVITY(
        id = "productivity",
        displayName = "Productivity",
        groupAliases = listOf(
            "work", "productivity", "deep work", "office", "projects",
            "study", "notes", "tasks", "focus", "documents", "docs", "workspace"
        )
    ),
    MEDIA(
        id = "media",
        displayName = "Media",
        groupAliases = listOf(
            "media", "entertainment", "music", "video", "streaming",
            "photos", "movies", "gallery", "audio", "podcasts", "cinema"
        )
    ),
    UTILITIES(
        id = "utilities",
        displayName = "Utilities",
        groupAliases = listOf(
            "tool", "tools", "utility", "utilities", "system",
            "toolbox", "settings", "browser", "navigation", "travel", "transit"
        )
    ),
    GAMES(
        id = "games",
        displayName = "Games",
        groupAliases = listOf(
            "game", "games", "gaming", "play", "arcade", "emulator"
        )
    ),
    HEALTH(
        id = "health",
        displayName = "Health",
        groupAliases = listOf(
            "health", "fitness", "health & fitness", "health and fitness", "workout", "wellness", "wellbeing", "diet", "gym"
        )
    ),
    SHOPPING(
        id = "shopping",
        displayName = "Shopping",
        groupAliases = listOf(
            "shopping", "shop", "food", "delivery", "store",
            "ecommerce", "e-commerce", "groceries", "grocery", "orders", "market"
        )
    )
}

@Singleton
class AppCategorizerImpl @Inject constructor(
    private val needleEngine: NeedleEngine
) : AppCategorizer {

    // ponytail: instant, deterministic, 100% offline rule and metadata engine is always ready without waiting on model downloads or LLM inference.
    override val isModelReady: Boolean
        get() = true

    companion object {
        val DEFAULT_SUGGESTED_GROUPS: List<AppGroup> = listOf(
            AppGroup(id = "group_finance", name = "Finance", iconName = "credit_card", colorHex = "#10B981", orderIndex = 0),
            AppGroup(id = "group_social", name = "Social", iconName = "chat", colorHex = "#3B82F6", orderIndex = 1),
            AppGroup(id = "group_work", name = "Deep Work", iconName = "work", colorHex = "#8B5CF6", orderIndex = 2),
            AppGroup(id = "group_media", name = "Media", iconName = "play_circle", colorHex = "#EC4899", orderIndex = 3),
            AppGroup(id = "group_utilities", name = "Utilities", iconName = "build", colorHex = "#F59E0B", orderIndex = 4),
            AppGroup(id = "group_games", name = "Games", iconName = "sports_esports", colorHex = "#EF4444", orderIndex = 5),
            AppGroup(id = "group_health", name = "Health & Fitness", iconName = "fitness_center", colorHex = "#14B8A6", orderIndex = 6),
            AppGroup(id = "group_shopping", name = "Shopping", iconName = "shopping_bag", colorHex = "#F97316", orderIndex = 7)
        )

        fun defaultGroupForCategory(category: TargetCategory): AppGroup {
            return when (category) {
                TargetCategory.FINANCE -> DEFAULT_SUGGESTED_GROUPS[0]
                TargetCategory.SOCIAL -> DEFAULT_SUGGESTED_GROUPS[1]
                TargetCategory.PRODUCTIVITY -> DEFAULT_SUGGESTED_GROUPS[2]
                TargetCategory.MEDIA -> DEFAULT_SUGGESTED_GROUPS[3]
                TargetCategory.UTILITIES -> DEFAULT_SUGGESTED_GROUPS[4]
                TargetCategory.GAMES -> DEFAULT_SUGGESTED_GROUPS[5]
                TargetCategory.HEALTH -> DEFAULT_SUGGESTED_GROUPS[6]
                TargetCategory.SHOPPING -> DEFAULT_SUGGESTED_GROUPS[7]
            }
        }
    }

    override suspend fun categorizeApps(
        apps: List<AppInfo>,
        existingGroups: List<AppGroup>,
        unassignedOnly: Boolean,
        strictness: String,
        forceWorkProfile: Boolean
    ): List<CategorizationSuggestion> = withContext(Dispatchers.Default) {
        if (apps.isEmpty()) {
            return@withContext emptyList()
        }

        // Filter candidate apps if only unassigned apps should be organized
        val candidateApps = if (unassignedOnly) {
            val assignedKeys = existingGroups.flatMap { group ->
                group.items.map { "${it.packageName}/${it.activityName}#${it.userHandleId}" }
            }.toSet()
            val assignedPackages = existingGroups.flatMap { group ->
                group.items.map { it.packageName }
            }.toSet()

            apps.filter { app ->
                val key = app.componentKey.ifEmpty { "${app.packageName}/${app.activityName}#${app.userHandleId}" }
                key !in assignedKeys && app.packageName !in assignedPackages
            }
        } else {
            apps
        }

        // ponytail: robustly deduplicate candidates by normalized component key or package name
        val distinctApps = candidateApps.distinctBy {
            val key = it.componentKey.ifBlank { "${it.packageName}/${it.activityName}#${it.userHandleId}" }
            key.ifBlank { it.packageName }
        }

        // Active groups track existing user drawers plus any newly created drawers
        val activeGroups = existingGroups.toMutableList()
        val knownGroupIds = (existingGroups.map { it.id }).toMutableSet()
        var nextOrderIndex = (existingGroups.maxOfOrNull { it.orderIndex } ?: -1) + 1

        val appsByGroup = mutableMapOf<String, MutableList<AppInfo>>()

        for (app in distinctApps) {
            coroutineContext.ensureActive()
            val category = try {
                classifyApp(app, strictness, forceWorkProfile)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } ?: continue

            // 1. Check existing or already created groups for a match
            var matchedGroup = findMatchingGroup(category, activeGroups, strictness)

            // 2. ponytail: if no matching group exists, dynamically create an additional drawer for this category so no apps are discarded
            if (matchedGroup == null) {
                val template = defaultGroupForCategory(category)
                val baseId = template.id
                var uniqueId = baseId
                var counter = 1
                while (uniqueId in knownGroupIds) {
                    uniqueId = "${baseId}_$counter"
                    counter++
                }
                val newGroup = template.copy(
                    id = uniqueId,
                    orderIndex = nextOrderIndex++
                )
                knownGroupIds.add(newGroup.id)
                activeGroups.add(newGroup)
                matchedGroup = newGroup
            }

            appsByGroup.getOrPut(matchedGroup.id) { mutableListOf() }.add(app)
        }

        // Return suggestions preserving existing groups first, followed by any newly created groups
        activeGroups.mapNotNull { group ->
            val suggestedApps = appsByGroup[group.id]
            if (!suggestedApps.isNullOrEmpty()) {
                CategorizationSuggestion(group = group, suggestedApps = suggestedApps)
            } else {
                null
            }
        }
    }

    private suspend fun classifyApp(
        app: AppInfo,
        strictness: String,
        forceWorkProfile: Boolean
    ): TargetCategory? {
        if (forceWorkProfile && app.isWorkProfile) {
            return TargetCategory.PRODUCTIVITY
        }

        // ponytail: purely rule & metadata driven; no reliance on AI models ensures instant, bulletproof, offline categorization.
        return classifyByMetadata(app, strictness)
    }

    private fun classifyByMetadata(app: AppInfo, strictness: String = "BALANCED"): TargetCategory? {
        val isStrict = strictness.equals("STRICT", ignoreCase = true)
        val isAggressive = strictness.equals("AGGRESSIVE", ignoreCase = true)
        val allowLooseTokens = !isStrict

        // Defensive sanitization: handle potential null or malformed strings safely
        val pkg = app.packageName.lowercase().trim()
        val label = app.label.lowercase().trim()
        val labelTokens = label.split(Regex("[\\s_\\-./,;:!?()\\[\\]{}]+")).filter { it.isNotBlank() }

        // 1. Finance & Banking & UPI & Investments
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
            pkg.contains("bank.") || pkg.contains(".pay.") || pkg.contains("coindcx") ||
            pkg.contains("wazirx") || pkg.contains("coinswitch") || pkg.contains("fi.money") ||
            pkg.contains("onecard") || pkg.contains("slice") || pkg.contains("kreditbee") ||
            pkg.contains("moneytap") || pkg.contains("cleartax") || pkg.contains("toshl") ||
            pkg.contains("bluecoins") || pkg.contains("spendee") || pkg.contains("phantom") ||
            pkg.contains("sbi") || pkg.contains("yono") || pkg.contains("hdfc") ||
            pkg.contains("icici") || pkg.contains("imobile") || pkg.contains("axis") ||
            pkg.contains("kotak") || pkg.contains("canara") || pkg.contains("baroda") ||
            pkg.contains("pnb") || pkg.contains("indusind") || pkg.contains("federalbank") ||
            pkg.contains("idfc") || pkg.contains("rbl") || pkg.contains("chase") ||
            pkg.contains("bankofamerica") || pkg.contains("wellsfargo") || pkg.contains("citi") ||
            pkg.contains("barclays") || pkg.contains("hsbc") || pkg.contains("capitalone") ||
            label.contains("splitwise") || label.contains("groww") || label.contains("bank") ||
            label.contains("finance") || label.contains("wallet") || label.contains("crypto") ||
            label.contains("invest") || label.contains("trading") || label.contains("stocks") ||
            label.contains("phonepe") || label.contains("paytm") || label.contains("gpay") ||
            label.contains("cred") || label.contains("zerodha") || label.contains("upstox") ||
            label.contains("angel one") || label.contains("dhan") || label.contains("kite") ||
            label.contains("bhim") || label.contains("loan") || label.contains("credit card") ||
            label.contains("mutual fund") || label.contains("demat") || label.contains("bitcoin") ||
            (allowLooseTokens && labelTokens.any { it in setOf("pay", "cash", "money", "tax", "taxes", "budget", "upi", "coin", "expense", "expenses") })
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
            pkg.contains("dialer") || pkg.contains("truecaller") || pkg.contains("bereal") ||
            pkg.contains("happn") || pkg.contains("jeevansathi") || pkg.contains("shaadi") ||
            pkg.contains("matrimony") || pkg.contains("element.io") || pkg.contains("threema") ||
            pkg.contains("session") ||
            (label.contains("chat") && !label.contains("chatgpt") && !pkg.contains("chatgpt")) || label.contains("social") || label.contains("telegram") ||
            label.contains("reddit") || label.contains("instagram") || label.contains("messenger") ||
            label.contains("whatsapp") || label.contains("discord") || label.contains("signal") ||
            label.contains("twitter") || label.contains("threads") || label.contains("snapchat") ||
            label.contains("linkedin") || label.contains("contacts") || label.contains("dialer") ||
            label.contains("truecaller") || label.contains("tinder") || label.contains("bumble") ||
            label.contains("hinge") || label.contains("dating") ||
            (allowLooseTokens && labelTokens.any { it in setOf("message", "messages", "dialer", "phone", "sms", "contacts", "call", "talk") })
        ) {
            return TargetCategory.SOCIAL
        }

        // 3. Work / Productivity & Office & AI Assistants
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
            pkg.contains("vflat") || pkg.contains("foxit") || pkg.contains("linear") ||
            pkg.contains("basecamp") || pkg.contains("superhuman") || pkg.contains("bluemail") ||
            pkg.contains("thunderbird") || pkg.contains("keep") || pkg.contains("zoho") ||
            pkg.contains("mega.privacy") || pkg.contains("pcloud") ||
            label.contains("notes") || label.contains("office") || label.contains("todo") ||
            label.contains("docs") || label.contains("teams") || label.contains("chatgpt") ||
            label.contains("claude") || label.contains("workspace") || label.contains("gemini") ||
            label.contains("copilot") || label.contains("perplexity") || label.contains("notion") ||
            label.contains("obsidian") || label.contains("evernote") || label.contains("drive") ||
            label.contains("sheets") || label.contains("slides") || label.contains("word") ||
            label.contains("excel") || label.contains("powerpoint") || label.contains("calendar") ||
            label.contains("tasks") || label.contains("scanner") || label.contains("pdf reader") ||
            label.contains("document") || label.contains("documents") ||
            (allowLooseTokens && labelTokens.any { it in setOf("mail", "task", "tasks", "work", "doc", "pdf", "meet", "keep", "note", "notes", "sheet", "slide", "scan") })
        ) {
            return TargetCategory.PRODUCTIVITY
        }

        // 4. Media & Entertainment & Music & Video
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
            pkg.contains(".audio.") || pkg.contains("jiocinema") || pkg.contains("sonyliv") ||
            pkg.contains("zee5") || pkg.contains("snapseed") || pkg.contains("lightroom") ||
            pkg.contains("vsco") || pkg.contains("picsart") || pkg.contains("inshot") ||
            pkg.contains("capcut") || pkg.contains("poweramp") || pkg.contains("musicolet") ||
            pkg.contains("blackplayer") || pkg.contains("jiosaavn") || pkg.contains("wynk") ||
            pkg.contains("gaana") || pkg.contains("kuku.fm") || pkg.contains("pocketfm") ||
            pkg.contains("storytel") || pkg.contains("apple.music") || pkg.contains("castbox") ||
            pkg.contains("antennapod") || pkg.contains("sunnxt") || pkg.contains("aha") ||
            pkg.contains("kick") ||
            label.contains("music") || label.contains("podcast") || label.contains("gallery") ||
            label.contains("photo") || label.contains("video") || label.contains("player") ||
            label.contains("radio") || label.contains("cinema") || label.contains("streaming") ||
            label.contains("spotify") || label.contains("vlc") || label.contains("shazam") ||
            label.contains("netflix") || label.contains("youtube") || label.contains("prime video") ||
            label.contains("hotstar") || label.contains("camera") || label.contains("jiocinema") ||
            label.contains("sonyliv") || label.contains("zee5") || label.contains("mx player") ||
            label.contains("audible") || label.contains("fm") || label.contains("sound") ||
            label.contains("mp3") ||
            (allowLooseTokens && labelTokens.any { it in setOf("audio", "sound", "camera", "movie", "movies", "tv", "song", "songs", "film", "album", "track") })
        ) {
            return TargetCategory.MEDIA
        }

        // 5. Utilities & System & Navigation
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
            pkg.contains("maps") || pkg.contains("waze") || pkg.contains("uber") ||
            pkg.contains("ola") || pkg.contains("rapido") || pkg.contains("irctc") ||
            pkg.contains("rly") || pkg.contains("rail") || pkg.contains("transit") ||
            pkg.contains("citymapper") || pkg.contains("protonvpn") || pkg.contains("nordvpn") ||
            pkg.contains("mullvad") || pkg.contains("wireguard") || pkg.contains("adguard") ||
            pkg.contains("gboard") || pkg.contains("swiftkey") || pkg.contains("solidexplorer") ||
            pkg.contains("cxexplorer") || pkg.contains("zarchiver") ||
            label.contains("settings") || label.contains("calculator") || label.contains("clock") ||
            label.contains("files") || label.contains("browser") || label.contains("terminal") ||
            label.contains("utility") || label.contains("utilities") || label.contains("cleaner") ||
            label.contains("vpn") || label.contains("maps") || label.contains("weather") ||
            label.contains("authenticator") || label.contains("speedtest") || label.contains("flashlight") ||
            label.contains("torch") || label.contains("compass") || label.contains("uber") ||
            label.contains("ola") || label.contains("rapido") || label.contains("transit") ||
            (allowLooseTokens && labelTokens.any { it in setOf("tools", "tool", "timer", "alarm", "stopwatch", "weather", "backup", "vpn", "calc", "browser", "map", "nav", "cab", "ride") })
        ) {
            return TargetCategory.UTILITIES
        }

        // 6. Games & Emulators
        if (pkg.contains("steam") || pkg.contains("playstation") || pkg.contains("xbox") ||
            pkg.contains("epicgames") || pkg.contains("roblox") || pkg.contains("minecraft") ||
            pkg.contains("pubg") || pkg.contains("codm") || pkg.contains("genshin") ||
            pkg.contains("candycrush") || pkg.contains("clashofclans") || pkg.contains("clashroyale") ||
            pkg.contains("retroarch") || pkg.contains("ppsspp") || pkg.contains("emulator") ||
            pkg.contains(".game.") || pkg.contains(".games.") || pkg.contains(".gaming.") ||
            pkg.contains("supercell") || pkg.contains("gameloft") || pkg.contains("krafton") ||
            pkg.contains("riotgames") || pkg.contains("blizzard") || pkg.contains("ea.com") ||
            pkg.contains("ubisoft") || pkg.contains("mojang") || pkg.contains("hoyoverse") ||
            pkg.contains("mihoyo") || pkg.contains("freefire") || pkg.contains("subwaysurf") ||
            pkg.contains("asphalt") || pkg.contains("chess") || pkg.contains("ludo") ||
            pkg.contains("sudoku") || pkg.contains("solitaire") || pkg.contains("pokemon") ||
            pkg.contains("brawlstars") || pkg.contains("aethersx2") || pkg.contains("citra") ||
            pkg.contains("dolphin") ||
            label.contains("game") || label.contains("games") || label.contains("arcade") ||
            label.contains("puzzle") || label.contains("chess") || label.contains("sudoku") ||
            label.contains("solitaire") || label.contains("simulator") || label.contains("pubg") ||
            label.contains("minecraft") || label.contains("roblox") || label.contains("free fire") ||
            label.contains("call of duty") || label.contains("clash") || label.contains("racing") ||
            label.contains("rpg") || label.contains("board") || label.contains("cards") ||
            label.contains("ludo") || label.contains("pool") || label.contains("cricket") ||
            label.contains("fifa") || label.contains("emulator") ||
            (allowLooseTokens && labelTokens.any { it in setOf("play", "game", "games", "arcade", "craft", "kart") })
        ) {
            return TargetCategory.GAMES
        }

        // 7. Health & Fitness & Wellness
        if (pkg.contains("fitbit") || pkg.contains("garmin") || pkg.contains("strava") ||
            pkg.contains("runkeeper") || pkg.contains("myfitnesspal") || pkg.contains("calm") ||
            pkg.contains("headspace") || pkg.contains("whoop") || pkg.contains("oura") ||
            pkg.contains("withings") || pkg.contains("pedometer") || pkg.contains("samsung.health") ||
            pkg.contains("google.fit") || pkg.contains("nike.plus") || pkg.contains("cult.fit") ||
            pkg.contains("healthifyme") || pkg.contains("flo.period") || pkg.contains("clue") ||
            pkg.contains("1mg") || pkg.contains("pharmeasy") || pkg.contains("apollo") ||
            pkg.contains("lifesum") || pkg.contains("cronometer") || pkg.contains("yuka") ||
            pkg.contains("fasting") || pkg.contains("freeletics") || pkg.contains("hevy") ||
            pkg.contains("jefit") ||
            label.contains("health") || label.contains("fitness") || label.contains("workout") ||
            label.contains("wellness") || label.contains("wellbeing") || label.contains("meditation") ||
            label.contains("yoga") || label.contains("step counter") || label.contains("calorie") ||
            label.contains("strava") || label.contains("pedometer") || label.contains("gym") ||
            label.contains("diet") || label.contains("pharmacy") || label.contains("medicine") ||
            (allowLooseTokens && labelTokens.any { it in setOf("gym", "run", "running", "sleep", "diet", "step", "steps", "heart", "pulse", "fasting") })
        ) {
            return TargetCategory.HEALTH
        }

        // 8. Shopping & Food Delivery & E-Commerce
        if (pkg.contains("amazon") || pkg.contains("ebay") || pkg.contains("flipkart") ||
            pkg.contains("walmart") || pkg.contains("target") || pkg.contains("aliexpress") ||
            pkg.contains("temu") || pkg.contains("shein") || pkg.contains("etsy") ||
            pkg.contains("myntra") || pkg.contains("ajio") || pkg.contains("meesho") ||
            pkg.contains("zomato") || pkg.contains("swiggy") || pkg.contains("ubereats") ||
            pkg.contains("doordash") || pkg.contains("instacart") || pkg.contains("blinkit") ||
            pkg.contains("zepto") || pkg.contains("bigbasket") || pkg.contains("grofers") ||
            pkg.contains("deliveroo") || pkg.contains("postmates") || pkg.contains("grubhub") ||
            pkg.contains("nykaa") || pkg.contains("tatacliq") || pkg.contains("shopsy") ||
            pkg.contains("olx") || pkg.contains("quikr") || pkg.contains("dominos") ||
            pkg.contains("mcdonalds") || pkg.contains("kfc") || pkg.contains("starbucks") ||
            pkg.contains("dineout") ||
            label.contains("shopping") || label.contains("grocery") || label.contains("groceries") ||
            label.contains("food delivery") || label.contains("store") || label.contains("market") ||
            label.contains("amazon") || label.contains("flipkart") || label.contains("myntra") ||
            label.contains("swiggy") || label.contains("zomato") || label.contains("blinkit") ||
            label.contains("zepto") || label.contains("bigbasket") ||
            (allowLooseTokens && labelTokens.any { it in setOf("shop", "cart", "order", "deals", "coupon", "coupons", "buy", "mall", "retail") })
        ) {
            return TargetCategory.SHOPPING
        }

        if (isAggressive) {
            return when {
                pkg.contains("android") || pkg.contains("system") || pkg.contains("google") || pkg.contains("service") -> TargetCategory.UTILITIES
                label.contains("play") || label.contains("game") -> TargetCategory.GAMES
                label.contains("media") || label.contains("view") || label.contains("audio") || label.contains("stream") -> TargetCategory.MEDIA
                label.contains("work") || label.contains("doc") || label.contains("pro") || label.contains("office") -> TargetCategory.PRODUCTIVITY
                label.contains("shop") || label.contains("store") || label.contains("pay") || label.contains("cart") -> TargetCategory.SHOPPING
                label.contains("chat") || label.contains("social") || label.contains("share") -> TargetCategory.SOCIAL
                else -> TargetCategory.UTILITIES
            }
        }

        // Fallback: None
        return null
    }

    private fun findMatchingGroup(
        category: TargetCategory,
        existingGroups: List<AppGroup>,
        strictness: String = "BALANCED"
    ): AppGroup? {
        var bestGroup: AppGroup? = null
        var bestScore = 0
        val minScoreThreshold = if (strictness.equals("STRICT", ignoreCase = true)) 75 else 50

        for (group in existingGroups) {
            val score = calculateGroupScore(category, group)
            if (score > bestScore && score >= minScoreThreshold) {
                bestScore = score
                bestGroup = group
            }
        }

        if (bestGroup == null && strictness.equals("AGGRESSIVE", ignoreCase = true)) {
            bestGroup = existingGroups.maxByOrNull { calculateGroupScore(category, it) }
                ?.takeIf { calculateGroupScore(category, it) > 0 }
                ?: existingGroups.firstOrNull()
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
