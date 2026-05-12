package com.hooked.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

/**
 * Encyclopedic biology / behavior data for common freshwater species, baked in.
 * Use when the user's personal data is thin, when validating a personal pattern
 * against biology, or for general species questions. The LLM blends this with
 * personal history.
 */
fun Server.registerGetSpeciesEcologyTool() {
    addTool(
        name = "get_species_ecology",
        description = """
            Look up biology and behavior for a freshwater species: preferred water
            temperature, spawning temperature + months, typical depth, peak activity
            time of day, cold/cool/warm water classification, feeding notes, common
            lures, and seasonal patterns. Use this when you need general knowledge
            independent of the user's catch history — e.g. "when does bass spawn?"
            or "what depth should I target for walleye?" Case-insensitive partial match.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("species") {
                    put("type", "string")
                    put("description", "Species name (case-insensitive partial match, e.g. 'largemouth' or 'walleye')")
                }
            },
            required = listOf("species")
        )
    ) { request ->
        val query = request.params.arguments?.get("species")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'species' parameter is required")), isError = true
            )

        val q = query.lowercase()
        val match = ecology.firstOrNull { rec ->
            rec.name.lowercase().contains(q) || rec.aliases.any { it.lowercase().contains(q) || q.contains(it.lowercase()) }
        }

        if (match == null) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No ecology entry for '$query'. Known species: ${ecology.joinToString(", ") { it.name }}"
            )))
        }

        val text = buildString {
            appendLine("${match.name} — ${match.tempClass} freshwater fish")
            appendLine()
            appendLine("WATER TEMPERATURE:")
            appendLine("  Preferred: ${match.preferredTempF}")
            appendLine("  Spawning:  ${match.spawnTempF} (typically ${match.spawnMonths.joinToString(", ")})")
            appendLine()
            appendLine("BEHAVIOR:")
            appendLine("  Activity peak: ${match.peakActivity}")
            appendLine("  Style:         ${match.style}")
            appendLine("  Typical depth: ${match.depthFt}")
            appendLine()
            appendLine("FEEDING:")
            appendLine("  ${match.feedingNotes}")
            appendLine()
            appendLine("PRESENTATIONS:")
            appendLine("  ${match.lures}")
            appendLine()
            appendLine("SEASONAL PATTERN:")
            appendLine("  ${match.seasonal}")
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}

private data class Ecology(
    val name: String,
    val aliases: List<String>,
    val tempClass: String,
    val preferredTempF: String,
    val spawnTempF: String,
    val spawnMonths: List<String>,
    val peakActivity: String,
    val style: String,
    val depthFt: String,
    val feedingNotes: String,
    val lures: String,
    val seasonal: String
)

private val ecology = listOf(
    Ecology(
        name = "Largemouth Bass",
        aliases = listOf("lmb", "bigmouth"),
        tempClass = "warm-water",
        preferredTempF = "65-80°F",
        spawnTempF = "60-65°F",
        spawnMonths = listOf("late April", "May", "early June"),
        peakActivity = "dawn and dusk (crepuscular); midday in cloudy/low-light",
        style = "ambush predator near cover",
        depthFt = "shallow 2-10 ft most of season; suspended 10-25 ft midsummer / late fall",
        feedingNotes = "Targets weed edges, wood, docks, and rock transitions. Aggressive on falling barometer pre-frontal. Slows in bluebird high-pressure days. Shad/baitfish + crayfish in most lakes.",
        lures = "Texas-rigged worms, chatterbaits, jigs near cover; topwater frogs and walking baits at first/last light; deep crankbaits on offshore structure summer.",
        seasonal = "Pre-spawn (water 50-60°F): cranks/jerkbaits on transition banks. Spawn (60-65°F): sight-fishing beds. Post-spawn through summer: weed edges. Fall: chase shad schools. Winter: deep, slow."
    ),
    Ecology(
        name = "Smallmouth Bass",
        aliases = listOf("smallie", "smb", "bronzeback"),
        tempClass = "cool-to-warm",
        preferredTempF = "60-72°F",
        spawnTempF = "58-65°F",
        spawnMonths = listOf("May", "June"),
        peakActivity = "dawn and dusk; tolerates midday better than largemouth in cooler water",
        style = "active hunter on hard bottom",
        depthFt = "rock and gravel; 4-30 ft depending on season",
        feedingNotes = "Crayfish, perch, gobies. Prefers current and rocky structure. Active in wind. Holds tighter to bottom than largemouth.",
        lures = "Tubes, drop-shot, ned rigs, jerkbaits, small crankbaits on rocky points.",
        seasonal = "Strong spring on warming rock flats. Summer goes deep — main-lake points and humps. Fall feed-up on shallow rock 50-60°F water."
    ),
    Ecology(
        name = "Walleye",
        aliases = listOf("eyes", "marble eye"),
        tempClass = "cool-water",
        preferredTempF = "55-68°F",
        spawnTempF = "42-50°F",
        spawnMonths = listOf("late March", "April"),
        peakActivity = "low light: dawn, dusk, night; deeper midday",
        style = "open-water predator with light-sensitive eyes",
        depthFt = "10-30 ft typical; shallow at low light and during spawn",
        feedingNotes = "Visual feeders adapted to dim light. Wind-blown shorelines (the 'walleye chop') concentrate prey and dim the water — classic productive condition.",
        lures = "Jig + minnow, crawler harnesses, deep-diving crankbaits, slip-bobber with leeches at night.",
        seasonal = "Spring run to rivers/inflows to spawn. Summer on main-lake structure 15-30 ft. Fall back shallow as water cools. Excellent ice fishery in winter."
    ),
    Ecology(
        name = "Northern Pike",
        aliases = listOf("pike", "northerns"),
        tempClass = "cool-water",
        preferredTempF = "55-70°F",
        spawnTempF = "40-52°F",
        spawnMonths = listOf("March", "April"),
        peakActivity = "daytime, often midday in cooler water",
        style = "ambush predator in weeds",
        depthFt = "5-15 ft most of year; deeper basin in midsummer",
        feedingNotes = "Loves cabbage and pencil reed weed edges. Eats fish 1/3 of its body length. Activity drops sharply in water above 75°F — looks for thermal refuge.",
        lures = "Inline spinners, spoons, large jerkbaits, swimbaits, dead-stick smelt under tip-up in winter.",
        seasonal = "Aggressive early spring post-spawn. Summer slows in warm water but small ones still hit. Fall feed-up best for big fish. Strong ice season."
    ),
    Ecology(
        name = "Muskellunge",
        aliases = listOf("musky", "muskie"),
        tempClass = "cool-water",
        preferredTempF = "60-72°F",
        spawnTempF = "50-60°F",
        spawnMonths = listOf("April", "May"),
        peakActivity = "low light and during weather changes",
        style = "apex predator; 'fish of 10,000 casts'",
        depthFt = "weeds 5-15 ft; suspended in midsummer; deep weedlines in fall",
        feedingNotes = "Strikes triggered by figure-8 boatside maneuvers. Moves correlate strongly with moon phase and barometric drops. Less affected by water temp than activity level.",
        lures = "Bucktails, large rubber, glide baits, topwater. Big — typically 8-12 inch lures.",
        seasonal = "Spring shallow over emerging weeds. Summer follow main-lake structure and ciscoes (where present). Fall = trophy season on big rubber and live suckers."
    ),
    Ecology(
        name = "Bluegill",
        aliases = listOf("gill", "sunfish", "panfish"),
        tempClass = "warm-water",
        preferredTempF = "70-82°F",
        spawnTempF = "68-75°F",
        spawnMonths = listOf("late May", "June", "early July"),
        peakActivity = "midday and warm afternoons",
        style = "schooling sight-feeder",
        depthFt = "2-10 ft on weed flats and around docks; deeper 15-25 ft in midsummer heat or winter",
        feedingNotes = "Eats insects, small invertebrates, fry. Beds in colonies — find one bed, find dozens.",
        lures = "Small jigs, waxworms, redworms, dry flies, foam spiders, tiny tubes under a slip-bobber.",
        seasonal = "Spawning beds in shallow sand/gravel — easy to spot. Summer suspended over weed edges. Fall they pack up in main-lake basins."
    ),
    Ecology(
        name = "Crappie",
        aliases = listOf("specks", "papermouth"),
        tempClass = "cool-to-warm",
        preferredTempF = "65-75°F",
        spawnTempF = "58-65°F",
        spawnMonths = listOf("late April", "May"),
        peakActivity = "dawn and dusk; suspended day",
        style = "schooling open-water feeder",
        depthFt = "suspended 8-20 ft most of year; shallow during spawn",
        feedingNotes = "Eats minnows and small invertebrates. Soft mouth — set hook gently. Notorious for moving up and down the column with light.",
        lures = "Small minnows or 1.5-2\" plastics on 1/16-1/32 jigs; spider rigging and slip-bobbers in deeper water.",
        seasonal = "Spring stage in coves and bays. Spawn shallow in brush/wood. Summer move to deeper main-lake brushpiles and bridge pilings. Fall and ice both productive."
    ),
    Ecology(
        name = "Yellow Perch",
        aliases = listOf("perch"),
        tempClass = "cool-water",
        preferredTempF = "55-70°F",
        spawnTempF = "44-50°F",
        spawnMonths = listOf("March", "April"),
        peakActivity = "midday in cool seasons; dawn in summer",
        style = "schooling bottom-cruiser",
        depthFt = "10-30 ft on sand/gravel flats",
        feedingNotes = "Eats minnows, insects, snails. Active during daylight (unlike walleye). Strong winter ice fishery.",
        lures = "Small jigging spoons, minnows, soft plastics on tiny jigs.",
        seasonal = "Spring tight to bottom on rocky/sand transitions. Summer in 15-30 ft. Fall feed-up before ice. Premier ice species."
    ),
    Ecology(
        name = "Rainbow Trout",
        aliases = listOf("rainbow", "stocker"),
        tempClass = "cold-water",
        preferredTempF = "50-60°F",
        spawnTempF = "44-55°F",
        spawnMonths = listOf("late March", "April", "May"),
        peakActivity = "low light; cooler water",
        style = "drift-feeding insectivore",
        depthFt = "shallow streams; in lakes seeks thermocline / cold springs in summer",
        feedingNotes = "Hatch-driven in streams. In lakes follows water 50-58°F — usually 15-30 ft once surface warms past 65°F.",
        lures = "Small spinners, spoons, flies matching the local hatch, PowerBait off bottom for stockers.",
        seasonal = "Spring shallow in cool runoff. Summer deep in lakes / shaded riffles in streams. Fall back shallow. Excellent winter stream fishery in regions that don't freeze."
    ),
    Ecology(
        name = "Brown Trout",
        aliases = listOf("browns"),
        tempClass = "cold-water",
        preferredTempF = "55-65°F",
        spawnTempF = "44-50°F",
        spawnMonths = listOf("October", "November"),
        peakActivity = "low light, especially dawn and after dark — most nocturnal of the trouts",
        style = "predatory drift-and-ambush",
        depthFt = "river runs and pools; lake-run in deep cold water",
        feedingNotes = "Eats baitfish, large insects, mice (yes really). Wariest of the trouts — fish small tippet and approach quietly.",
        lures = "Streamers (woolly buggers, sculpins), nymphs under indicator, big dries during hex/cicada hatches, mouse patterns at night in summer.",
        seasonal = "Spring/fall best. Summer push for deep cold tributaries or thermal refuge. Late-fall spawn run brings the biggest fish into rivers."
    ),
    Ecology(
        name = "Channel Catfish",
        aliases = listOf("channel cat", "catfish"),
        tempClass = "warm-water",
        preferredTempF = "70-85°F",
        spawnTempF = "70-80°F",
        spawnMonths = listOf("June", "July"),
        peakActivity = "night, dusk; falling barometer",
        style = "scent-driven scavenger and predator",
        depthFt = "deep holes by day; shallow flats and current breaks at night",
        feedingNotes = "Smells and tastes food via barbels. Locks onto blood, fermented, and live baits. Storms and rising water often trigger feeding binges.",
        lures = "Cut bait, chicken liver, dough baits, big stinky shrimp; sometimes big jigs/spinners during flood pulses.",
        seasonal = "Big run from late spring through summer. Fall thinning as water cools. Winter slow — sit deep in river holes."
    )
)
