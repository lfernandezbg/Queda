package com.luisete.queda.core.model.product

import com.luisete.queda.core.model.barcode.Barcode
import com.luisete.queda.core.model.id.HouseholdId
import com.luisete.queda.core.model.id.ProductId

data class Product(
    val id: ProductId,
    val householdId: HouseholdId,
    val name: ProductName,
    val barcode: Barcode? = null,
)
