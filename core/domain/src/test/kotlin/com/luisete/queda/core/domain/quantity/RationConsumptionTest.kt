package com.luisete.queda.core.domain.quantity

import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class RationConsumptionTest {
    @Test
    fun twoRationsCanBeConsumedFromOnePreparedLot() {
        val available = ExactQuantity.of("3", MeasurementUnit.RATION)
        val eaten = ExactQuantity.of("2", MeasurementUnit.RATION)
        assertEquals(
            ExactQuantity.of("1", MeasurementUnit.RATION),
            QuantityOperations.consume(available, eaten).successValue(),
        )
    }

    @Test
    fun rationsCannotBeSilentlyConvertedToUnitsOrMass() {
        val portion = ExactQuantity.of("1", MeasurementUnit.RATION)
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.convert(portion, MeasurementUnit.UNIT).failureError(),
        )
        assertEquals(
            DomainError.IncompatibleQuantityDimensions,
            QuantityOperations.convert(portion, MeasurementUnit.GRAM).failureError(),
        )
    }
}
