package com.luisete.queda.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.inventory.ManageReceiptUseCase
import com.luisete.queda.core.domain.inventory.ReceiptRecognition
import com.luisete.queda.core.domain.inventory.ReceiptResult
import com.luisete.queda.core.domain.inventory.RecognizeReceiptUseCase
import com.luisete.queda.core.model.inventory.ReceiptDraft
import com.luisete.queda.core.model.inventory.ReceiptLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class ReceiptState(val draft: ReceiptDraft? = null, val busy: Boolean = false, val result: ReceiptResult? = null)

@HiltViewModel
class ReceiptViewModel
    @Inject
    constructor(
        private val manage: ManageReceiptUseCase,
        private val recognizeReceipt: RecognizeReceiptUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ReceiptState())
        val state = mutableState.asStateFlow()
        private val updates = Channel<ReceiptDraft>(Channel.CONFLATED)
        private val mutex = Mutex()
        private val gate = AtomicBoolean(false)
        private var restored = false

        init {
            viewModelScope.launch {
                manage.draft().collect { draft ->
                    if (!restored && !gate.get()) {
                        mutableState.update { it.copy(draft = draft) }
                        if (draft != null) restored = true
                    }
                }
            }
            viewModelScope.launch {
                for (draft in updates) mutex.withLock {
                    if (!gate.get() && mutableState.value.draft?.id == draft.id) {
                        val result = manage.save(draft)
                        if (result != ReceiptResult.SAVED) mutableState.update { it.copy(result = result) }
                    }
                }
            }
        }

        fun recognize(uri: String) = recognize(listOf(uri))

        fun recognize(uris: List<String>) =
            perform {
                when (val recognition = recognizeReceipt(uris)) {
                    ReceiptRecognition.Unreadable -> mutableState.update { it.copy(result = ReceiptResult.UNREADABLE) }
                    is ReceiptRecognition.Recognized -> {
                        val result = manage.save(recognition.draft)
                        if (result == ReceiptResult.SAVED) {
                            restored = true
                            mutableState.update { it.copy(draft = recognition.draft, result = null) }
                        } else {
                            mutableState.update { it.copy(result = result) }
                        }
                    }
                }
            }

        fun edit(
            id: String,
            change: (ReceiptLine) -> ReceiptLine,
        ) {
            if (gate.get()) return
            val draft = mutableState.value.draft ?: return
            val updated = draft.copy(lines = draft.lines.map { if (it.id == id) change(it) else it })
            mutableState.update { it.copy(draft = updated, result = null) }
            updates.trySend(updated)
        }

        fun import() =
            perform {
                val draft = mutableState.value.draft ?: return@perform
                val saved = manage.save(draft)
                val result = if (saved == ReceiptResult.SAVED) manage.import(draft) else saved
                mutableState.update {
                    it.copy(
                        result = result,
                        draft = if (result == ReceiptResult.IMPORTED) null else it.draft,
                    )
                }
            }

        fun discard() =
            perform {
                val draft = mutableState.value.draft ?: return@perform
                manage.discard(draft.id)
                restored = true
                mutableState.value = ReceiptState(busy = true)
            }

        fun unavailable() {
            mutableState.update { it.copy(result = ReceiptResult.UNREADABLE) }
        }

        private fun perform(action: suspend () -> Unit) {
            if (!gate.compareAndSet(false, true)) return
            mutableState.update { it.copy(busy = true, result = null) }
            viewModelScope.launch {
                try {
                    mutex.withLock { action() }
                } finally {
                    gate.set(false)
                    mutableState.update { it.copy(busy = false) }
                }
            }
        }
    }
