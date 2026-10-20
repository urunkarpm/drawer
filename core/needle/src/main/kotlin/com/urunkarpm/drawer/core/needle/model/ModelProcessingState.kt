package com.urunkarpm.drawer.core.needle.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelProcessingState(
    val query: String = "",
    val step: ProcessingStep = ProcessingStep.Idle,
    val modelName: String = "Needle 3 (CQ2 · 121M On-Device)",
    val reasoning: String? = null,
    val confidence: Float? = null,
    val prefillTps: Float? = null,
    val decodeTps: Float? = null,
    val peakRamMb: Float? = null,
    val toolName: String? = null,
    val arguments: Map<String, String> = emptyMap(),
    val rawJson: String? = null,
    val isNativeInference: Boolean = false,
    val summary: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

enum class ProcessingStep {
    Idle,
    Inferring,
    Executing,
    Success,
    Fallback,
    Error
}
