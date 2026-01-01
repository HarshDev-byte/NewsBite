package com.example.newsbite

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsbite.data.repository.BookmarkRepository
import com.example.newsbite.databinding.ActivityMainBinding
import com.example.newsbite.ui.NewsViewModel
import com.example.newsbite.util.ThemeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), View.OnClickListener {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: NewsViewModel by viewModels()
    private lateinit var adapter: NewsRecyclerAdapter
    private lateinit var loadStateAdapter: NewsLoadStateAdapter
    
    @Inject
    lateinit var bookmarkRepository: BookmarkRepository
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        
        setupRecyclerView()
        setupClickListeners()
        setupSearchView()
        setupSwipeRefresh()
        collectPagedNews()
        observeLoadStates()
        observeBookmarks()
        updateThemeIcon()
    }
    
    /**
     * Sets up RecyclerView with PagingDataAdapter and LoadStateAdapter.
     * Uses ConcatAdapter to combine news adapter with footer load state adapter.
     * Requirements: 1.2, 1.3, 1.4
     */
    private fun setupRecyclerView() {
        binding.newsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Create the main news adapter
        adapter = NewsRecyclerAdapter { article ->
            bookmarkRepository.toggleBookmark(article)
            Toast.makeText(this, R.string.msg_bookmark_updated, Toast.LENGTH_SHORT).show()
        }
        
        // Create load state adapter for footer with retry callback
        loadStateAdapter = NewsLoadStateAdapter { adapter.retry() }
        
        // Use ConcatAdapter to combine news adapter with load state footer
        binding.newsRecyclerView.adapter = adapter.withLoadStateFooter(
            footer = loadStateAdapter
        )
    }
    
    private fun setupClickListeners() {
        binding.btn1.setOnClickListener(this)
        binding.btn2.setOnClickListener(this)
        binding.btn3.setOnClickListener(this)
        binding.btn4.setOnClickListener(this)
        binding.btn5.setOnClickListener(this)
        binding.btn6.setOnClickListener(this)
        binding.btn7.setOnClickListener(this)
        
        binding.retryButton.setOnClickListener { adapter.retry() }
        
        binding.bookmarksButton.setOnClickListener {
            startActivity(Intent(this, BookmarksActivity::class.java))
        }
        
        binding.themeToggleButton.setOnClickListener {
            themeManager.toggleTheme()
        }
    }
    
    private fun updateThemeIcon() {
        val icon = if (themeManager.isDarkMode) {
            R.drawable.ic_light_mode
        } else {
            R.drawable.ic_dark_mode
        }
        binding.themeToggleButton.setImageResource(icon)
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchNews(it) }
                binding.searchView.clearFocus()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }
    
    /**
     * Sets up pull-to-refresh to trigger adapter refresh.
     * Requirements: 2.3
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.my_primary)
        binding.swipeRefresh.setOnRefreshListener { 
            adapter.refresh()
        }
    }
    
    /**
     * Collects pagedNews Flow and submits PagingData to adapter.
     * Uses lifecycleScope with repeatOnLifecycle for lifecycle-aware collection.
     * Requirements: 1.1
     */
    private fun collectPagedNews() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pagedNews.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }
    }
    
    /**
     * Observes adapter load states for initial load handling.
     * Shows shimmer only for initial refresh, handles errors.
     * Requirements: 1.2
     */
    private fun observeLoadStates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { loadStates ->
                    val refreshState = loadStates.refresh
                    
                    // Handle swipe refresh indicator
                    binding.swipeRefresh.isRefreshing = refreshState is LoadState.Loading && 
                        adapter.itemCount > 0
                    
                    // Show shimmer only for initial load (when list is empty)
                    val showShimmer = refreshState is LoadState.Loading && adapter.itemCount == 0
                    showShimmer(showShimmer)
                    
                    // Handle error state for initial load
                    when (refreshState) {
                        is LoadState.Error -> {
                            if (adapter.itemCount == 0) {
                                showError(refreshState.error.localizedMessage ?: getString(R.string.msg_error_generic))
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    refreshState.error.localizedMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        is LoadState.NotLoading -> {
                            if (adapter.itemCount > 0) {
                                showContent()
                            }
                        }
                        else -> { /* Loading state handled above */ }
                    }
                }
            }
        }
    }
    
    private fun observeBookmarks() {
        bookmarkRepository.getAllBookmarks().observe(this) { bookmarks ->
            val urls = bookmarks?.map { it.url }?.toSet() ?: emptySet()
            adapter.setBookmarkedUrls(urls)
        }
    }
    
    private fun showShimmer(show: Boolean) {
        if (show) {
            binding.shimmerLayout.root.visibility = View.VISIBLE
            binding.shimmerLayout.root.startShimmer()
            binding.newsRecyclerView.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            hideError()
        } else {
            binding.shimmerLayout.root.stopShimmer()
            binding.shimmerLayout.root.visibility = View.GONE
        }
    }
    
    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.newsRecyclerView.visibility = View.GONE
        binding.errorText.text = message
    }
    
    private fun hideError() {
        binding.errorLayout.visibility = View.GONE
    }
    
    private fun showContent() {
        binding.errorLayout.visibility = View.GONE
        binding.newsRecyclerView.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }
    
    override fun onClick(v: View) {
        val btn = v as Button
        val category = btn.text.toString().lowercase()
        viewModel.clearSearch()
        binding.searchView.setQuery("", false)
        binding.searchView.clearFocus()
        viewModel.setCategory(category)
    }
}
