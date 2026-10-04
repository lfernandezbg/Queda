package com.luisete.queda.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.ExactQuantityInputParser
import com.luisete.queda.core.domain.inventory.ExactQuantityInputResult
import com.luisete.queda.core.domain.inventory.ManageLocationsUseCase
import com.luisete.queda.core.domain.inventory.ManageStockUseCase
import com.luisete.queda.core.domain.inventory.SelectedStock
import com.luisete.queda.core.domain.inventory.StockWriteResult
import com.luisete.queda.core.model.inventory.FoodType
import com.luisete.queda.core.model.inventory.StockDetails
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class StockForm(
    val name: String = "",
    val locationId: String? = null,
    val foodType: FoodType = FoodType.FOOD,
    val label: String = "",
    val preparedOn: String = "",
    val bestBefore: String = "",
)

data class StockManagementState(
    val form: StockForm = StockForm(),
    val editing: InventoryItemUiModel? = null,
    val selecting: Boolean = false,
    val selected: List<InventoryItemUiModel> = emptyList(),
    val busy: Boolean = false,
    val result: StockWriteResult? = null,
    val consumeAmounts: Map<String, String> = emptyMap(),
)

@HiltViewModel
@Suppress("TooManyFunctions")
class StockManagementViewModel
    @Inject
    constructor(
        private val manage: ManageStockUseCase,
        private val manageLocations: ManageLocationsUseCase,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val mutableState =
            MutableStateFlow(
                StockManagementState(
                    form =
                        StockForm(
                            name = savedState["name"] ?: "",
                            locationId = savedState["location"],
                            foodType = FoodType.valueOf(savedState["foodType"] ?: FoodType.FOOD.name),
                            label = savedState["label"] ?: "",
                            preparedOn = savedState["prepared"] ?: "",
                            bestBefore = savedState["expiry"] ?: "",
                        ),
                ),
            )
        val state = mutableState.asStateFlow()
        val locations =
            manageLocations.observe().stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(TIMEOUT),
                emptyList(),
            )
        private val gate = AtomicBoolean(false)

        fun form(form: StockForm) {
            if (gate.get()) return
            savedState["name"] = form.name
            savedState["location"] = form.locationId
            savedState["foodType"] = form.foodType.name
            savedState["label"] = form.label
            savedState["prepared"] = form.preparedOn
            savedState["expiry"] = form.bestBefore
            mutableState.update { it.copy(form = form, result = null) }
        }

        fun edit(item: InventoryItemUiModel) {
            if (gate.get()) return
            val details = item.details
            form(
                StockForm(
                    name = item.name,
                    locationId = details.locationId,
                    foodType = details.foodType,
                    label = details.label.orEmpty(),
                    preparedOn = details.preparedOn?.toString().orEmpty(),
                    bestBefore = details.bestBefore?.toString().orEmpty(),
                ),
            )
            mutableState.update { it.copy(editing = item) }
        }

        fun dismiss() {
            if (!gate.get()) mutableState.update { it.copy(editing = null, result = null) }
        }

        fun saveEdit() {
            val current = mutableState.value
            val item = current.editing ?: return
            val details = current.form.toDetails()
            if (details == null) {
                mutableState.update { it.copy(result = StockWriteResult.INVALID) }
                return
            }
            perform { manage.edit(item.id, item.details, details, item.name, current.form.name) }
        }

        fun submitAdd(
            name: String,
            quantity: String,
            unit: MeasurementUnit,
            saveNormal: (StockDetails) -> Unit,
        ) {
            val details = mutableState.value.form.toDetails()
            if (details == null) {
                mutableState.update { it.copy(result = StockWriteResult.INVALID) }
                return
            }
            if (details.foodType == FoodType.FOOD) {
                saveNormal(details.copy(label = null))
            } else {
                perform { manage.addPrepared(name, quantity, unit, details.copy(label = null)) }
            }
        }

        fun selectionMode() {
            if (!gate.get()) {
                mutableState.update {
                    it.copy(
                        selecting = !it.selecting,
                        selected = emptyList(),
                        result = null,
                    )
                }
            }
        }

        fun toggle(item: InventoryItemUiModel) {
            if (gate.get()) return
            mutableState.update { state ->
                val selected = state.selected
                when {
                    selected.any { it.id == item.id } -> state.copy(selected = selected.filterNot { it.id == item.id })
                    selected.size >= ManageStockUseCase.MAX_SELECTION -> state.copy(result = StockWriteResult.INVALID)
                    else -> state.copy(selecting = true, selected = selected + item, result = null)
                }
            }
        }

        fun consume(items: List<InventoryItemUiModel> = mutableState.value.selected) {
            val amounts = mutableState.value.consumeAmounts
            val snapshot =
                items.map { item ->
                    val input = amounts[item.id].orEmpty().trim()
                    val requested =
                        if (input.isBlank()) {
                            null
                        } else {
                            val exact = item.quantity as? ExactQuantity
                            val parsed = exact?.let { ExactQuantityInputParser.parse(input, it.unit) }
                            if (parsed !is ExactQuantityInputResult.Success) {
                                mutableState.update { it.copy(result = StockWriteResult.INVALID) }
                                return
                            }
                            parsed.quantity
                        }
                    SelectedStock(item.id, item.quantity, requested)
                }
            perform { manage.consume(snapshot) }
        }

        fun consumeAmount(
            id: String,
            amount: String,
        ) {
            if (!gate.get()) {
                mutableState.update { it.copy(consumeAmounts = it.consumeAmounts + (id to amount), result = null) }
            }
        }

        fun move(locationId: String?) {
            val snapshot = mutableState.value.selected.map { SelectedStock(it.id, it.quantity) }
            perform { manage.move(snapshot, locationId) }
        }

        private fun perform(action: suspend () -> StockWriteResult) {
            if (!gate.compareAndSet(false, true)) return
            mutableState.update { it.copy(busy = true, result = null) }
            viewModelScope.launch {
                try {
                    val result = action()
                    mutableState.update {
                        it.copy(
                            result = result,
                            editing = if (result == StockWriteResult.SAVED) null else it.editing,
                            selected = if (result == StockWriteResult.SAVED) emptyList() else it.selected,
                            consumeAmounts = if (result == StockWriteResult.SAVED) emptyMap() else it.consumeAmounts,
                        )
                    }
                } finally {
                    gate.set(false)
                    mutableState.update { it.copy(busy = false) }
                }
            }
        }

        companion object {
            private const val TIMEOUT = 5000L
        }
    }

private fun StockForm.toDetails(): StockDetails? =
    try {
        StockDetails(
            this.locationId,
            this.foodType,
            this.label.trim().takeIf(String::isNotEmpty),
            this.preparedOn.takeIf(String::isNotBlank)?.let(LocalDate::parse),
            this.bestBefore.takeIf(String::isNotBlank)?.let(LocalDate::parse),
        ).takeIf { it.isValid(LocalDate.now()) }
    } catch (_: DateTimeParseException) {
        null
    }
