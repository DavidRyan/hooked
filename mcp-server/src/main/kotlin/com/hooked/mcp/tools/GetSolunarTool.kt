package com.hooked.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Solunar / astronomical info for a date + location. Pure math, no external deps.
 *
 * Returns sunrise/sunset/civil twilight, moon phase + illumination + age, and a
 * solunar score 1-4 based on lunar proximity to new/full and time of year.
 *
 * Solunar theory holds that fish feed most actively around moon transit (overhead),
 * antitransit (underfoot), and near new/full moons. This tool gives the raw data;
 * the LLM is expected to interpret it.
 */
fun Server.registerGetSolunarTool() {
    addTool(
        name = "get_solunar",
        description = """
            Sun and moon info for a date and location: sunrise, sunset, civil twilight,
            moon phase, illumination percentage, lunar age in days, and a 1-4 solunar score
            (4 = best, near new or full moon). Use this for "is tomorrow a good moon?",
            "when does the sun rise at the cabin?", or to flag prime feeding windows.
            Coordinates default to central Wisconsin (43.5, -89.5) if omitted.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("date") {
                    put("type", "string")
                    put("description", "ISO date YYYY-MM-DD. Defaults to today (UTC) if omitted.")
                }
                putJsonObject("latitude") {
                    put("type", "number")
                    put("description", "Latitude in decimal degrees. Defaults to 43.5.")
                }
                putJsonObject("longitude") {
                    put("type", "number")
                    put("description", "Longitude in decimal degrees (negative west). Defaults to -89.5.")
                }
            },
            required = emptyList()
        )
    ) { request ->
        val args = request.params.arguments
        val dateStr = args?.get("date")?.jsonPrimitive?.contentOrNull
        val lat = args?.get("latitude")?.jsonPrimitive?.doubleOrNull ?: 43.5
        val lng = args?.get("longitude")?.jsonPrimitive?.doubleOrNull ?: -89.5

        val date = runCatching { LocalDate.parse(dateStr ?: LocalDate.now().toString()) }
            .getOrElse {
                return@addTool CallToolResult(
                    content = listOf(TextContent("Error: bad date '$dateStr'. Use YYYY-MM-DD.")),
                    isError = true
                )
            }

        val jd = julianDay(date)
        val sun = sunTimes(jd, lat, lng)
        val moon = moonInfo(jd)

        val moonName = phaseName(moon.age)
        val solunarScore = solunarScore(moon.age)

        val text = buildString {
            appendLine("Solunar / sky data for $date at ($lat, $lng)")
            appendLine()
            appendLine("SUN:")
            appendLine("  Sunrise:        ${formatHourUtcLocal(sun.sunrise, lng)}")
            appendLine("  Sunset:         ${formatHourUtcLocal(sun.sunset, lng)}")
            appendLine("  Civil twilight: ${formatHourUtcLocal(sun.civilDawn, lng)} (dawn) / ${formatHourUtcLocal(sun.civilDusk, lng)} (dusk)")
            appendLine("  Day length:     ${"%.1f".format(sun.dayLengthHours)} h")
            appendLine()
            appendLine("MOON:")
            appendLine("  Phase:        $moonName")
            appendLine("  Age:          %.1f days (of 29.5)".format(moon.age))
            appendLine("  Illumination: ${moon.illumination}%")
            appendLine()
            appendLine("SOLUNAR SCORE: $solunarScore / 4")
            appendLine("  (4 = peak: ±3 days from new/full moon)")
            appendLine("  (Major feeding ≈ near moon transit/antitransit; minor ≈ moonrise/set)")
            appendLine("  Without exact lunar transit times: roughly dawn and dusk are reliable feeding")
            appendLine("  windows; full and new moons amplify activity.")
        }

        CallToolResult(content = listOf(TextContent(text)))
    }
}

// --- astronomy helpers ----------------------------------------------------

private fun julianDay(date: LocalDate): Double {
    val y = date.year
    val m = date.monthValue
    val d = date.dayOfMonth.toDouble()
    val (yy, mm) = if (m <= 2) Pair(y - 1, m + 12) else Pair(y, m)
    val a = floor(yy / 100.0)
    val b = 2 - a + floor(a / 4.0)
    return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5
}

private data class SunTimes(val sunrise: Double, val sunset: Double, val civilDawn: Double, val civilDusk: Double, val dayLengthHours: Double)

/**
 * Compute sunrise/sunset using the NOAA-derived "Sunrise Equation" approximation.
 * Returns times in hours UTC (0..24). For null result use NaN.
 */
private fun sunTimes(jd: Double, lat: Double, lngE: Double): SunTimes {
    // Notice: longitude west is negative in the convention we use, but for the math
    // we treat west as positive.
    val n = jd - 2451545.0 + 0.0008
    val jStar = n - (lngE / 360.0)
    val m = ((357.5291 + 0.98560028 * jStar) % 360 + 360) % 360
    val mRad = Math.toRadians(m)
    val c = 1.9148 * sin(mRad) + 0.0200 * sin(2 * mRad) + 0.0003 * sin(3 * mRad)
    val lambda = ((m + c + 180.0 + 102.9372) % 360 + 360) % 360
    val lambdaRad = Math.toRadians(lambda)
    val jTransit = 2451545.0 + jStar + 0.0053 * sin(mRad) - 0.0069 * sin(2 * lambdaRad)
    val delta = asin(sin(lambdaRad) * sin(Math.toRadians(23.4397)))

    fun hourAngle(zenithDeg: Double): Double {
        val zr = Math.toRadians(zenithDeg)
        val latRad = Math.toRadians(lat)
        val arg = (cos(zr) - sin(latRad) * sin(delta)) / (cos(latRad) * cos(delta))
        return if (arg in -1.0..1.0) Math.toDegrees(acos(arg)) else Double.NaN
    }

    val omegaSun = hourAngle(90.833)   // standard sunrise/set (refraction-corrected)
    val omegaCivil = hourAngle(96.0)   // civil twilight: sun 6° below horizon

    fun toLocalHoursUTC(jdEvent: Double): Double {
        val frac = (jdEvent - floor(jdEvent - 0.5)) - 0.5
        val hours = frac * 24.0
        return ((hours % 24) + 24) % 24
    }

    val sunrise = if (omegaSun.isNaN()) Double.NaN else toLocalHoursUTC(jTransit - omegaSun / 360.0)
    val sunset = if (omegaSun.isNaN()) Double.NaN else toLocalHoursUTC(jTransit + omegaSun / 360.0)
    val civilDawn = if (omegaCivil.isNaN()) Double.NaN else toLocalHoursUTC(jTransit - omegaCivil / 360.0)
    val civilDusk = if (omegaCivil.isNaN()) Double.NaN else toLocalHoursUTC(jTransit + omegaCivil / 360.0)
    val dayLength = if (omegaSun.isNaN()) 0.0 else 2 * omegaSun / 15.0

    return SunTimes(sunrise, sunset, civilDawn, civilDusk, dayLength)
}

private data class MoonInfo(val age: Double, val illumination: Int)

private fun moonInfo(jd: Double): MoonInfo {
    val syn = 29.530588853
    val refNewMoon = 2451550.1   // 2000-01-06 known new moon
    val age = ((jd - refNewMoon) % syn + syn) % syn
    val illum = ((1 - cos(2 * PI * age / syn)) * 50).toInt().coerceIn(0, 100)
    return MoonInfo(age, illum)
}

private fun phaseName(age: Double): String = when {
    age < 1.0 || age >= 28.5 -> "New Moon"
    age < 6.5  -> "Waxing Crescent"
    age < 8.5  -> "First Quarter"
    age < 13.5 -> "Waxing Gibbous"
    age < 15.5 -> "Full Moon"
    age < 20.5 -> "Waning Gibbous"
    age < 22.5 -> "Last Quarter"
    else       -> "Waning Crescent"
}

private fun solunarScore(age: Double): Int {
    val syn = 29.530588853
    // Distance to nearest new or full moon (closer = better feeding window).
    val distToNew = minOf(age, syn - age)
    val distToFull = abs(age - syn / 2.0)
    val dist = minOf(distToNew, distToFull)
    return when {
        dist < 1.0 -> 4
        dist < 3.0 -> 3
        dist < 6.0 -> 2
        else       -> 1
    }
}

private fun formatHourUtcLocal(h: Double, lngE: Double): String {
    if (h.isNaN()) return "—"
    // Convert UTC fractional hour to local time using longitude offset.
    val offset = lngE / 15.0
    val local = ((h + offset) % 24 + 24) % 24
    val hh = local.toInt()
    val mm = ((local - hh) * 60).toInt()
    return "%02d:%02d local (%02d:%02dZ)".format(hh, mm, h.toInt(), ((h - h.toInt()) * 60).toInt())
}
