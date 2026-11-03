package com.salud.personal.domain.sleep

import com.salud.personal.data.local.entity.SleepSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class SleepDebtCalculatorTest {

    private fun createSleep(date: LocalDate, minutes: Int): SleepSessionEntity {
        return SleepSessionEntity(
            date = date,
            startTime = Instant.EPOCH,
            endTime = Instant.EPOCH.plusSeconds((minutes * 60).toLong()),
            deepSleepMinutes = minutes / 4,
            lightSleepMinutes = minutes / 2,
            remSleepMinutes = minutes / 4,
            awakeMinutes = 15,
            totalSleepMinutes = minutes,
            sleepEfficiencyScore = 88,
            sourceDevice = "GTR 3"
        )
    }

    @Test
    fun calculate7DayDebt_withNoSleepHistory_returnsZeroDebt() {
        val result = SleepDebtCalculator.calculate7DayDebt(
            recentSleeps = emptyList(),
            baselineNeedHours = 8.0
        )

        assertEquals(0, result.totalDebtMinutes)
        assertEquals(0, result.dailyCompensationMinutes)
        assertEquals(SleepDebtStatus.NONE, result.status)
        assertEquals(0, result.analyzedNights)
    }

    @Test
    fun calculate7DayDebt_withExactSleepMeetingNeed_hasZeroDebt() {
        val today = LocalDate.of(2026, 11, 3)
        // 7 nights of 8 hours (480 minutes)
        val sleeps = (1..7).map { day ->
            createSleep(today.minusDays(day.toLong()), 480)
        }

        val result = SleepDebtCalculator.calculate7DayDebt(
            recentSleeps = sleeps,
            baselineNeedHours = 8.0
        )

        assertEquals(0, result.totalDebtMinutes)
        assertEquals(0, result.dailyCompensationMinutes)
        assertEquals(SleepDebtStatus.NONE, result.status)
        assertEquals(7, result.analyzedNights)
    }

    @Test
    fun calculate7DayDebt_withSleepDeficit_accumulatesDebtAndCapsDailyCompensation() {
        val today = LocalDate.of(2026, 11, 3)
        // 4 nights with 6 hours each (360 min) when need is 8h (480 min) -> deficit is 120 min/night = 480 min total
        val sleeps = (1..4).map { day ->
            createSleep(today.minusDays(day.toLong()), 360)
        }

        val result = SleepDebtCalculator.calculate7DayDebt(
            recentSleeps = sleeps,
            baselineNeedHours = 8.0
        )

        // Total deficit = 4 nights * 120 min = 480 min
        assertEquals(480, result.totalDebtMinutes)
        // Max compensation per night is capped to 45 min to prevent circadian disruption
        assertEquals(SleepDebtCalculator.MAX_DAILY_COMPENSATION_MINUTES, result.dailyCompensationMinutes)
        assertEquals(SleepDebtStatus.HIGH, result.status)
        // Adjusted need = 8.0h + 45 min (0.75h) = 8.75h
        assertEquals(8.75, result.adjustedBedtimeNeedHours, 0.01)
    }

    @Test
    fun calculate7DayDebt_withSmallDebt_compensatesProportionally() {
        val today = LocalDate.of(2026, 11, 3)
        // 1 night with 7.5 hours (450 min) when need is 8h (480 min) -> deficit = 30 min
        val sleeps = listOf(createSleep(today.minusDays(1), 450))

        val result = SleepDebtCalculator.calculate7DayDebt(
            recentSleeps = sleeps,
            baselineNeedHours = 8.0
        )

        assertEquals(30, result.totalDebtMinutes)
        // Half of small debt distributed or capped (at least 15m)
        assertTrue(result.dailyCompensationMinutes in 15..30)
        assertEquals(SleepDebtStatus.LOW, result.status)
    }

    @Test
    fun calculate7DayDebt_withExtraRecentTrimp_addsExtraSleepNeed() {
        val today = LocalDate.of(2026, 11, 3)
        val sleeps = listOf(createSleep(today.minusDays(1), 480))

        val result = SleepDebtCalculator.calculate7DayDebt(
            recentSleeps = sleeps,
            baselineNeedHours = 7.75,
            recentTrimp = 160.0 // High strain adds 0.50h (30 min)
        )

        assertEquals(0.50, result.extraNeedTrimpHours, 0.01)
        assertEquals(8.25, result.totalSleepNeedHours, 0.01)
    }
}
