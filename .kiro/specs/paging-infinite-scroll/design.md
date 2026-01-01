# Design Document: Paging 3 with Infinite Scroll

## Overview

This design implements infinite scroll pagination using Android's Paging 3 library. The implementation uses a `RemoteMediator` pattern to support offline-first behavior - fetching from network and caching to Room database, with automatic fallback to cached data when offline.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌─────────────────┐    ┌──────────────────────────────┐   │
│  │  MainActivity   │───▶│  PagingDataAdapter           │   │
│  │                 │    │  + LoadStateAdapter (footer) │   │
│  └────────┬────────┘    └──────────────────────────────┘   │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────┐                                        │
│  │  NewsViewModel  │◀── Flow<PagingData<Article>>           │
│  └────────┬────────┘                                        │
└───────────┼─────────────────────────────────────────────────┘
            │
┌───────────┼─────────────────────────────────────────────────┐
│           ▼              Data Layer                          │
│  ┌─────────────────┐                                        │
│  │ NewsRepository  │                                        │
│  └────────┬────────┘                                        │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │     Pager       │───▶│ RemoteMediator  │                │
│  │  (PagingConfig) │    │ (Network+Cache) │                │
│  └────────┬────────┘    └────────┬────────┘                │
│           │                      │                          │
│           ▼                      ▼                          │
│  ┌─────────────────┐    ┌─────────────────┐                │
│  │  PagingSource   │    │  NewsApiService │                │
│  │ (from Room)     │    │  (Retrofit)     │                │
│  └────────┬────────┘    └─────────────────┘                │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────┐                                        │
│  │  Room Database  │                                        │
│  │ (CachedArticle) │                                        │
│  └─────────────────┘                                        │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. NewsRemoteMediator

Handles fetching from network and caching to database.

```kotlin
@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val category: String,
    private val query: String?,
    private val apiService: NewsApiService,
    private val database: NewsDatabase
) : RemoteMediator<Int, CachedArticle>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedArticle>
    ): MediatorResult {
        // Handle REFRESH, PREPEND, APPEND
        // Fetch from API, save to Room
        // Return MediatorResult.Success or Error
    }
}
```

### 2. Updated CachedArticleDao

Add PagingSource query for Room.

```kotlin
@Dao
interface CachedArticleDao {
    @Query("SELECT * FROM cached_articles WHERE category = :category ORDER BY cachedAt DESC")
    fun getArticlesPagingSource(category: String): PagingSource<Int, CachedArticle>
    
    @Query("SELECT * FROM cached_articles WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY cachedAt DESC")
    fun searchArticlesPagingSource(query: String): PagingSource<Int, CachedArticle>
}
```

### 3. Updated NewsRepository

Returns `Flow<PagingData<Article>>` instead of `Resource<List<Article>>`.

```kotlin
class NewsRepository @Inject constructor(
    private val apiService: NewsApiService,
    private val database: NewsDatabase
) {
    fun getPagedNews(category: String, query: String?): Flow<PagingData<Article>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            remoteMediator = NewsRemoteMediator(category, query, apiService, database),
            pagingSourceFactory = { database.cachedArticleDao().getArticlesPagingSource(category) }
        ).flow.map { pagingData ->
            pagingData.map { it.toArticle() }
        }
    }
}
```

### 4. NewsPagingAdapter

Extends `PagingDataAdapter` instead of `ListAdapter`.

```kotlin
class NewsPagingAdapter(
    private val onBookmarkClick: (Article) -> Unit
) : PagingDataAdapter<Article, NewsPagingAdapter.NewsViewHolder>(ArticleDiffCallback()) {
    // Similar to current adapter but extends PagingDataAdapter
}
```

### 5. LoadStateAdapter

Shows loading/error footer at bottom of list.

```kotlin
class NewsLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<NewsLoadStateAdapter.LoadStateViewHolder>() {
    
    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }
}
```

### 6. Updated NewsViewModel

Exposes `Flow<PagingData<Article>>` and handles category/search changes.

```kotlin
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: NewsRepository
) : ViewModel() {
    
    private val _currentCategory = MutableStateFlow("general")
    private val _currentQuery = MutableStateFlow<String?>(null)
    
    val pagedNews: Flow<PagingData<Article>> = combine(
        _currentCategory,
        _currentQuery
    ) { category, query ->
        Pair(category, query)
    }.flatMapLatest { (category, query) ->
        repository.getPagedNews(category, query)
    }.cachedIn(viewModelScope)
}
```

## Data Models

### RemoteKeys Entity (for page tracking)

```kotlin
@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey
    val articleUrl: String,
    val prevKey: Int?,
    val nextKey: Int?,
    val category: String
)
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Pagination triggers on scroll threshold

*For any* scroll position near the end of the loaded list (within prefetchDistance), the PagingSource SHALL request the next page of data.

**Validates: Requirements 1.1**

### Property 2: State change invalidates pagination

*For any* category change, search query change, or pull-to-refresh action, the current PagingData SHALL be invalidated and pagination SHALL restart from page 1.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 3: Offline fallback to cache

*For any* network failure when cached articles exist for the requested category, the System SHALL return cached articles from the Room database.

**Validates: Requirements 3.1**

### Property 4: Network success updates cache

*For any* successful API response, the fetched articles SHALL be persisted to the Room database before being displayed.

**Validates: Requirements 3.3**

### Property 5: Cache prevents redundant fetches

*For any* scroll to previously loaded pages, the System SHALL NOT make additional network requests for those pages.

**Validates: Requirements 4.2**

## Error Handling

| Scenario | Handling |
|----------|----------|
| Initial load fails | Show full-screen error with retry button |
| Append/prepend fails | Show error in footer with retry button |
| Network timeout | Fall back to cache if available |
| Empty response | Show "No more articles" in footer |
| Invalid API key | Show error message, disable retry |

## Testing Strategy

### Unit Tests
- Test RemoteMediator load logic for REFRESH, APPEND, PREPEND
- Test PagingConfig values (pageSize = 20, prefetchDistance = 5)
- Test category/query change triggers new Pager creation
- Test CachedArticle to Article mapping

### Property-Based Tests
- **Property 2**: Generate random sequences of category changes and verify pagination resets each time
- **Property 3**: Generate random network failure scenarios with varying cache states
- **Property 4**: Generate random API responses and verify database contains all articles

### Integration Tests
- Test full flow: scroll → load → cache → display
- Test offline mode with pre-populated cache
- Test LoadState changes reflect in UI

## Migration Notes

1. Keep existing `Resource` class for non-paged operations (bookmarks)
2. Adapter change: `ListAdapter` → `PagingDataAdapter`
3. ViewModel change: `LiveData<Resource<List>>` → `Flow<PagingData>`
4. Add `ConcatAdapter` to combine news adapter with load state adapter
5. Database version bump required for RemoteKeys table
