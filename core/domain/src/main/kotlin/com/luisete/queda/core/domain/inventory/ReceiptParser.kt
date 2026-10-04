package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.ReceiptDraft
import com.luisete.queda.core.model.inventory.ReceiptLine
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object ReceiptParser {
    private const val MAX_LINES = 120
    private const val HEX_MASK = 0xff
    private const val MIN_NAME = 2
    private const val MAX_NAME = 100
    private val spaces = Regex("\\s+")
    private val prices = Regex("(?:\\s+[0-9]+[,.][0-9]{2}(?:\\s*€)?)+$")
    private val count = Regex("^([1-9][0-9]{0,2})\\s+[xX]?\\s*(.+)$")
    private val metadata =
        Regex(
            "^(TOTAL|SUBTOTAL|IVA|BASE|CUOTA|EFECTIVO|TARJETA|CAMBIO|VISA|MASTERCARD|" +
                "MERCADONA|LIDL|ALDI|CARREFOUR|TICKET|FACTURA|FECHA|HORA|CAJA|CAJERO|" +
                "CIF|NIF|TEL|WWW|GRACIAS|DESCUENTO|AHORRO|IMPORTE|PAGO|AUTORIZACION)\\b",
        )

    fun parse(text: String): ReceiptDraft {
        val normalized =
            text.lineSequence().map { it.trim().replace(spaces, " ") }
                .filter { it.isNotEmpty() }.take(MAX_LINES).toList()
        val fingerprint =
            MessageDigest.getInstance("SHA-256")
                .digest(normalized.joinToString("\n").uppercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and HEX_MASK) }
        val lines =
            normalized.filter(::isCandidate).map { source ->
                val withoutPrices = source.replace(prices, "")
                val match = count.matchEntire(withoutPrices)
                ReceiptLine(
                    id = UUID.randomUUID().toString(),
                    source = source,
                    name = (match?.groupValues?.get(2) ?: withoutPrices).trim(),
                    quantity = match?.groupValues?.get(1) ?: "1",
                )
            }
        return ReceiptDraft(UUID.randomUUID().toString(), fingerprint, lines)
    }

    private fun isCandidate(line: String): Boolean =
        line.any(Char::isLetter) && !metadata.containsMatchIn(line.uppercase(Locale.ROOT)) &&
            line.replace(prices, "").length in MIN_NAME..MAX_NAME
}
