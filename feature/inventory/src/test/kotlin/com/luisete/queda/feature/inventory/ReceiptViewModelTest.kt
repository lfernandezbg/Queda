package com.luisete.queda.feature.inventory

import com.luisete.queda.core.domain.inventory.ManageReceiptUseCase
import com.luisete.queda.core.domain.inventory.ReceiptParser
import com.luisete.queda.core.domain.inventory.ReceiptRecognition
import com.luisete.queda.core.domain.inventory.ReceiptRepository
import com.luisete.queda.core.domain.inventory.ReceiptResult
import com.luisete.queda.core.domain.inventory.ReceiptTextReader
import com.luisete.queda.core.domain.inventory.RecognizeReceiptUseCase
import com.luisete.queda.core.model.inventory.ReceiptDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun rapidEditsSaveTheLatestFieldsAndDiscardCannotResurrectTheDraft() =
        runTest {
            val repository = RecordingReceipts()
            val model =
                ReceiptViewModel(
                    ManageReceiptUseCase(repository),
                    RecognizeReceiptUseCase(
                        object : ReceiptTextReader {
                            override suspend fun recognize(imageUri: String): ReceiptRecognition {
                                return ReceiptRecognition.Unreadable
                            }
                        },
                    ),
                )
            advanceUntilIdle()
            val id = model.state.value.draft!!.lines.single().id
            model.edit(id) { it.copy(name = "Pan integral") }
            model.edit(id) { it.copy(quantity = "2") }
            model.edit(id) { it.copy(selected = true, reviewed = true) }
            advanceUntilIdle()
            assertEquals("Pan integral", repository.saved.last().lines.single().name)
            assertEquals("2", repository.saved.last().lines.single().quantity)
            assertTrue(repository.saved.last().lines.single().reviewed)
            model.edit(id) { it.copy(name = "Nueva edición", reviewed = false) }
            model.discard()
            advanceUntilIdle()
            assertEquals(null, repository.draft.value)
            assertEquals(null, model.state.value.draft)
        }

    private class RecordingReceipts : ReceiptRepository {
        val draft = MutableStateFlow<ReceiptDraft?>(ReceiptParser.parse("PAN 0,90"))
        val saved = mutableListOf<ReceiptDraft>()

        override fun observeDraft() = draft

        override suspend fun save(draft: ReceiptDraft): ReceiptResult {
            saved += draft
            this.draft.value = draft
            return ReceiptResult.SAVED
        }

        override suspend fun discard(id: String) {
            draft.value = null
        }

        override suspend fun import(draft: ReceiptDraft): ReceiptResult = ReceiptResult.IMPORTED
    }
}
