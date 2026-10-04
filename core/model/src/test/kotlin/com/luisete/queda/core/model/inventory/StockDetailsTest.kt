package com.luisete.queda.core.model.inventory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StockDetailsTest {
    private val today = LocalDate.of(2026, 10, 3)

    @Test
    fun preparedMealsCanHaveIndependentStorageAndDates() {
        assertTrue(StockDetails("fridge", FoodType.PREPARED, "Lentejas", today, today.plusDays(2)).isValid(today))
        assertTrue(
            StockDetails(
                "freezer",
                FoodType.PREPARED,
                "Lentejas",
                today.minusDays(2),
                today.plusDays(30),
            ).isValid(today),
        )
    }

    @Test
    fun futurePreparationOrExpiryBeforePreparationIsRejected() {
        assertFalse(StockDetails(foodType = FoodType.PREPARED, preparedOn = today.plusDays(1)).isValid(today))
        assertFalse(
            StockDetails(
                foodType = FoodType.PREPARED,
                preparedOn = today,
                bestBefore = today.minusDays(1),
            ).isValid(today),
        )
        assertFalse(StockDetails(preparedOn = today).isValid(today))
    }
}
