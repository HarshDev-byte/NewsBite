package com.example.newsbite

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newsbite.data.local.BookmarkedArticle
import com.example.newsbite.databinding.BookmarkRowBinding
import com.squareup.picasso.Picasso

class BookmarksAdapter(
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<BookmarkedArticle, BookmarksAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = BookmarkRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookmarkViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position), onRemoveClick)
    }
    
    class BookmarkViewHolder(
        private val binding: BookmarkRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(article: BookmarkedArticle, onRemoveClick: (String) -> Unit) {
            binding.articleTitle.text = article.title
            binding.articleSource.text = article.sourceName
            
            Picasso.get()
                .load(article.urlToImage)
                .error(R.drawable.no_image_icon)
                .placeholder(R.drawable.no_image_icon)
                .into(binding.articleImageView)
            
            binding.removeButton.setOnClickListener {
                onRemoveClick(article.url)
            }
            
            binding.shareButton.setOnClickListener { view ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, article.title)
                    putExtra(Intent.EXTRA_TEXT, "${article.title}\n\nRead more: ${article.url}")
                }
                view.context.startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
            
            itemView.setOnClickListener {
                val intent = Intent(it.context, NewsFullActivity::class.java).apply {
                    putExtra("url", article.url)
                }
                it.context.startActivity(intent)
            }
        }
    }
    
    private class BookmarkDiffCallback : DiffUtil.ItemCallback<BookmarkedArticle>() {
        override fun areItemsTheSame(oldItem: BookmarkedArticle, newItem: BookmarkedArticle): Boolean {
            return oldItem.url == newItem.url
        }
        
        override fun areContentsTheSame(oldItem: BookmarkedArticle, newItem: BookmarkedArticle): Boolean {
            return oldItem.title == newItem.title
        }
    }
}
