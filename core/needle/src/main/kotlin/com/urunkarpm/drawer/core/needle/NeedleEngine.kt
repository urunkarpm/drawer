package com.urunkarpm.drawer.core.needle

import com.urunkarpm.drawer.core.needle.model.ModelProcessingState
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.core.needle.model.ToolCallResult
import com.urunkarpm.drawer.core.needle.model.ToolDefinition
import kotlinx.coroutines.flow.StateFlow

interface NeedleEngine {
    val modelStatus: StateFlow<ModelStatus>
    val processingState: StateFlow<ModelProcessingState>
    suspend fun predictToolCall(query: String, tools: List<ToolDefinition>): List<ToolCallResult>
    suspend fun getEmbedding(text: String): FloatArray?
    suspend fun downloadModel(): Result<Unit>
    suspend fun deleteModel(): Boolean
    fun updateProcessingSuccess(summary: String)
    fun updateProcessingError(summary: String)
    fun resetProcessingState()
}
