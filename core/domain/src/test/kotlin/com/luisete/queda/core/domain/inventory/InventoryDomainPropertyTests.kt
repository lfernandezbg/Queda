package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.id.ProductId
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.testing.FakeCurrentHouseholdIdProvider
import com.luisete.queda.core.testing.FakeInventoryRepository
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalKotest::class)
class InventoryDomainPropertyTests {
    private val config = PropTestConfig(iterations = 1000, seed = 20260716L)

    private val validNameArb: Arb<String> =
        Arb.string(1, 40).map { it.replace(Regex("[^a-zA-Z0-9]"), "A") }

    private val amountArb: Arb<BigDecimal> =
        arbitrary {
            val unscaled = Arb.int(1, 1_000_000).bind().toLong()
            val scale = Arb.int(0, 3).bind()
            BigDecimal.valueOf(unscaled, scale)
        }

    @Test
    fun productNameNormalizationIsIdempotent() =
        runTest {
            checkAll(config, validNameArb) { name ->
                val res1 = ProductName.create(name).successValue()
                val res2 = ProductName.create(res1.displayValue).successValue()
                res1.normalizedKey shouldBe res2.normalizedKey
            }
        }

    @Test
    fun caseVariantsProduceSameNormalizedKey() =
        runTest {
            checkAll(config, validNameArb) { name ->
                val upper = name.uppercase(Locale.ROOT)
                val lower = name.lowercase(Locale.ROOT)
                val res1 = ProductName.create(upper).successValue()
                val res2 = ProductName.create(lower).successValue()
                res1.normalizedKey shouldBe res2.normalizedKey
            }
        }

    @Test
    fun spacingVariantsProduceSameNormalizedKey() =
        runTest {
            checkAll(config, validNameArb) { name ->
                val spaced = "  $name   "
                val res1 = ProductName.create(name).successValue()
                val res2 = ProductName.create(spaced).successValue()
                res1.normalizedKey shouldBe res2.normalizedKey
            }
        }

    @Test
    fun dotAndCommaProduceEquivalentExactQuantity() =
        runTest {
            checkAll(config, amountArb) { amount ->
                val dot = amount.toPlainString().replace(',', '.')
                val comma = amount.toPlainString().replace('.', ',')
                val res1 = ExactQuantityInputParser.parse(dot, MeasurementUnit.UNIT).successValue()
                val res2 = ExactQuantityInputParser.parse(comma, MeasurementUnit.UNIT).successValue()
                res1.amount.compareTo(res2.amount) shouldBe 0
            }
        }

    @Test
    fun validQuantityInputNeverLosesPrecision() =
        runTest {
            checkAll(config, amountArb) { amount ->
                val raw = amount.toPlainString()
                val res = ExactQuantityInputParser.parse(raw, MeasurementUnit.UNIT).successValue()
                res.amount.compareTo(amount) shouldBe 0
            }
        }

    @Test
    fun successfulAddPreservesSelectedUnit() =
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            checkAll(config, validNameArb, amountArb, Arb.enum<MeasurementUnit>()) { name, amount, unit ->
                val res = useCase(name, amount.toPlainString(), unit).addedItem()
                val quantity = res.stockItem.quantity as ExactQuantity
                quantity.unit shouldBe unit
            }
        }

    @Test
    fun successfulAddPreservesExactAmount() =
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            checkAll(config, validNameArb, amountArb) { name, amount ->
                val res = useCase(name, amount.toPlainString(), MeasurementUnit.UNIT).addedItem()
                val quantity = res.stockItem.quantity as ExactQuantity
                quantity.amount.compareTo(amount) shouldBe 0
            }
        }

    @Test
    fun generatedIdentifiersRemainDistinctAcrossAdds() =
        runTest {
            val repository = FakeInventoryRepository()
            val useCase = AddExactInventoryItemUseCase(repository, FakeCurrentHouseholdIdProvider())
            val pIds = mutableSetOf<ProductId>()
            val sIds = mutableSetOf<StockItemId>()
            checkAll(config, validNameArb) { name ->
                val res = useCase(name, "1", MeasurementUnit.UNIT).addedItem()
                pIds.add(res.product.id) shouldBe true
                sIds.add(res.stockItem.id) shouldBe true
            }
        }

    private fun ProductNameCreationResult.successValue(): ProductName =
        when (this) {
            is ProductNameCreationResult.Success -> productName
            else -> throw AssertionError("Expected success, got $this")
        }

    private fun ExactQuantityInputResult.successValue(): com.luisete.queda.core.model.quantity.ExactQuantity =
        when (this) {
            is ExactQuantityInputResult.Success -> quantity
            else -> throw AssertionError("Expected success, got $this")
        }

    private fun AddExactInventoryItemResult.addedItem(): com.luisete.queda.core.model.inventory.InventoryItem =
        when (this) {
            is AddExactInventoryItemResult.Added -> inventoryItem
            else -> throw AssertionError("Expected Added, got $this")
        }
}
