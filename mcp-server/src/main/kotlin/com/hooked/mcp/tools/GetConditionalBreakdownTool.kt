package com.hooked.mcp.tools

import com.hooked.mcp.tables.CatchesTable
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Bucket catches by a single environmental/temporal dimension and return per-bucket
 * counts + top species + relative species density across buckets. Designed to let the
 * model say "X is 3× more common under condition Y" rather than just listing aggregates.
 */
fun Server.registerGetConditionalBreakdownTool(userId: UUID) {
    addTool(
        name = "get_conditional_breakdown",
        description = """
            Bucket catches by a single dimension and return per-bucket counts, top species,
            and *relative density* of each species across buckets. Use this whenever a user
            asks about correlations (e.g. "does pressure affect what I catch?", "are bass
            more likely in the morning?"). Output gives you direct phrases like
            "X is 2.4× more common under falling pressure."

            Valid dimensions:
              - "pressure_trend"        (rising / stable / falling, derived from weather)
              - "wind_direction"        (N / NE / E / SE / S / SW / W / NW)
              - "hour_of_day"           (Pre-dawn / Dawn / Morning / Afternoon / Evening / Night)
              - "cloud_cover"           (Clear / Partly cloudy / Mostly cloudy / Overcast)
              - "air_temp"              (<40 / 40-55 / 55-70 / 70-85 / >85 °F)
              - "water_temp"            (<50 / 50-60 / 60-70 / 70-80 / >80 °F)
              - "weather_main"          (Clear / Clouds / Rain / Thunderstorm / Snow / etc.)
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("dimension") {
                    put("type", "string")
                    put("description", "One of: pressure_trend, wind_direction, hour_of_day, cloud_cover, air_temp, water_temp, weather_main")
                }
            },
            required = listOf("dimension")
        )
    ) { request ->
        val dimension = request.params.arguments?.get("dimension")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'dimension' parameter is required")), isError = true
            )

        data class CatchRow(
            val species: String?,
            val hour: Int?,
            val pressure: Double?,
            val pressureTrend: String?,
            val windDeg: Double?,
            val cloudPct: Int?,
            val airTemp: Double?,
            val waterTemp: Double?,
            val weatherMain: String?
        )

        val rows: List<CatchRow> = transaction {
            CatchesTable
                .select(CatchesTable.species, CatchesTable.caughtAt, CatchesTable.weatherData)
                .where { CatchesTable.userId eq userId }
                .map { row ->
                    val w = row[CatchesTable.weatherData]?.jsonObject
                    val main = w?.get("main")?.jsonObject
                    val wind = w?.get("wind")?.jsonObject
                    val clouds = w?.get("clouds")?.jsonObject
                    val weatherArr = w?.get("weather")?.jsonArray?.firstOrNull()?.jsonObject

                    CatchRow(
                        species = row[CatchesTable.species]?.trim()?.takeIf { it.isNotEmpty() },
                        hour = row[CatchesTable.caughtAt]?.hour,
                        pressure = main?.get("pressure")?.jsonPrimitive?.doubleOrNull,
                        pressureTrend = w?.get("pressure_trend")?.jsonPrimitive?.contentOrNull,
                        windDeg = wind?.get("deg")?.jsonPrimitive?.doubleOrNull,
                        cloudPct = clouds?.get("all")?.jsonPrimitive?.intOrNull,
                        airTemp = main?.get("temp")?.jsonPrimitive?.doubleOrNull,
                        waterTemp = w?.get("water_temp")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                        weatherMain = weatherArr?.get("main")?.jsonPrimitive?.contentOrNull
                    )
                }
        }

        if (rows.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent("No catch data available.")))
        }

        // Bucket extractor by dimension.
        val bucketFn: (CatchRow) -> String? = when (dimension) {
            "pressure_trend" -> { r ->
                r.pressureTrend ?: r.pressure?.let {
                    when {
                        it < 1010 -> "falling"
                        it < 1020 -> "stable"
                        else      -> "rising"
                    }
                }
            }
            "wind_direction" -> { r ->
                r.windDeg?.let { compass(it) }
            }
            "hour_of_day" -> { r ->
                r.hour?.let { hourBand(it) }
            }
            "cloud_cover" -> { r ->
                r.cloudPct?.let { cloudBand(it) }
            }
            "air_temp" -> { r ->
                r.airTemp?.let { tempBand(it) }
            }
            "water_temp" -> { r ->
                r.waterTemp?.let { tempBand(it) }
            }
            "weather_main" -> { r -> r.weatherMain?.takeIf { it.isNotBlank() } }
            else -> return@addTool CallToolResult(
                content = listOf(TextContent("Error: unknown dimension '$dimension'. Valid: pressure_trend, wind_direction, hour_of_day, cloud_cover, air_temp, water_temp, weather_main")),
                isError = true
            )
        }

        val tagged = rows.mapNotNull { r -> bucketFn(r)?.let { it to r } }
        if (tagged.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No catches have data for dimension '$dimension'. Try a different dimension."
            )))
        }

        val total = tagged.size
        val byBucket = tagged.groupBy({ it.first }, { it.second })
            .toList()
            .sortedByDescending { it.second.size }

        // Per-species cross-tab so we can compute relative density.
        val allSpecies = tagged.mapNotNull { it.second.species }.toSet()
        val perSpeciesByBucket: Map<String, Map<String, Int>> = allSpecies.associateWith { sp ->
            byBucket.associate { (bucket, list) ->
                bucket to list.count { it.species == sp }
            }
        }

        val text = buildString {
            appendLine("Conditional breakdown by '$dimension' — $total catches with data")
            appendLine()
            byBucket.forEach { (bucket, list) ->
                val pct = (list.size * 100.0 / total).roundToInt()
                val top = list.mapNotNull { it.species }
                    .groupingBy { it }.eachCount().entries
                    .sortedByDescending { it.value }.take(3)
                    .joinToString(", ") { "${it.key} (${it.value})" }
                    .ifEmpty { "—" }
                appendLine("  $bucket — ${list.size} catches ($pct%) — top species: $top")
            }
            appendLine()

            // Relative density: for species with ≥3 total catches, compare bucket
            // share to the species' average share, surfacing the strongest skew.
            val minSampleSize = 3
            val notableSkews = mutableListOf<Triple<String, String, Double>>() // species, bucket, multiplier vs baseline

            for ((sp, byB) in perSpeciesByBucket) {
                val totalForSp = byB.values.sum()
                if (totalForSp < minSampleSize) continue
                val avgShare = totalForSp / byBucket.size.toDouble() / totalForSp // = 1/numBuckets
                byB.forEach { (bucket, count) ->
                    if (count == 0) return@forEach
                    val bucketSize = byBucket.first { it.first == bucket }.second.size
                    val speciesShareInBucket = count.toDouble() / bucketSize
                    val speciesShareOverall = totalForSp.toDouble() / total
                    if (speciesShareOverall <= 0.0) return@forEach
                    val lift = speciesShareInBucket / speciesShareOverall
                    if (lift >= 1.5 || lift <= 0.5) {
                        notableSkews += Triple(sp, bucket, lift)
                    }
                }
            }

            if (notableSkews.isNotEmpty()) {
                appendLine("NOTABLE SKEWS (lift vs baseline; >=1.5x = over-represented, <=0.5x = under-represented):")
                notableSkews
                    .sortedByDescending { kotlin.math.abs(kotlin.math.log(it.third, kotlin.math.E.toFloat().toDouble())) }
                    .take(8)
                    .forEach { (sp, bucket, lift) ->
                        val direction = if (lift >= 1.0) "${"%.1fx".format(lift)} more likely" else "${"%.1fx".format(1.0 / lift)} less likely"
                        appendLine("  $sp under '$bucket': $direction than baseline")
                    }
            } else {
                appendLine("No species shows a notable skew across these buckets (sample sizes may be small).")
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}

private fun compass(deg: Double): String {
    val d = ((deg % 360) + 360) % 360
    return when {
        d < 22.5 || d >= 337.5 -> "N"
        d < 67.5  -> "NE"
        d < 112.5 -> "E"
        d < 157.5 -> "SE"
        d < 202.5 -> "S"
        d < 247.5 -> "SW"
        d < 292.5 -> "W"
        else      -> "NW"
    }
}

private fun hourBand(h: Int): String = when (h) {
    in 0..4   -> "Pre-dawn (0-5)"
    in 5..7   -> "Dawn (5-8)"
    in 8..11  -> "Morning (8-12)"
    in 12..16 -> "Afternoon (12-17)"
    in 17..20 -> "Evening (17-21)"
    else      -> "Night (21-24)"
}

private fun cloudBand(pct: Int): String = when {
    pct < 25 -> "Clear (<25%)"
    pct < 50 -> "Partly cloudy (25-50%)"
    pct < 75 -> "Mostly cloudy (50-75%)"
    else     -> "Overcast (>=75%)"
}

private fun tempBand(t: Double): String = when {
    t < 40 -> "<40°F"
    t < 55 -> "40-55°F"
    t < 70 -> "55-70°F"
    t < 85 -> "70-85°F"
    else   -> ">85°F"
}
