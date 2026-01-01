# Implementation Plan: Paging 3 with Infinite Scroll

## Overview

This plan implements infinite scroll pagination using Paging 3 library with RemoteMediator for offline-first behavior. Tasks are ordered to build incrementally with working code at each checkpoint.

## Tasks

- [x] 1. Add Paging 3 dependencies
  - Add paging-runtime and paging-common to libs.versions.toml
  - Add dependencies to app/build.gradle.kts
  - _Requirements: 1.1_

- [x] 2. Create RemoteKeys entity and DAO
  - [x] 2.1 Create RemoteKeys data class with @Entity annotation
    - Fields: articleUrl (PrimaryKey), prevKey, nextKey, category
    - _Requirements: 1.1, 2.1_
  - [x] 2.2 Create RemoteKeysDao interface
    - Methods: insertAll, getRemoteKeyByArticleUrl, clearByCategory, clearAll
    - _Requirements: 1.1, 2.1_

- [x] 3. Update database for pagination
  - [x] 3.1 Update NewsDatabase to include RemoteKeys entity
    - Bump database version to 3
    - Add remoteKeysDao() abstract method
    - _Requirements: 1.1_
  - [x] 3.2 Update CachedArticleDao with PagingSource queries
    - Add getArticlesPagingSource(category) returning PagingSource
    - Add searchArticlesPagingSource(query) returning PagingSource
    - _Requirements: 1.1, 2.2_
  - [x] 3.3 Update DatabaseModule to provide RemoteKeysDao
    - _Requirements: 1.1_

- [x] 4. Checkpoint - Verify database changes compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement NewsRemoteMediator
  - [x] 5.1 Create NewsRemoteMediator class
    - Implement RemoteMediator<Int, CachedArticle>
    - Handle LoadType.REFRESH, PREPEND, APPEND
    - Fetch from API, save to Room with RemoteKeys
    - Return MediatorResult.Success or Error
    - _Requirements: 1.1, 1.2, 1.3, 3.1, 3.3_
  - [x] 5.2 Write property test for offline fallback
    - **Property 3: Offline fallback to cache**
    - **Validates: Requirements 3.1**
  - [x] 5.3 Write property test for cache update on success
    - **Property 4: Network success updates cache**
    - **Validates: Requirements 3.3**

- [x] 6. Update NewsRepository for pagination
  - [x] 6.1 Add getPagedNews method returning Flow<PagingData<Article>>
    - Create Pager with PagingConfig (pageSize=20, prefetchDistance=5)
    - Use NewsRemoteMediator
    - Use Room PagingSource as pagingSourceFactory
    - Map CachedArticle to Article
    - _Requirements: 1.1, 1.5, 4.2_
  - [x] 6.2 Write property test for pagination invalidation
    - **Property 2: State change invalidates pagination**
    - **Validates: Requirements 2.1, 2.2, 2.3**

- [x] 7. Checkpoint - Verify repository changes compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Create LoadStateAdapter for footer
  - [x] 8.1 Create load_state_footer.xml layout
    - ProgressBar for loading state
    - Error text + Retry button for error state
    - _Requirements: 1.2, 1.3_
  - [x] 8.2 Create NewsLoadStateAdapter class
    - Extend LoadStateAdapter
    - Bind loading/error states to views
    - Handle retry click callback
    - _Requirements: 1.2, 1.3, 1.4_

- [x] 9. Convert NewsRecyclerAdapter to PagingDataAdapter
  - [x] 9.1 Update NewsRecyclerAdapter to extend PagingDataAdapter
    - Change parent class from ListAdapter to PagingDataAdapter
    - Update method signatures as needed
    - Keep bookmark functionality working
    - _Requirements: 1.1, 4.1_

- [x] 10. Update NewsViewModel for pagination
  - [x] 10.1 Replace LiveData with StateFlow for category/query
    - Use MutableStateFlow for currentCategory and currentQuery
    - _Requirements: 2.1, 2.2_
  - [x] 10.2 Create pagedNews Flow
    - Combine category and query flows
    - FlatMapLatest to repository.getPagedNews()
    - Cache in viewModelScope
    - _Requirements: 1.1, 2.1, 2.2, 2.3_
  - [x] 10.3 Update refresh method to invalidate PagingSource
    - _Requirements: 2.3_

- [x] 11. Checkpoint - Verify ViewModel changes compile
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Update MainActivity for pagination
  - [x] 12.1 Update adapter setup
    - Use ConcatAdapter to combine NewsPagingAdapter with NewsLoadStateAdapter
    - Set up retry callback
    - _Requirements: 1.2, 1.3, 1.4_
  - [x] 12.2 Collect pagedNews Flow
    - Use lifecycleScope.launch with repeatOnLifecycle
    - Submit PagingData to adapter
    - _Requirements: 1.1_
  - [x] 12.3 Update loading state handling
    - Observe adapter.loadStateFlow for initial load states
    - Show shimmer only for initial refresh
    - _Requirements: 1.2_
  - [x] 12.4 Update pull-to-refresh
    - Call adapter.refresh() instead of viewModel.refresh()
    - _Requirements: 2.3_

- [x] 13. Final checkpoint - Full integration test
  - Ensure all tests pass, ask the user if questions arise.
  - Test category switching resets pagination
  - Test search resets pagination
  - Test pull-to-refresh works
  - Test offline mode shows cached data
  - Test error footer appears on network failure

## Notes

- Tasks marked with `*` are optional property-based tests
- Each checkpoint ensures incremental progress compiles
- Existing bookmark functionality must continue working
- Keep Resource class for non-paged operations (bookmarks screen)
