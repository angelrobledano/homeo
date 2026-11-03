package com.salud.personal.domain.heartrate

import com.salud.personal.data.local.entity.HeartRateSampleEntity
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

data class HeartRateZonesResult(
    val minBpm: Int,
    val avgBpm: Int,
    val maxBpm: Int,
    val totalSamples: Int,
    val zone1Pct: Int, // < 60% FC max
    val zone2Pct: Int, // 60 - 70%
    val zone3Pct: Int, // 70 - 80%
    val zone4Pct: Int, // 80 - 90%
    val zone5Pct: Int, // > 90%
    val hourlyTimeline: List<HourlyHeartRatePoint> = emptyList()
)

data class HourlyHeartRatePoint(
    val hour: Int, // 0..23
    val avgBpm: Int,
    val minBpm: Int,
    val maxBpm: Int
)

object HeartRateZonesCalculator {

    fun calculateZones(
        samples: List<HeartRateSampleEntity>,
        maxHr: Int = 190
    ): HeartRateZonesResult {
        if (samples.isEmpty()) {
            return HeartRateZonesResult(
                minBpm = 0,
                avgBpm = 0,
                maxBpm = 0,
                totalSamples = 0,
                zone1Pct = 0,
                zone2Pct = 0,
                zone3Pct = 0,
                zone4Pct = 0,
                zone5Pct = 0
            )
        }

        val bpms = samples.map { it.bpm }
        val minBpm = bpms.minOrNull() ?: 0
        val maxBpm = bpms.maxOrNull() ?: 0
        val avgBpm = bpms.average().roundToInt()

        // Thresholds based on maxHr
        val z1Limit = (maxHr * 0.60).toInt()
        val z2Limit = (maxHr * 0.70).toInt()
        val z3Limit = (maxHr * 0.80).toInt()
        val z4Limit = (maxHr * 0.90).toInt()

        var z1 = 0
        var z2 = 0
        var z3 = 0
        var z4 = 0
        var z5 = 0

        for (bpm in bpms) {
            when {
                bpm < z1Limit -> z1++
                bpm < z2Limit -> z2++
                bpm < z3Limit -> z3++
                bpm < z4Limit -> z4++
                else -> z5++
            }
        }

        val total = samples.size
        val z1Pct = ((z1.toDouble() / total) * 100).roundToInt()
        val z2Pct = ((z2.toDouble() / total) * 100).roundToInt()
        val z3Pct = ((z3.toDouble() / total) * 100).roundToInt()
        val z4Pct = ((z4.toDouble() / total) * 100).roundToInt()
        val z5Pct = ((z5.toDouble() / total) * 100).roundToInt()

        // Hourly aggregation
        val zoneId = ZoneId.systemDefault()
        val hourlyMap = samples.groupBy { sample ->
            Instant.ofEpochMilli(sample.timestamp).atZone(zoneId).hour
        }

        val hourlyTimeline = (0..23).mapNotNull { hour ->
            val list = hourlyMap[hour]
            if (list != null && list.isNotEmpty()) {
                val hBpms = list.map { it.bpm }
                HourlyHeartRatePoint(
                    hour = hour,
                    avgBpm = hBpms.average().roundToInt(),
                    minBpm = hBpms.minOrNull() ?: 0,
                    maxBpm = hBpms.maxOrNull() ?: 0
                )
            } else null
        }

        return HeartRateZonesResult(
            minBpm = minBpm,
            avgBpm = avgBpm,
            maxBpm = maxBpm,
            totalSamples = total,
            zone1Pct = z1Pct,
            zone2Pct = z2Pct,
            zone3Pct = z3Pct,
            zone4Pct = z4Pct,
            zone5Pct = z5Pct,
            hourlyTimeline = hourlyTimeline
        )
    }
}
