package com.luisete.queda.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.domain.shopping.AddShoppingEntry
import com.luisete.queda.core.domain.shopping.ObserveShoppingEntries
import com.luisete.queda.core.domain.shopping.SetShoppingPurchased
import com.luisete.queda.core.domain.shopping.ShoppingResult
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.shopping.ShoppingEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel
    @Inject
    constructor(
        private val observe: ObserveShoppingEntries,
        private val addShoppingEntry: AddShoppingEntry,
        private val setPurchasedUseCase: SetShoppingPurchased,
    ) : ViewModel() {
        private val _entries = MutableStateFlow<List<ShoppingEntry>>(emptyList())
        val entries: StateFlow<List<ShoppingEntry>> = _entries
        private val _message = MutableStateFlow<ShoppingMessage?>(null)
        val message: StateFlow<ShoppingMessage?> = _message

        init {
            viewModelScope.launch {
                observe()
                    .catch { _message.value = ShoppingMessage.SaveError }
                    .collectLatest { _entries.value = it }
            }
        }

        fun add(raw: String) {
            viewModelScope.launch {
                _message.value =
                    if (raw.isBlank() || raw.length > MAX_NAME_LENGTH) {
                        ShoppingMessage.InvalidName
                    } else {
                        when (addShoppingEntry(raw)) {
                            ShoppingResult.Saved -> null
                            ShoppingResult.AlreadyExists -> ShoppingMessage.AlreadyExists
                            else -> ShoppingMessage.SaveError
                        }
                    }
            }
        }

        fun setPurchased(
            id: ShoppingEntryId,
            purchased: Boolean,
        ) {
            viewModelScope.launch {
                if (setPurchasedUseCase(id, purchased) != ShoppingResult.Saved) {
                    _message.value = ShoppingMessage.SaveError
                }
            }
        }

        fun clearMessage() {
            _message.value = null
        }

        private companion object {
            private const val MAX_NAME_LENGTH = 80
        }
    }

enum class ShoppingMessage { InvalidName, AlreadyExists, SaveError }
