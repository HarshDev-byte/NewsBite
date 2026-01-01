package com.example.newsbite

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newsbite.data.ai.AiSummarizationService
import com.example.newsbite.databinding.ActivityNewsFullBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NewsFullActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNewsFullBinding
    
    @Inject
    lateinit var aiService: AiSummarizationService
    
    private var isExpanded = false
    private var hasSummarized = false
    private var articleTitle: String? = null
    private var articleDescription: String? = null
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityNewsFullBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        
        val url = intent.getStringExtra("url")
        articleTitle = intent.getStringExtra("title")
        articleDescription = intent.getStringExtra("description")
        
        setupWebView()
        setupSummaryPanel()
        
        if (!url.isNullOrEmpty() && isValidUrl(url)) {
            binding.webView.loadUrl(url)
        } else {
            finish()
        }
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }
    
    private fun setupSummaryPanel() {
        // Check if API key is configured
        val hasApiKey = BuildConfig.GEMINI_API_KEY.isNotBlank()
        
        if (!hasApiKey) {
            binding.summaryCard.visibility = View.GONE
            return
        }
        
        binding.expandCollapseButton.setOnClickListener {
            toggleSummaryPanel()
        }
        
        binding.summaryCard.setOnClickListener {
            toggleSummaryPanel()
        }
        
        binding.summarizeButton.setOnClickListener {
            generateSummary()
        }
    }
    
    private fun toggleSummaryPanel() {
        isExpanded = !isExpanded
        binding.summaryContentLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        binding.expandCollapseButton.setImageResource(
            if (isExpanded) android.R.drawable.arrow_down_float 
            else android.R.drawable.arrow_up_float
        )
        
        // Auto-generate summary on first expand if not done yet
        if (isExpanded && !hasSummarized) {
            generateSummary()
        }
    }
    
    private fun generateSummary() {
        if (articleTitle.isNullOrBlank()) {
            binding.summaryText.text = getString(R.string.msg_summary_error)
            return
        }
        
        binding.summaryProgress.visibility = View.VISIBLE
        binding.summarizeButton.visibility = View.GONE
        binding.summaryText.text = getString(R.string.msg_summarizing)
        
        lifecycleScope.launch {
            val result = aiService.summarizeArticle(
                title = articleTitle!!,
                description = articleDescription,
                content = null
            )
            
            binding.summaryProgress.visibility = View.GONE
            
            result.onSuccess { summary ->
                hasSummarized = true
                binding.summaryText.text = summary
                binding.summarizeButton.visibility = View.GONE
            }.onFailure { error ->
                binding.summaryText.text = getString(R.string.msg_summary_error)
                binding.summarizeButton.visibility = View.VISIBLE
                Toast.makeText(
                    this@NewsFullActivity,
                    error.message ?: getString(R.string.msg_summary_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled = true
        }
        
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val requestUrl = request?.url?.toString() ?: return false
                return !isValidUrl(requestUrl)
            }
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("https://") || url.startsWith("http://")
    }
    
    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }
    
    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
