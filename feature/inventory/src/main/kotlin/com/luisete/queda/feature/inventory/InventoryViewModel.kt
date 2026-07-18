package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.ObserveExactInventoryItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel
    @Inject
    constructor(
        private val observeExactInventoryItemsUseCase: ObserveExactInventoryItemsUseCase,
    ) : ViewModel() {
        private val refreshTrigger = MutableStateFlow(0)

        val uiState: StateFlow<InventoryUiState> =
            refreshTrigger
                .flatMapLatest {
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
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    initialValue = InventoryUiState.Loading,
                )

        fun retry() {
            refreshTrigger.update { it + 1 }
        }

        companion object {
            private const val STOP_TIMEOUT_MILLIS = 5000L
        }
    }
