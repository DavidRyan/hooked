package com.hooked.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Five-day weather forecast via OpenWeather's free 3-hour-step forecast endpoint.
 * Surfaces fishing-relevant data: per-day summaries, pressure trend, wind, sky
 * cover, precip chance, sunrise/sunset.
 */
fun Server.registerGetWeatherForecastTool(apiKey: String?) {
    addTool(
        name = "get_weather_forecast",
        description = """
            Five-day weather forecast for a location, broken down per day with a focus on
            fishing-relevant signals: high/low temp, pressure trend (rising/stable/falling),
            wind direction + speed, sky cover, rain/snow probability, storm windows, and
            sunrise/sunset. Optionally filter to a specific date (ISO YYYY-MM-DD) — dates
            outside the 5-day window snap to the nearest available day with a note.
            Use this for "should I fish tomorrow at Petenwell?" or "what's the weekend
            looking like at Lake Mendota?" Combine with personal-history tools to compare
            the forecast to historically productive conditions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "Location name (e.g. 'Lake Mendota WI')")
                }
                putJsonObject("date") {
                    put("type", "string")
                    put("description", "Optional ISO date YYYY-MM-DD to filter to a single day. Omit for the 5-day summary.")
                }
            },
            required = listOf("location")
        )
    ) { request ->
        if (apiKey == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(
                    "Forecast unavailable: OPENWEATHER_API_KEY is not set on this server."
                )), isError = true
            )
        }

        val args = request.params.arguments
        val location = args?.get("location")?.jsonPrimitive?.contentOrNull
            ?: return@addTool CallToolResult(
                content = listOf(TextContent("Error: 'location' is required")), isError = true
            )
        val filterDateStr = args["date"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val filterDate = filterDateStr?.let {
            runCatching { LocalDate.parse(it) }.getOrElse {
                return@addTool CallToolResult(
                    content = listOf(TextContent("Error: bad date '$it'. Use YYYY-MM-DD.")), isError = true
                )
            }
        }

        val encoded = URLEncoder.encode(location, "UTF-8")
        val url = URI.create(
            "https://api.openweathermap.org/data/2.5/forecast?q=$encoded&appid=$apiKey&units=imperial"
        )

        val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
        val response = runCatching {
            client.send(
                HttpRequest.newBuilder(url).GET().timeout(Duration.ofSeconds(15)).build(),
                HttpResponse.BodyHandlers.ofString()
            )
        }.getOrElse { e ->
            return@addTool CallToolResult(
                content = listOf(TextContent("Error fetching forecast: ${e.message}")), isError = true
            )
        }

        when (response.statusCode()) {
            404 -> return@addTool CallToolResult(
                content = listOf(TextContent("Location not found: '$location'. Try a more specific name."))
            )
            200 -> Unit
            else -> return@addTool CallToolResult(
                content = listOf(TextContent("Weather API error (${response.statusCode()})")), isError = true
            )
        }

        val root = Json.parseToJsonElement(response.body()).jsonObject
        val city = root["city"]?.jsonObject
        val tzOffsetSec = city?.get("timezone")?.jsonPrimitive?.intOrNull ?: 0
        val zone = ZoneOffset.ofTotalSeconds(tzOffsetSec)
        val sunrise = city?.get("sunrise")?.jsonPrimitive?.longOrNull
        val sunset = city?.get("sunset")?.jsonPrimitive?.longOrNull
        val cityName = city?.get("name")?.jsonPrimitive?.contentOrNull ?: location
        val country = city?.get("country")?.jsonPrimitive?.contentOrNull

        val slots: List<Slot> = root["list"]?.jsonArray?.mapNotNull { node ->
            val obj = node.jsonObject
            val dt = obj["dt"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val main = obj["main"]?.jsonObject ?: return@mapNotNull null
            val weather = obj["weather"]?.jsonArray?.firstOrNull()?.jsonObject
            val wind = obj["wind"]?.jsonObject
            val clouds = obj["clouds"]?.jsonObject
            Slot(
                time = Instant.ofEpochSecond(dt).atZone(zone),
                temp = main["temp"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                pressure = main["pressure"]?.jsonPrimitive?.intOrNull ?: 0,
                humidity = main["humidity"]?.jsonPrimitive?.intOrNull,
                description = weather?.get("description")?.jsonPrimitive?.contentOrNull,
                mainCategory = weather?.get("main")?.jsonPrimitive?.contentOrNull,
                cloudPct = clouds?.get("all")?.jsonPrimitive?.intOrNull,
                windSpeed = wind?.get("speed")?.jsonPrimitive?.doubleOrNull,
                windDeg = wind?.get("deg")?.jsonPrimitive?.intOrNull,
                popPct = ((obj["pop"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 100).roundToInt(),
                rain3h = obj["rain"]?.jsonObject?.get("3h")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                snow3h = obj["snow"]?.jsonObject?.get("3h")?.jsonPrimitive?.doubleOrNull ?: 0.0
            )
        } ?: emptyList()

        if (slots.isEmpty()) {
            return@addTool CallToolResult(content = listOf(TextContent(
                "No forecast slots returned for '$location'."
            )))
        }

        val groupedByDay: Map<LocalDate, List<Slot>> = slots.groupBy { it.time.toLocalDate() }
        val availableDates = groupedByDay.keys.sorted()

        val daysToShow: List<LocalDate> = if (filterDate != null) {
            val exact = if (filterDate in availableDates) filterDate else availableDates.minByOrNull {
                abs(java.time.temporal.ChronoUnit.DAYS.between(filterDate, it))
            }!!
            listOf(exact)
        } else availableDates

        val text = buildString {
            appendLine("Forecast: $cityName${country?.let { ", $it" } ?: ""}")
            if (filterDate != null && daysToShow.first() != filterDate) {
                appendLine("(Requested $filterDate is outside the 5-day window; showing closest: ${daysToShow.first()})")
            }
            if (sunrise != null && sunset != null && filterDate == null) {
                val sr = Instant.ofEpochSecond(sunrise).atZone(zone)
                val ss = Instant.ofEpochSecond(sunset).atZone(zone)
                appendLine("Today's sunrise/sunset: ${sr.format(timeFmt)} / ${ss.format(timeFmt)}")
            }
            appendLine()

            daysToShow.forEach { day ->
                val daySlots = groupedByDay[day] ?: return@forEach
                appendDayBlock(day, daySlots)
                appendLine()
            }
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}

// --- helpers ---------------------------------------------------------------

private data class Slot(
    val time: ZonedDateTime,
    val temp: Double,
    val pressure: Int,
    val humidity: Int?,
    val description: String?,
    val mainCategory: String?,
    val cloudPct: Int?,
    val windSpeed: Double?,
    val windDeg: Int?,
    val popPct: Int,
    val rain3h: Double,
    val snow3h: Double
)

private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val dayLabelFmt = DateTimeFormatter.ofPattern("EEEE, MMM d")

private fun StringBuilder.appendDayBlock(day: LocalDate, slots: List<Slot>) {
    val high = slots.maxOf { it.temp }
    val low = slots.minOf { it.temp }
    val avgWind = slots.mapNotNull { it.windSpeed }.average().takeIf { !it.isNaN() } ?: 0.0
    val avgClouds = slots.mapNotNull { it.cloudPct }.average().takeIf { !it.isNaN() } ?: 0.0
    val maxPop = slots.maxOf { it.popPct }

    val firstP = slots.first().pressure
    val lastP = slots.last().pressure
    val pressureDelta = lastP - firstP
    val pressureTrend = when {
        pressureDelta >= 4  -> "rising"
        pressureDelta <= -4 -> "falling"
        else                -> "stable"
    }

    // Pick a representative wind direction (slot near midday, fallback to first).
    val midday = slots.firstOrNull { it.time.hour in 11..14 } ?: slots.first()
    val windDirStr = midday.windDeg?.let { compass(it) }
    val midTemp = midday.temp
    val midDesc = midday.description ?: midday.mainCategory ?: "fair"

    appendLine("=== ${dayLabelFmt.format(day)} ===")
    appendLine(
        "  High %.0f°F / Low %.0f°F · Midday %.0f°F, $midDesc".format(high, low, midTemp)
    )
    appendLine(
        "  Wind avg %.1f mph${windDirStr?.let { " (mid-day from $it)" } ?: ""} · ".format(avgWind) +
            "Clouds ${avgClouds.roundToInt()}% · Rain chance ${maxPop}%"
    )
    appendLine(
        "  Pressure: $firstP → $lastP hPa ($pressureTrend over the day)"
    )

    // Storm/precip windows worth flagging
    val stormSlots = slots.filter {
        it.popPct >= 50 || (it.mainCategory?.equals("Thunderstorm", ignoreCase = true) == true)
    }
    if (stormSlots.isNotEmpty()) {
        val window = stormSlots.first().time.format(timeFmt) +
            " – " + stormSlots.last().time.format(timeFmt)
        val why = if (stormSlots.any { it.mainCategory?.equals("Thunderstorm", ignoreCase = true) == true }) {
            "thunderstorms"
        } else {
            "rain ≥50%"
        }
        appendLine("  ⚡ Wet window: $window ($why)")
    }
}

private fun compass(deg: Int): String {
    val d = ((deg % 360) + 360) % 360
    val dirs = listOf("N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW")
    return dirs[((d + 11.25) / 22.5).toInt() % 16]
}
