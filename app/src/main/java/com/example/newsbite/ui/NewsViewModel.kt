package com.example.newsbite.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.newsbite.data.model.Article
import com.example.newsbite.data.repository.NewsRepository
import com.example.newsbite.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    
    // Legacy LiveData for non-paged operations (kept for backward compatibility)
    private val _newsLiveData = MutableLiveData<Resource<List<Article>>>()
    val newsLiveData: LiveData<Resource<List<Article>>> = _newsLiveData
    
    // StateFlow for category - used by pagination (Requirements 2.1)
    private val _currentCategory = MutableStateFlow("general")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()
    
    // StateFlow for search query - used by pagination (Requirements 2.2)
    private val _currentQuery = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _currentQuery.asStateFlow()
    
    private val _isFromCache = MutableLiveData(false)
    val isFromCache: LiveData<Boolean> = _isFromCache
    
    // Refresh trigger - incrementing this invalidates the current PagingSource (Requirements 2.3)
    private val _refreshTrigger = MutableStateFlow(0)
    
    /**
     * Paginated news flow that automatically updates when category, query, or refresh changes.
     * Uses flatMapLatest to cancel previous collection when inputs change.
     * Cached in viewModelScope to survive configuration changes.
     * 
     * Requirements: 1.1, 2.1, 2.2, 2.3
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedNews: Flow<PagingData<Article>> = combine(
        _currentCategory,
        _currentQuery,
        _refreshTrigger
    ) { category, query, _ ->
        Pair(category, query)
    }.flatMapLatest { (category, query) ->
        repository.getPagedNews(category, query)
    }.cachedIn(viewModelScope)
    
    /**
     * Sets the current category and triggers pagination reset.
     * Requirements: 2.1
     */
    fun setCategory(category: String) {
        if (_currentCategory.value != category) {
            _currentCategory.value = category
            // Clear search when switching categories
            _currentQuery.value = null
        }
    }
    
    /**
     * Legacy method for non-paged news loading (kept for backward compatibility).
     */
    fun loadNews(category: String, query: String? = null, forceRefresh: Boolean = false) {
        _currentCategory.value = category
        _currentQuery.value = query
        
        _newsLiveData.value = Resource.Loading
        
        viewModelScope.launch {
            val result = repository.getTopHeadlines(category, query, forceRefresh)
            _newsLiveData.value = result
        }
    }
    
    /**
     * Triggers a refresh by invalidating the current PagingSource.
     * This causes the Pager to create a new PagingSource and reload from page 1.
     * Requirements: 2.3
     */
    fun refresh() {
        _refreshTrigger.value++
    }
    
    /**
     * Sets the search query and triggers pagination reset.
     * Requirements: 2.2
     */
    fun searchNews(query: String) {
        _currentQuery.value = query.ifBlank { null }
    }
    
    /**
     * Clears the current search query and triggers pagination reset.
     */
    fun clearSearch() {
        _currentQuery.value = null
    }
    
    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }
}
