@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming", "detekt:LongMethod")

package com.luisete.queda.feature.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luisete.queda.core.designsystem.theme.QuedaSpacing
import com.luisete.queda.core.model.id.ShoppingEntryId
import com.luisete.queda.core.model.shopping.ShoppingEntry

@Composable
fun ShoppingRoute(viewModel: ShoppingViewModel) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    ShoppingScreen(
        entries = entries,
        message = message,
        onAdd = viewModel::add,
        onSetPurchased = viewModel::setPurchased,
        onDismissMessage = viewModel::clearMessage,
    )
}

@Composable
fun ShoppingScreen(
    entries: List<ShoppingEntry>,
    message: ShoppingMessage?,
    onAdd: (String) -> Unit,
    onSetPurchased: (ShoppingEntryId, Boolean) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(entries) {
        if (draft.isNotBlank() && entries.any { it.name == draft.trim() }) {
            draft = ""
        }
    }
    val pending = entries.filterNot { it.purchased }
    val purchased = entries.filter { it.purchased }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .semantics { testTagsAsResourceId = true }
                .padding(horizontal = QuedaSpacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.shopping_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.shopping_shared),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = QuedaSpacing.Medium, horizontal = QuedaSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(stringResource(R.string.shopping_add_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            if (draft.isNotBlank()) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onAdd(draft)
                                draft = ""
                            }
                        },
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("shopping_name"),
            )
            Spacer(modifier = Modifier.width(QuedaSpacing.Small))
            Button(
                onClick = {
                    if (draft.isNotBlank()) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onAdd(draft)
                        draft = ""
                    }
                },
                modifier = Modifier.testTag("shopping_add"),
            ) {
                Text("+")
            }
        }
        if (message != null) {
            val text =
                when (message) {
                    ShoppingMessage.InvalidName -> R.string.shopping_invalid_name
                    ShoppingMessage.AlreadyExists -> R.string.shopping_already_exists
                    ShoppingMessage.SaveError -> R.string.shopping_save_error
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(text),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissMessage) {
                    Text("×")
                }
            }
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(QuedaSpacing.Small),
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag("shopping_list"),
        ) {
            item {
                Text(
                    text = stringResource(R.string.shopping_pending, pending.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (pending.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(QuedaSpacing.Large)) {
                        Text(
                            text = stringResource(R.string.shopping_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.shopping_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            items(
                items = pending,
                key = { it.id.value },
            ) { entry ->
                ShoppingRow(entry = entry, onSetPurchased = onSetPurchased)
            }
            item {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.shopping_purchased, purchased.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
            }
            items(
                items = purchased,
                key = { it.id.value },
            ) { entry ->
                ShoppingRow(entry = entry, onSetPurchased = onSetPurchased)
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    entry: ShoppingEntry,
    onSetPurchased: (ShoppingEntryId, Boolean) -> Unit,
) {
    val description =
        stringResource(
            if (entry.purchased) R.string.shopping_mark_pending else R.string.shopping_mark_purchased,
            entry.name,
        )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onSetPurchased(entry.id, !entry.purchased) }
                    .testTag("shopping_${entry.id.value}")
                    .padding(QuedaSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = entry.purchased,
                onCheckedChange = null,
                modifier = Modifier.semantics { contentDescription = description },
            )
            Text(
                text = entry.name,
                modifier = Modifier.padding(start = QuedaSpacing.Small),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
