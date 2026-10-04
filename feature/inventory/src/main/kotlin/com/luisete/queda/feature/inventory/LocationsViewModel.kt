package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.ManageLocationsUseCase
import com.luisete.queda.core.domain.inventory.StockWriteResult
import com.luisete.queda.core.model.inventory.StorageLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class LocationEditor(
    val id: String? = null,
    val name: String = "",
    val archived: Boolean = false,
    val busy: Boolean = false,
    val result: StockWriteResult? = null,
)

@HiltViewModel
class LocationsViewModel
    @Inject
    constructor(private val manage: ManageLocationsUseCase) : ViewModel() {
        val locations = manage.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(TIMEOUT), emptyList())
        private val mutableEditor = MutableStateFlow(LocationEditor())
        val editor = mutableEditor.asStateFlow()
        private val gate = AtomicBoolean(false)

        fun name(name: String) {
            if (!gate.get()) mutableEditor.update { it.copy(name = name, result = null) }
        }

        fun edit(location: StorageLocation) {
            if (!gate.get()) mutableEditor.value = LocationEditor(location.id, location.name, location.archived)
        }

        fun cancel() {
            if (!gate.get()) mutableEditor.value = LocationEditor()
        }

        fun archive(location: StorageLocation) {
            submit(LocationEditor(location.id, location.name, !location.archived))
        }

        fun resolve(
            location: StorageLocation,
            keepLocal: Boolean,
        ) {
            execute { manage.resolve(location.id, keepLocal) }
        }

        fun save() {
            submit(mutableEditor.value)
        }

        private fun submit(snapshot: LocationEditor) {
            execute { manage.save(snapshot.id, snapshot.name, snapshot.archived) }
        }

        private fun execute(action: suspend () -> StockWriteResult) {
            if (!gate.compareAndSet(false, true)) return
            mutableEditor.update { it.copy(busy = true, result = null) }
            viewModelScope.launch {
                try {
                    val result = action()
                    mutableEditor.update {
                        if (result == StockWriteResult.SAVED) {
                            LocationEditor(
                                result = result,
                            )
                        } else {
                            it.copy(result = result)
                        }
                    }
                } finally {
                    gate.set(false)
                    mutableEditor.update { it.copy(busy = false) }
                }
            }
        }

        companion object {
            private const val TIMEOUT = 5000L
        }
    }
