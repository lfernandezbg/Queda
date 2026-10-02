@file:Suppress("detekt:TooGenericExceptionCaught", "detekt:SwallowedException", "detekt:MaxLineLength")

package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.AddExactInventoryItemResult
import com.luisete.queda.core.domain.inventory.AddExactInventoryItemUseCase
import com.luisete.queda.core.domain.inventory.AddExactQuantityUseCase
import com.luisete.queda.core.domain.inventory.ExactQuantityInputParser
import com.luisete.queda.core.domain.inventory.ExactQuantityInputResult
import com.luisete.queda.core.domain.inventory.QuantityMutationResult
import com.luisete.queda.core.domain.inventory.SetPresenceUseCase
import com.luisete.queda.core.model.id.StockItemId
import com.luisete.queda.core.model.quantity.MeasurementUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ContinuousScanViewModel
    @Inject
    constructor(
        private val add: AddExactInventoryItemUseCase,
        private val increase: AddExactQuantityUseCase,
        private val setPresence: SetPresenceUseCase,
    ) : ViewModel() {
        private val _savedCount = MutableStateFlow(0)
        val savedCount = _savedCount.asStateFlow()
        private val _error = MutableStateFlow(false)
        val error = _error.asStateFlow()
        private val saved = Channel<Unit>(Channel.BUFFERED)
        val savedEvents = saved.receiveAsFlow()
        private val savingGate = AtomicBoolean(false)
        private val _saving = MutableStateFlow(false)
        val saving = _saving.asStateFlow()

        fun save(
            pending: PendingScan,
            name: String,
            amount: String,
            unit: MeasurementUnit,
        ) {
            if (!savingGate.compareAndSet(false, true)) return
            _saving.value = true
            viewModelScope.launch {
                try {
                    _error.value = false
                    val result =
                        when (pending) {
                            is PendingScan.New -> add(name, amount, unit, pending.barcode) is AddExactInventoryItemResult.Added
                            is PendingScan.Existing -> {
                                if (pending.isPresence) {
                                    setPresence(StockItemId.from(pending.stockItemId), true) is QuantityMutationResult.Success
                                } else {
                                    val parsed = ExactQuantityInputParser.parse(amount, unit)
                                    parsed is ExactQuantityInputResult.Success &&
                                        increase(StockItemId.from(pending.stockItemId), parsed.quantity) is QuantityMutationResult.Success
                                }
                            }
                        }
                    if (result) {
                        _savedCount.value += 1
                        saved.send(Unit)
                    } else {
                        _error.value = true
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _error.value = true
                } finally {
                    _saving.value = false
                    savingGate.set(false)
                }
            }
        }
    }
