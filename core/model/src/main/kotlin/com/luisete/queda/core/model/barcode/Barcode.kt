package com.luisete.queda.core.model.barcode

data class Barcode private constructor(val value: String) {
    companion object {
        private val EAN_UPC_ITF_REGEX = Regex("^\\d+$")
        private val SUPPORTED_LENGTHS = setOf(8, 12, 13, 14)
        private const val WEIGHT_3 = 3
        private const val WEIGHT_1 = 1
        private const val MOD_10 = 10

        fun create(raw: String): BarcodeCreationResult {
            val normalized = raw.trim()
            return when {
                normalized.isBlank() -> BarcodeCreationResult.Blank
                !EAN_UPC_ITF_REGEX.matches(normalized) -> BarcodeCreationResult.NonDigit
                normalized.length !in SUPPORTED_LENGTHS -> BarcodeCreationResult.UnsupportedFormat
                !validateCheckDigit(normalized) -> BarcodeCreationResult.InvalidCheckDigit
                else -> BarcodeCreationResult.Success(Barcode(normalized))
            }
        }

        private fun validateCheckDigit(barcode: String): Boolean {
            val length = barcode.length
            val checkDigit = barcode.last().digitToInt()
            val dataDigits = barcode.substring(0, length - 1)

            var sum = 0
            for (i in dataDigits.indices.reversed()) {
                val digit = dataDigits[i].digitToInt()
                val positionFromRight = length - i
                val weight = if (positionFromRight % 2 == 0) WEIGHT_3 else WEIGHT_1
                sum += digit * weight
            }

            val remainder = sum % MOD_10
            val expectedCheckDigit = if (remainder == 0) 0 else MOD_10 - remainder

            return checkDigit == expectedCheckDigit
        }
    }
}

sealed interface BarcodeCreationResult {
    data class Success(val barcode: Barcode) : BarcodeCreationResult

    data object Blank : BarcodeCreationResult

    data object NonDigit : BarcodeCreationResult

    data object UnsupportedFormat : BarcodeCreationResult

    data object InvalidCheckDigit : BarcodeCreationResult
}
