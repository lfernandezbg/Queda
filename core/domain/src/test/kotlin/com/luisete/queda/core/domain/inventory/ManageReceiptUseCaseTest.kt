package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.ReceiptDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ManageReceiptUseCaseTest {
    @Test
    fun everySelectedLineMustBeReviewedAndValidBeforeImport() =
        runTest {
            val repository = RecordingReceipts()
            val useCase = ManageReceiptUseCase(repository)
            val draft = ReceiptParser.parse("PAN 0,90\nLECHE 1,20")
            val lines = draft.lines.map { it.copy(selected = true, reviewed = true) }
            assertEquals(
                ReceiptResult.INVALID,
                useCase.import(
                    draft.copy(
                        lines =
                            lines.mapIndexed { i, line ->
                                if (i == 1) line.copy(reviewed = false) else line
                            },
                    ),
                ),
            )
            assertEquals(
                ReceiptResult.INVALID,
                useCase.import(
                    draft.copy(
                        lines =
                            lines.mapIndexed { i, line ->
                                if (i == 1) line.copy(quantity = "0") else line
                            },
                    ),
                ),
            )
            assertEquals(0, repository.imports)
            assertEquals(ReceiptResult.IMPORTED, useCase.import(draft.copy(lines = lines)))
            assertEquals(1, repository.imports)
        }

    @Test
    fun unselectedUnreviewedLinesDoNotBlockReviewedItems() =
        runTest {
            val repository = RecordingReceipts()
            val draft = ReceiptParser.parse("PAN 0,90\nLECHE 1,20")
            val lines =
                draft.lines.mapIndexed {
                        i,
                        line,
                    ->
                    if (i == 0) {
                        line.copy(
                            selected = true,
                            reviewed = true,
                        )
                    } else {
                        line
                    }
                }
            assertEquals(ReceiptResult.IMPORTED, ManageReceiptUseCase(repository).import(draft.copy(lines = lines)))
        }

    private class RecordingReceipts : ReceiptRepository {
        var imports = 0

        override fun observeDraft(): Flow<ReceiptDraft?> = flowOf(null)

        override suspend fun save(draft: ReceiptDraft): ReceiptResult = ReceiptResult.SAVED

        override suspend fun discard(id: String) = Unit

        override suspend fun import(draft: ReceiptDraft): ReceiptResult {
            imports++
            return ReceiptResult.IMPORTED
        }
    }
}
