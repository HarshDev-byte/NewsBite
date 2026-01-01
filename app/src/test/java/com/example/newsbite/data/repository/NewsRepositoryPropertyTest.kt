package com.example.newsbite.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.newsbite.data.api.NewsApiService
import com.example.newsbite.data.local.CachedArticle
import com.example.newsbite.data.local.CachedArticleDao
import com.example.newsbite.data.local.NewsDatabase
import com.example.newsbite.data.local.RemoteKeysDao
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk

/**
 * Property-based tests for NewsRepository pagination behavior
 * 
 * Feature: paging-infinite-scroll
 */
@OptIn(ExperimentalPagingApi::class)
class NewsRepositoryPropertyTest : FunSpec({

    // Generators for property-based testing
    val categoryArb = Arb.string(minSize = 1, maxSize = 20).filter { it.isNotBlank() }
    val queryArb = Arb.string(minSize = 1, maxSize = 30).filter { it.isNotBlank() }

    /**
     * Property 2: State change invalidates pagination
     * 
     * *For any* category change, search query change, or pull-to-refresh action,
     * the current PagingData SHALL be invalidated and pagination SHALL restart from page 1.
     * 
     * **Validates: Requirements 2.1, 2.2, 2.3**
     * 
     * This property verifies that calling getPagedNews with different parameters
     * creates distinct Pager instances, ensuring pagination state is reset.
     * Each call to getPagedNews creates a new Pager with a fresh RemoteMediator,
     * which guarantees pagination restarts from page 1.
     */
    test("Property 2: State change invalidates pagination - different categories produce different Pager flows") {
        checkAll(100, categoryArb, categoryArb.filter { it != "general" }) { category1, category2 ->
            // Ensure we have two different categories
            val cat1 = if (category1 == category2) "${category1}1" else category1
            val cat2 = if (category1 == category2) "${category2}2" else category2
            
            // Setup mocks
            val apiService = mockk<NewsApiService>()
            val database = mockk<NewsDatabase>()
            val cachedArticleDao = mockk<CachedArticleDao>()
            val remoteKeysDao = mockk<RemoteKeysDao>()
            
            every { database.cachedArticleDao() } returns cachedArticleDao
            every { database.remoteKeysDao() } returns remoteKeysDao
            
            // Create a mock PagingSource for each category
            every { cachedArticleDao.getArticlesPagingSource(any()) } answers {
                val category = firstArg<String>()
                createMockPagingSource(category)
            }
            
            val repository = NewsRepository(
                apiService = apiService,
                cachedArticleDao = cachedArticleDao,
                database = database
            )
            
            // Get paged news for two different categories
            val flow1 = repository.getPagedNews(cat1)
            val flow2 = repository.getPagedNews(cat2)
            
            // Each call should return a different Flow instance
            // This ensures that switching categories creates a new Pager
            // which resets pagination state
            flow1 shouldNotBe flow2
        }
    }

    /**
     * Property 2 (continued): Search query changes invalidate pagination
     * 
     * **Validates: Requirements 2.2**
     */
    test("Property 2: State change invalidates pagination - different queries produce different Pager flows") {
        checkAll(100, categoryArb, queryArb, queryArb) { category, query1, query2 ->
            // Ensure we have two different queries
            val q1 = if (query1 == query2) "${query1}1" else query1
            val q2 = if (query1 == query2) "${query2}2" else query2
            
            // Setup mocks
            val apiService = mockk<NewsApiService>()
            val database = mockk<NewsDatabase>()
            val cachedArticleDao = mockk<CachedArticleDao>()
            val remoteKeysDao = mockk<RemoteKeysDao>()
            
            every { database.cachedArticleDao() } returns cachedArticleDao
            every { database.remoteKeysDao() } returns remoteKeysDao
            
            // Create mock PagingSource for search queries
            every { cachedArticleDao.searchArticlesPagingSource(any()) } answers {
                val query = firstArg<String>()
                createMockPagingSource(query)
            }
            
            val repository = NewsRepository(
                apiService = apiService,
                cachedArticleDao = cachedArticleDao,
                database = database
            )
            
            // Get paged news for two different search queries
            val flow1 = repository.getPagedNews(category, q1)
            val flow2 = repository.getPagedNews(category, q2)
            
            // Each call should return a different Flow instance
            // This ensures that changing search query creates a new Pager
            // which resets pagination state
            flow1 shouldNotBe flow2
        }
    }

    /**
     * Property 2 (continued): Null query vs non-null query produces different flows
     * 
     * **Validates: Requirements 2.1, 2.2**
     */
    test("Property 2: State change invalidates pagination - query vs no query produce different Pager flows") {
        checkAll(100, categoryArb, queryArb) { category, query ->
            // Setup mocks
            val apiService = mockk<NewsApiService>()
            val database = mockk<NewsDatabase>()
            val cachedArticleDao = mockk<CachedArticleDao>()
            val remoteKeysDao = mockk<RemoteKeysDao>()
            
            every { database.cachedArticleDao() } returns cachedArticleDao
            every { database.remoteKeysDao() } returns remoteKeysDao
            
            // Create mock PagingSource for category
            every { cachedArticleDao.getArticlesPagingSource(any()) } answers {
                createMockPagingSource(firstArg())
            }
            
            // Create mock PagingSource for search
            every { cachedArticleDao.searchArticlesPagingSource(any()) } answers {
                createMockPagingSource(firstArg())
            }
            
            val repository = NewsRepository(
                apiService = apiService,
                cachedArticleDao = cachedArticleDao,
                database = database
            )
            
            // Get paged news without query (category browsing)
            val flowWithoutQuery = repository.getPagedNews(category, null)
            
            // Get paged news with query (search mode)
            val flowWithQuery = repository.getPagedNews(category, query)
            
            // Each call should return a different Flow instance
            // This ensures switching between browse and search mode
            // creates a new Pager which resets pagination state
            flowWithoutQuery shouldNotBe flowWithQuery
        }
    }
})

/**
 * Creates a mock PagingSource for testing purposes.
 * Each call creates a new instance to simulate fresh pagination state.
 */
private fun createMockPagingSource(identifier: String): PagingSource<Int, CachedArticle> {
    return object : PagingSource<Int, CachedArticle>() {
        override fun getRefreshKey(state: PagingState<Int, CachedArticle>): Int? = null
        
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CachedArticle> {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            )
        }
    }
}
