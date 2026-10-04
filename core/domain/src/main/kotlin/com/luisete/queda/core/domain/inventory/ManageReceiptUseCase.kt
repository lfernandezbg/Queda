package com.luisete.queda.core.domain.inventory

import com.luisete.queda.core.model.inventory.ReceiptDraft
import com.luisete.queda.core.model.product.ProductName
import com.luisete.queda.core.model.product.ProductNameCreationResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class ReceiptResult { SAVED, IMPORTED, DUPLICATE, INVALID, UNREADABLE, STORAGE_FAILURE }

private const val MAX_RECEIPT_PAGES = 12

interface ReceiptRepository {
    fun observeDraft(): Flow<ReceiptDraft?>

    suspend fun save(draft: ReceiptDraft): ReceiptResult

    suspend fun discard(id: String)

    suspend fun import(draft: ReceiptDraft): ReceiptResult
}

sealed interface ReceiptRecognition {
    data class Recognized(val draft: ReceiptDraft) : ReceiptRecognition

    data object Unreadable : ReceiptRecognition
}

interface ReceiptTextReader {
    suspend fun recognize(imageUri: String): ReceiptRecognition

    suspend fun recognizePages(imageUris: List<String>): ReceiptRecognition {
        if (imageUris.isEmpty() || imageUris.size > MAX_RECEIPT_PAGES) return ReceiptRecognition.Unreadable
        val lines = mutableListOf<String>()
        imageUris.forEach { image ->
            val next =
                (recognize(image) as? ReceiptRecognition.Recognized)?.draft?.lines
                    ?.map { it.source }.orEmpty()
            val overlap =
                (1..minOf(lines.size, next.size)).lastOrNull { count ->
                    lines.takeLast(count) == next.take(count)
                } ?: 0
            lines += next.drop(overlap)
        }
        return if (lines.isEmpty()) {
            ReceiptRecognition.Unreadable
        } else {
            ReceiptRecognition.Recognized(ReceiptParser.parse(lines.joinToString("\n")))
        }
    }
}

class RecognizeReceiptUseCase
    @Inject
    constructor(private val reader: ReceiptTextReader) {
        suspend operator fun invoke(imageUri: String): ReceiptRecognition = reader.recognize(imageUri)

        suspend operator fun invoke(imageUris: List<String>): ReceiptRecognition = reader.recognizePages(imageUris)
    }

class ManageReceiptUseCase
    @Inject
    constructor(private val repository: ReceiptRepository) {
        fun draft(): Flow<ReceiptDraft?> = repository.observeDraft()

        suspend fun save(draft: ReceiptDraft): ReceiptResult =
            if (draft.lines.size > MAX_LINES || draft.lines.map { it.id }.distinct().size != draft.lines.size) {
                ReceiptResult.INVALID
            } else {
                repository.save(draft)
            }

        suspend fun discard(id: String) = repository.discard(id)

        suspend fun import(draft: ReceiptDraft): ReceiptResult {
            val selected = draft.lines.filter { it.selected }
            val valid =
                selected.size in 1..MAX_IMPORT &&
                    selected.all {
                        it.reviewed && ProductName.create(it.name) is ProductNameCreationResult.Success &&
                            ExactQuantityInputParser.parse(it.quantity, it.unit) is ExactQuantityInputResult.Success
                    }
            return if (!valid) ReceiptResult.INVALID else repository.import(draft)
        }

        companion object {
            private const val MAX_LINES = 120
            private const val MAX_IMPORT = 60
        }
    }
