@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming", "detekt:LongMethod", "detekt:MagicNumber")

package com.luisete.queda.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.domain.inventory.ObserveExactInventoryItemsUseCase
import com.luisete.queda.core.domain.shopping.ObserveShoppingEntries
import com.luisete.queda.core.model.inventory.InventoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TodayViewModel
    @Inject
    constructor(
        inventory: ObserveExactInventoryItemsUseCase,
        shopping: ObserveShoppingEntries,
    ) : ViewModel() {
        val state =
            combine(
                inventory(),
                shopping(),
            ) { stock, list ->
                TodayState(stock, list.count { !it.purchased })
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TodayState(),
            )
    }

data class TodayState(
    val inventory: List<InventoryItem> = emptyList(),
    val pending: Int = 0,
)

@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    onShopping: () -> Unit,
    onInventory: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onShopping = onShopping,
        onInventory = onInventory,
    )
}

@Composable
fun TodayScreen(
    state: TodayState,
    onShopping: () -> Unit,
    onInventory: () -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = QuedaSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
    ) {
        item {
            Text(
                text = stringResource(R.string.today_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = QuedaSpacing.Large),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(QuedaSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Medium),
                ) {
                    Text(
                        text = stringResource(R.string.today_hero),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = stringResource(R.string.today_shopping_count, state.pending),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Button(
                        onClick = onShopping,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(stringResource(R.string.today_open_shopping))
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.today_inventory_count, state.inventory.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Button(onClick = onInventory) {
                    Text(stringResource(R.string.today_open_inventory))
                }
            }
            Text(
                text = stringResource(R.string.today_at_home),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        items(
            items = state.inventory.take(4),
            key = { it.stockItem.id.value },
        ) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.product.name.displayValue,
                    modifier = Modifier.padding(QuedaSpacing.Medium),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
