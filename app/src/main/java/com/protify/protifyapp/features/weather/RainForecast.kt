package com.protify.protifyapp.features.weather

import java.time.LocalDateTime

data class RainForecast(
    val time: LocalDateTime,
    val probability: Int
) {

}