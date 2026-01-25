package com.hooked.catches.presentation.model

data class WeatherUi(
    val windDirection: Float,
    val windSpeed: Float,
    val weatherText: String
) {
    companion object {
        fun fromMap(weather: Map<String, String?>): WeatherUi {
            val weatherText = weather
                .filterValues { !it.isNullOrBlank() }
                .entries
                .joinToString(", ") { "${it.key}: ${it.value}" }

            val windDirection = weather["wind_direction"]?.toFloatOrNull() ?: 0f
            val windSpeed = weather["wind_speed"]?.toFloatOrNull() ?: 0f

            return WeatherUi(
                windDirection = windDirection,
                windSpeed = windSpeed,
                weatherText = weatherText
            )
        }
    }
}
