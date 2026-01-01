package com.example.newsbite.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.newsbite.data.api.NewsApiService
import com.example.newsbite.data.local.CachedArticle
import com.example.newsbite.data.local.CachedArticleDao
import com.example.newsbite.data.local.NewsDatabase
import com.example.newsbite.data.local.RemoteKeys
import com.example.newsbite.data.local.RemoteKeysDao
import com.example.newsbite.data.model.Article
import com.example.newsbite.data.model.NewsResponse
import com.example.newsbite.data.model.Source
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import retrofit2.Response
import java.io.IOException

/**
 * Property-based tests for NewsRemoteMediator
 * 
 * Feature: paging-infinite-scroll
 */
@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediatorPropertyTest : FunSpec({

    // Generators for property-based testing
    val categoryArb = Arb.string(minSize = 1, maxSize = 20)
    
    val cachedArticleArb = arbitrary { rs ->
        CachedArticle(
            url = "https://example.com/${Arb.string(5, 20).bind()}",
            title = Arb.string(5, 50).bind(),
            description = Arb.string(10, 100).bind(),
            urlToImage = "https://example.com/image.jpg",
            sourceName = Arb.string(3, 15).bind(),
            publishedAt = "2024-01-01T00:00:00Z",
            category = Arb.string(3, 15).bind(),
            cachedAt = System.currentTimeMillis()
        )
    }

    /**
     * Property 3: Offline fallback to cache
     * 
     * *For any* network failure when cached articles exist for the requested category,
     * the System SHALL return cached articles from the Room database.
     * 
     * **Validates: Requirements 3.1**
     * 
     * This property verifies that when a network error occurs (IOException),
     * the RemoteMediator returns an Error result, allowing the PagingSource
     * to serve cached data from Room.
     */
    test("Property 3: Offline fallback - network failure returns MediatorResult.Error allowing cache fallback") {
        checkAll(100, categoryArb) { category ->
            // Setup mocks
            val apiService = mockk<NewsApiService>()
            val database = mockk<NewsDatabase>()
            val cachedArticleDao = mockk<CachedArticleDao>()
            val remoteKeysDao = mockk<RemoteKeysDao>()
            
            every { database.cachedArticleDao() } returns cachedArticleDao
            every { database.remoteKeysDao() } returns remoteKeysDao
            
            // Simulate network failure
            coEvery { 
                apiService.getTopHeadlines(any(), any(), any(), any(), any(), any()) 
            } throws IOException("Network unavailable")
            
            // Cache has valid data (not stale)
            coEvery { cachedArticleDao.getLastCacheTime(category) } returns System.currentTimeMillis()
            
            val mediator = NewsRemoteMediator(
                category = category,
                query = null,
                apiService = apiService,
                database = database
            )
            
            val pagingState = PagingState<Int, CachedArticle>(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0
            )
            
            // When network fails, RemoteMediator should return Error
            // This allows the PagingSource (Room) to serve cached data
            val result = mediator.load(LoadType.REFRESH, pagingState)
            
            result.shouldBeInstanceOf<RemoteMediator.MediatorResult.Error>()
        }
    }


    /**
     * Property 4: Network success updates cache
     * 
     * *For any* successful API response, the fetched articles SHALL be persisted
     * to the Room database before being displayed.
     * 
     * **Validates: Requirements 3.3**
     * 
     * This property verifies that when the API returns articles successfully,
     * they are saved to the database within a transaction.
     */
    test("Property 4: Network success updates cache - articles are persisted to database") {
        checkAll(100, categoryArb, Arb.int(1, 10)) { category, articleCount ->
            // Setup mocks
            val apiService = mockk<NewsApiService>()
            val database = mockk<NewsDatabase>(relaxed = true)
            val cachedArticleDao = mockk<CachedArticleDao>(relaxed = true)
            val remoteKeysDao = mockk<RemoteKeysDao>(relaxed = true)
            
            every { database.cachedArticleDao() } returns cachedArticleDao
            every { database.remoteKeysDao() } returns remoteKeysDao
            
            // Generate random articles from API
            val apiArticles = (1..articleCount).map { i ->
                Article(
                    title = "Article $i",
                    description = "Description $i",
                    url = "https://example.com/article-$i-${System.nanoTime()}",
                    urlToImage = "https://example.com/image$i.jpg",
                    publishedAt = "2024-01-01T00:00:00Z",
                    source = Source(id = "source$i", name = "Source $i")
                )
            }
            
            val newsResponse = NewsResponse(
                status = "ok",
                totalResults = articleCount,
                articles = apiArticles
            )
            
            coEvery { 
                apiService.getTopHeadlines(any(), any(), any(), any(), any(), any()) 
            } returns Response.success(newsResponse)
            
            // Cache is stale to trigger refresh
            coEvery { cachedArticleDao.getLastCacheTime(category) } returns 0L
            
            // Capture inserted articles
            val insertedArticlesSlot = slot<List<CachedArticle>>()
            coEvery { cachedArticleDao.insertAll(capture(insertedArticlesSlot)) } returns Unit
            
            // Capture inserted remote keys
            val insertedKeysSlot = slot<List<RemoteKeys>>()
            coEvery { remoteKeysDao.insertAll(capture(insertedKeysSlot)) } returns Unit
            
            // Mock database transaction
            coEvery { database.withTransaction(any<suspend () -> Any>()) } coAnswers {
                val block = firstArg<suspend () -> Any>()
                block()
            }
            
            val mediator = NewsRemoteMediator(
                category = category,
                query = null,
                apiService = apiService,
                database = database
            )
            
            val pagingState = PagingState<Int, CachedArticle>(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0
            )
            
            val result = mediator.load(LoadType.REFRESH, pagingState)
            
            // Verify success
            result.shouldBeInstanceOf<RemoteMediator.MediatorResult.Success>()
            
            // Verify articles were inserted into cache
            coVerify { cachedArticleDao.insertAll(any()) }
            
            // Verify the correct number of articles were cached
            insertedArticlesSlot.captured.size shouldBe articleCount
            
            // Verify remote keys were also inserted
            coVerify { remoteKeysDao.insertAll(any()) }
            insertedKeysSlot.captured.size shouldBe articleCount
            
            // Verify each cached article has the correct category
            insertedArticlesSlot.captured.forEach { article ->
                article.category shouldBe category
            }
        }
    }
})
