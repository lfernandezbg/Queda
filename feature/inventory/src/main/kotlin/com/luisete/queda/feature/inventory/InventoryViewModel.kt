package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.ConsumeExactQuantityUseCase
import com.luisete.queda.core.domain.inventory.CorrectExactQuantityUseCase
import com.luisete.queda.core.domain.inventory.ExactQuantityInputParser
import com.luisete.queda.core.domain.inventory.ExactQuantityInputResult
import com.luisete.queda.core.domain.inventory.GetConsumePreviewUseCase
import com.luisete.queda.core.domain.inventory.GetCorrectPreviewUseCase
import com.luisete.queda.core.domain.inventory.ObserveExactInventoryItemsUseCase
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.inventory.SetPresenceUseCase
import com.luisete.queda.core.domain.result.DomainError
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.ExactQuantity
import com.luisete.queda.core.model.quantity.MeasurementUnit
import com.luisete.queda.core.model.quantity.PresenceQuantity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@Suppress("TooManyFunctions")
class InventoryViewModel
    @Inject
    constructor(
        private val observeExactInventoryItemsUseCase: ObserveExactInventoryItemsUseCase,
        private val consumeExactQuantityUseCase: ConsumeExactQuantityUseCase,
        private val correctExactQuantityUseCase: CorrectExactQuantityUseCase,
        private val getConsumePreviewUseCase: GetConsumePreviewUseCase,
        private val getCorrectPreviewUseCase: GetCorrectPreviewUseCase,
        private val setPresenceUseCase: SetPresenceUseCase,
    ) : ViewModel() {
        private val refreshTrigger = MutableStateFlow(0)
        private val quantityActionState = MutableStateFlow<QuantityActionUiState>(QuantityActionUiState.Closed)
        private val presenceActionState = MutableStateFlow<PresenceActionUiState>(PresenceActionUiState.Closed)

        val uiState: StateFlow<InventoryUiState> =
            combine(
                refreshTrigger.flatMapLatest {
                    observeExactInventoryItemsUseCase()
                        .map { items ->
                            if (items.isEmpty()) {
                                InventoryUiState.Empty
                            } else {
                                InventoryUiState.Content(items.map { it.toUiModel() })
                            }
                        }
                        .onStart { emit(InventoryUiState.Loading) }
                        .catch { e ->
                            if (e is CancellationException) throw e
                            emit(InventoryUiState.Error)
                        }
                },
                quantityActionState,
                presenceActionState,
            ) { baseState, qActionState, pActionState ->
                if (baseState is InventoryUiState.Content) {
                    baseState.copy(
                        quantityAction = qActionState,
                        presenceAction = pActionState,
                    )
                } else {
                    baseState
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = InventoryUiState.Loading,
            )

        fun retry() {
            refreshTrigger.update { it + 1 }
        }

        fun onItemClick(item: InventoryItemUiModel) {
            if (item.quantity is PresenceQuantity) {
                presenceActionState.value =
                    PresenceActionUiState.Managing(
                        item = item,
                        isPresent = item.quantity.isPresent,
                    )
            } else {
                quantityActionState.value = QuantityActionUiState.ActionSelection(item)
            }
        }

        fun selectItemById(itemId: String) {
            viewModelScope.launch {
                uiState
                    .filterIsInstance<InventoryUiState.Content>()
                    .mapNotNull { content ->
                        content.items.find { it.id == itemId }
                    }
                    .first()
                    .let { item ->
                        onItemClick(item)
                    }
            }
        }

        fun onDismissSheet() {
            if (isSubmitting()) return
            quantityActionState.value = QuantityActionUiState.Closed
            presenceActionState.value = PresenceActionUiState.Closed
        }

        fun onSelectConsume() {
            val current = quantityActionState.value as? QuantityActionUiState.ActionSelection ?: return
            val quantity = current.item.quantity as? ExactQuantity ?: return
            quantityActionState.value =
                QuantityActionUiState.ConsumeEditing(
                    item = current.item,
                    selectedUnit = quantity.unit,
                )
        }

        fun onSelectCorrect() {
            val current = quantityActionState.value as? QuantityActionUiState.ActionSelection ?: return
            val quantity = current.item.quantity as? ExactQuantity ?: return
            quantityActionState.value =
                QuantityActionUiState.CorrectEditing(
                    item = current.item,
                    selectedUnit = quantity.unit,
                )
        }

        fun onAmountChange(amount: String) {
            quantityActionState.update { current ->
                when (current) {
                    is QuantityActionUiState.ConsumeEditing -> {
                        val next = current.copy(amountInput = amount, error = null)
                        next.copy(preview = calculateConsumePreview(next))
                    }

                    is QuantityActionUiState.CorrectEditing -> {
                        val next = current.copy(amountInput = amount, error = null)
                        next.copy(preview = calculateCorrectPreview(next))
                    }

                    else -> current
                }
            }
        }

        fun onUnitChange(unit: MeasurementUnit) {
            quantityActionState.update { current ->
                when (current) {
                    is QuantityActionUiState.ConsumeEditing -> {
                        val next = current.copy(selectedUnit = unit, error = null)
                        next.copy(preview = calculateConsumePreview(next))
                    }

                    is QuantityActionUiState.CorrectEditing -> {
                        val next = current.copy(selectedUnit = unit, error = null)
                        next.copy(preview = calculateCorrectPreview(next))
                    }

                    else -> current
                }
            }
        }

        fun onConfirm() {
            val currentQ = quantityActionState.value
            if (currentQ is QuantityActionUiState.ConsumeEditing) {
                confirmConsume(currentQ)
            } else if (currentQ is QuantityActionUiState.CorrectEditing) {
                confirmCorrect(currentQ)
            }
        }

        fun onTogglePresence(isPresent: Boolean) {
            val current = presenceActionState.value as? PresenceActionUiState.Managing ?: return
            if (current.isSubmitting) return

            presenceActionState.value =
                current.copy(
                    isPresent = isPresent,
                    isSubmitting = true,
                    error = false,
                )

            viewModelScope.launch {
                val result =
                    setPresenceUseCase(
                        stockItemId = StockItemId.from(current.item.id),
                        isPresent = isPresent,
                    )
                when (result) {
                    is QuantityMutationResult.Success -> {
                        presenceActionState.value = PresenceActionUiState.Closed
                    }

                    is QuantityMutationResult.Failure -> {
                        presenceActionState.value =
                            current.copy(
                                isPresent = !isPresent,
                                isSubmitting = false,
                                error = true,
                            )
                    }
                }
            }
        }

        private fun confirmConsume(state: QuantityActionUiState.ConsumeEditing) {
            val parseResult = ExactQuantityInputParser.parse(state.amountInput, state.selectedUnit)
            if (parseResult !is ExactQuantityInputResult.Success) {
                quantityActionState.value = state.copy(error = mapParseError(parseResult))
                return
            }

            quantityActionState.value = state.copy(isSubmitting = true)
            viewModelScope.launch {
                val result =
                    consumeExactQuantityUseCase(
                        stockItemId = StockItemId.from(state.item.id),
                        toConsume = parseResult.quantity,
                    )
                handleMutationResult(result)
            }
        }

        private fun confirmCorrect(state: QuantityActionUiState.CorrectEditing) {
            val parseResult = ExactQuantityInputParser.parse(state.amountInput, state.selectedUnit)
            if (parseResult !is ExactQuantityInputResult.Success) {
                quantityActionState.value = state.copy(error = mapParseError(parseResult))
                return
            }

            quantityActionState.value = state.copy(isSubmitting = true)
            viewModelScope.launch {
                val result =
                    correctExactQuantityUseCase(
                        stockItemId = StockItemId.from(state.item.id),
                        newQuantity = parseResult.quantity,
                    )
                handleMutationResult(result)
            }
        }

        private fun handleMutationResult(result: QuantityMutationResult) {
            when (result) {
                is QuantityMutationResult.Success -> {
                    quantityActionState.value = QuantityActionUiState.Closed
                }

                is QuantityMutationResult.Failure -> {
                    val error =
                        when (result.error) {
                            DomainError.IncompatibleQuantityDimensions -> QuantityActionError.INCOMPATIBLE_UNIT
                            DomainError.InsufficientQuantity,
                            DomainError.ResultingQuantityMustBePositive,
                            DomainError.AmountMustBeLowerThanCurrent,
                            -> QuantityActionError.MUST_BE_LOWER_THAN_CURRENT
                            DomainError.AmountMustBePositive -> QuantityActionError.MUST_BE_POSITIVE
                            DomainError.NegativeQuantity,
                            DomainError.TooManyDecimalPlaces,
                            -> QuantityActionError.INVALID_AMOUNT
                            DomainError.UnchangedQuantity -> QuantityActionError.UNCHANGED
                            DomainError.ProductNotFound -> QuantityActionError.PRODUCT_NOT_FOUND
                            DomainError.StorageFailure,
                            DomainError.ApproximateLevelDidNotDecrease,
                            DomainError.IncompatibleMode,
                            -> QuantityActionError.STORAGE_FAILURE
                        }
                    quantityActionState.update { current ->
                        when (current) {
                            is QuantityActionUiState.ConsumeEditing -> current.copy(error = error, isSubmitting = false)
                            is QuantityActionUiState.CorrectEditing -> current.copy(error = error, isSubmitting = false)
                            else -> current
                        }
                    }
                }
            }
        }

        private fun mapParseError(result: ExactQuantityInputResult): QuantityActionError =
            when (result) {
                ExactQuantityInputResult.Blank,
                ExactQuantityInputResult.InvalidFormat,
                -> QuantityActionError.INVALID_AMOUNT
                ExactQuantityInputResult.NotPositive -> QuantityActionError.MUST_BE_POSITIVE
                ExactQuantityInputResult.TooManyDecimalPlaces -> QuantityActionError.INVALID_AMOUNT
                else -> QuantityActionError.INVALID_AMOUNT
            }

        @Suppress("ReturnCount")
        private fun calculateConsumePreview(state: QuantityActionUiState.ConsumeEditing): QuantityPreviewUiModel? {
            val parseResult = ExactQuantityInputParser.parse(state.amountInput, state.selectedUnit)
            if (parseResult !is ExactQuantityInputResult.Success) return null

            val quantity = state.item.quantity as? ExactQuantity ?: return null

            val result =
                getConsumePreviewUseCase(
                    available = quantity,
                    toConsume = parseResult.quantity,
                )

            return if (result is com.luisete.queda.core.domain.result.Success) {
                QuantityPreviewUiModel(
                    amountFormatted = ExactQuantityUiFormatter.format(result.value),
                    unit = result.value.unit,
                )
            } else {
                null
            }
        }

        @Suppress("ReturnCount")
        private fun calculateCorrectPreview(state: QuantityActionUiState.CorrectEditing): QuantityPreviewUiModel? {
            val parseResult = ExactQuantityInputParser.parse(state.amountInput, state.selectedUnit)
            if (parseResult !is ExactQuantityInputResult.Success) return null

            val quantity = state.item.quantity as? ExactQuantity ?: return null

            val result =
                getCorrectPreviewUseCase(
                    current = quantity,
                    newQuantity = parseResult.quantity,
                )

            return if (result is com.luisete.queda.core.domain.result.Success) {
                QuantityPreviewUiModel(
                    amountFormatted = ExactQuantityUiFormatter.format(result.value),
                    unit = result.value.unit,
                )
            } else {
                null
            }
        }

        private fun isSubmitting(): Boolean {
            val currentQ = quantityActionState.value
            val currentP = presenceActionState.value
            return (currentQ as? QuantityActionUiState.ConsumeEditing)?.isSubmitting == true ||
                (currentQ as? QuantityActionUiState.CorrectEditing)?.isSubmitting == true ||
                (currentP as? PresenceActionUiState.Managing)?.isSubmitting == true
        }

        companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
