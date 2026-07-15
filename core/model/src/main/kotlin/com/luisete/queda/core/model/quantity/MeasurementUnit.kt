package com.luisete.queda.core.model.quantity

enum class MeasurementUnit(
    val dimension: QuantityDimension,
) {
    UNIT(QuantityDimension.COUNT),
    GRAM(QuantityDimension.MASS),
    KILOGRAM(QuantityDimension.MASS),
    MILLILITER(QuantityDimension.VOLUME),
    LITER(QuantityDimension.VOLUME),
}
