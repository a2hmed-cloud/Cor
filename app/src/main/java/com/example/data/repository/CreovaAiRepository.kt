package com.example.data.repository

import android.util.Log
import com.example.data.catalog.CuratedCatalog
import com.example.data.preferences.GeminiModelOption
import com.example.domain.model.CreativeResource
import com.example.domain.model.CreativeTool
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val AI_TAG = "CreovaAiRepo"

data class AiStructuredPick(
    val name: String,
    val type: String,
    val pricing: String,
    val reason: String,
    val verifiedUrl: String
)

data class AiStructuredResponse(
    val summary: String,
    val workflowSteps: List<String>,
    val picks: List<AiStructuredPick>
)

class CreovaAiRepository {

    private fun buildSystemPrompt(
        tools: List<CreativeTool>,
        resources: List<CreativeResource>
    ): String {
        val toolIndex = tools.joinToString("\n") { tool ->
            "- ${tool.name} (${tool.nameAr}) | pricing=${tool.pricing} | categories=${tool.categories.joinToString(",")} | url=${tool.officialUrl}"
        }
        val resourceIndex = resources.take(25).joinToString("\n") { res ->
            "- ${res.title} (${res.titleAr}) | category=${res.category} | format=${res.fileFormat} | license=${res.licenseInfo} | url=${res.downloadUrl}"
        }
        return """
You are CREOVA AI, the creative ecosystem assistant inside the CREOVA Android app.
Language & Tone:
- Respond naturally in a friendly, sharp Egyptian Arabic & English creative blend by default, or match Modern Standard Arabic / English if the user writes in them.
- Be concise, practical, and structured for mobile reading.

Strict Grounding & Honesty Rules:
1. Only cite URLs that appear in the VERIFIED CREOVA DIRECTORY or VERIFIED CREOVA RESOURCES below. NEVER invent or guess URLs.
2. NEVER falsely label a 'paid' or 'freemium' tool as completely 'free'. State the exact pricing classification honestly.
3. Clearly distinguish verified CREOVA directory items from general creative workflow advice.

VERIFIED CREOVA DIRECTORY:
$toolIndex

VERIFIED CREOVA RESOURCES:
$resourceIndex
        """.trimIndent()
    }

    fun streamChat(
        modelOption: GeminiModelOption,
        historyPairs: List<Pair<String, String>>,
        userMessage: String,
        tools: List<CreativeTool> = CuratedCatalog.verifiedTools,
        resources: List<CreativeResource> = CuratedCatalog.curatedResources
    ): Flow<String> = flow {
        val systemInstruction = content { text(buildSystemPrompt(tools, resources)) }
        val model by lazy {
            Firebase.ai.generativeModel(
                modelName = modelOption.modelId,
                systemInstruction = systemInstruction
            )
        }
        val chatHistory: List<Content> = historyPairs.takeLast(8).flatMap { (userMsg, modelMsg) ->
            listOf(
                content("user") { text(userMsg) },
                content("model") { text(modelMsg) }
            )
        }
        val chat = model.startChat(history = chatHistory)
        val acc = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    acc.append(chunkText)
                    emit(acc.toString())
                }
            }
        }
        if (acc.isBlank()) {
            throw IllegalStateException("CREOVA AI returned an empty response.")
        }
    }

    suspend fun generateStructuredPicks(
        modelOption: GeminiModelOption,
        userGoal: String,
        tools: List<CreativeTool> = CuratedCatalog.verifiedTools,
        resources: List<CreativeResource> = CuratedCatalog.curatedResources
    ): Result<AiStructuredResponse> {
        val pickSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(description = "Exact tool or resource name"),
                "type" to Schema.enumeration(listOf("tool", "resource", "workflow_tip")),
                "pricing" to Schema.enumeration(listOf("free", "freemium", "paid", "open_source", "guide")),
                "reason" to Schema.string(description = "Concise reason in Egyptian Arabic or English"),
                "verifiedUrl" to Schema.string(description = "Exact verified https URL from CREOVA catalog, or empty string if general tip")
            ),
            optionalProperties = listOf("verifiedUrl")
        )
        val responseSchema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Friendly summary answer in Egyptian Arabic & English blend"),
                "workflowSteps" to Schema.array(
                    items = Schema.string(),
                    description = "3 to 5 concise actionable creative workflow steps"
                ),
                "picks" to Schema.array(
                    items = pickSchema,
                    description = "Recommended verified tools or resources"
                )
            )
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            this.responseSchema = responseSchema
        }
        val model by lazy {
            Firebase.ai.generativeModel(
                modelName = modelOption.modelId,
                generationConfig = config,
                systemInstruction = content { text(buildSystemPrompt(tools, resources)) }
            )
        }
        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(userGoal)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Model returned empty structured JSON."))
            Result.success(parseStructuredResponse(rawJson, tools, resources))
        } catch (e: TimeoutCancellationException) {
            Log.e(AI_TAG, "Structured AI generation timed out", e)
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AI_TAG, "Structured AI generation failed", e)
            Result.failure(e)
        }
    }

    companion object {
        fun parseStructuredResponse(
            rawJson: String,
            verifiedTools: List<CreativeTool> = CuratedCatalog.verifiedTools,
            verifiedResources: List<CreativeResource> = CuratedCatalog.curatedResources
        ): AiStructuredResponse {
            val allowedUrls = (verifiedTools.map { it.officialUrl } + verifiedResources.map { it.downloadUrl }).toSet()
            val json = JSONObject(rawJson)
            val summary = json.optString("summary", "")
            val stepsArr = json.optJSONArray("workflowSteps")
            val steps = if (stepsArr != null) {
                (0 until stepsArr.length()).mapNotNull { stepsArr.optString(it).takeIf { s -> s.isNotBlank() } }
            } else {
                emptyList()
            }
            val picksArr = json.optJSONArray("picks")
            val picks = if (picksArr != null) {
                (0 until picksArr.length()).mapNotNull { idx ->
                    val obj = picksArr.optJSONObject(idx) ?: return@mapNotNull null
                    val rawUrl = obj.optString("verifiedUrl", "").trim()
                    val safeVerifiedUrl = if (rawUrl in allowedUrls) rawUrl else ""
                    AiStructuredPick(
                        name = obj.optString("name", ""),
                        type = obj.optString("type", "tool"),
                        pricing = obj.optString("pricing", "freemium"),
                        reason = obj.optString("reason", ""),
                        verifiedUrl = safeVerifiedUrl
                    )
                }.filter { it.name.isNotBlank() }
            } else {
                emptyList()
            }
            return AiStructuredResponse(summary = summary, workflowSteps = steps, picks = picks)
        }
    }
}
