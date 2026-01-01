package com.example.newsbite

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsbite.data.repository.BookmarkRepository
import com.example.newsbite.databinding.ActivityBookmarksBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BookmarksActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBookmarksBinding
    private lateinit var adapter: BookmarksAdapter
    
    @Inject
    lateinit var bookmarkRepository: BookmarkRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.bookmarksRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }
        
        setupRecyclerView()
        setupClickListeners()
        observeBookmarks()
    }
    
    private fun setupRecyclerView() {
        binding.bookmarksRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BookmarksAdapter { url ->
            bookmarkRepository.removeBookmark(url)
        }
        binding.bookmarksRecyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
    }
    
    private fun observeBookmarks() {
        bookmarkRepository.getAllBookmarks().observe(this) { bookmarks ->
            if (bookmarks.isNullOrEmpty()) {
                binding.emptyLayout.visibility = View.VISIBLE
                binding.bookmarksRecyclerView.visibility = View.GONE
            } else {
                binding.emptyLayout.visibility = View.GONE
                binding.bookmarksRecyclerView.visibility = View.VISIBLE
                adapter.submitList(bookmarks)
            }
        }
    }
}
