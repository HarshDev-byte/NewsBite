package com.example.newsbite.data.ai

import com.example.newsbite.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSummarizationService @Inject constructor() {
    
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 500
            }
        )
    }
    
    suspend fun summarizeArticle(title: String, description: String?, content: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildPrompt(title, description, content)
                val response = model.generateContent(prompt)
                val summary = response.text
                
                if (summary.isNullOrBlank()) {
                    Result.failure(Exception("Empty response from AI"))
                } else {
                    Result.success(summary)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun buildPrompt(title: String, description: String?, content: String?): String {
        val articleContent = buildString {
            append("Title: $title\n")
            if (!description.isNullOrBlank()) {
                append("Description: $description\n")
            }
            if (!content.isNullOrBlank()) {
                append("Content: $content\n")
            }
        }
        
        return """
            You are a helpful news summarizer. Summarize the following news article in 3-4 concise bullet points.
            Focus on the key facts and main takeaways. Keep it brief and informative.
            
            $articleContent
            
            Provide the summary in bullet points format:
        """.trimIndent()
    }
}
