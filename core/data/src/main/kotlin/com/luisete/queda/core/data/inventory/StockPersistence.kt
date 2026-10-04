package com.luisete.queda.core.data.inventory

import com.luisete.queda.core.database.StockItemEntity
import com.luisete.queda.core.model.inventory.FoodType
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import com.luisete.queda.core.model.quantity.StockQuantity
import java.time.LocalDate

internal fun StockItemEntity.details(): StockDetails =
    StockDetails(
        locationId,
        FoodType.valueOf(foodType),
        label,
        preparedOn?.let(LocalDate::parse),
        bestBefore?.let(LocalDate::parse),
    )

internal fun StockItemEntity.withDetails(details: StockDetails): StockItemEntity =
    copy(
        locationId = details.locationId,
        foodType = details.foodType.name,
        label = details.label,
        preparedOn = details.preparedOn?.toString(),
        bestBefore = details.bestBefore?.toString(),
    )

internal fun StockItemEntity.quantity(): StockQuantity =
    if (trackingMode == "EXACT") {
        ExactQuantity.of(checkNotNull(quantityAmount), MeasurementUnit.valueOf(checkNotNull(quantityUnit)))
    } else {
        check(trackingMode == "PRESENCE")
        PresenceQuantity(checkNotNull(isPresent))
    }
