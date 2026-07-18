package com.luisete.queda.core.model.product

import java.util.Locale

class ProductName private constructor(
    val displayValue: String,
    val normalizedKey: String,
) {
    companion object {
        private const val MAX_LENGTH = 80

        @Suppress("ReturnCount", "ComplexMethod")
        fun create(rawName: String): ProductNameCreationResult {
            if (rawName.any { it.isISOControl() || it.category == CharCategory.FORMAT }) {
                return ProductNameCreationResult.ContainsForbiddenCharacter
            }

            var temp = rawName
            while (temp.startsWith(' ')) {
                temp = temp.substring(1)
            }
            while (temp.endsWith(' ')) {
                temp = temp.substring(0, temp.length - 1)
            }

            if (temp.isEmpty()) return ProductNameCreationResult.Blank

            val collapsedBuilder = StringBuilder()
            var lastWasSpace = false
            for (char in temp) {
                if (char == ' ') {
                    if (!lastWasSpace) {
                        collapsedBuilder.append(char)
                    }
                    lastWasSpace = true
                } else {
                    collapsedBuilder.append(char)
                    lastWasSpace = false
                }
            }
            val collapsed = collapsedBuilder.toString()

            if (collapsed.length > MAX_LENGTH) return ProductNameCreationResult.TooLong

            val normalized = collapsed.lowercase(Locale.ROOT)

            return ProductNameCreationResult.Success(
                ProductName(
                    displayValue = collapsed,
                    normalizedKey = normalized,
                ),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProductName
        return normalizedKey == other.normalizedKey
    }

    override fun hashCode(): Int = normalizedKey.hashCode()

    override fun toString(): String = displayValue
}

sealed interface ProductNameCreationResult {
    data class Success(val productName: ProductName) : ProductNameCreationResult

    data object Blank : ProductNameCreationResult

    data object TooLong : ProductNameCreationResult

    data object ContainsForbiddenCharacter : ProductNameCreationResult
}
