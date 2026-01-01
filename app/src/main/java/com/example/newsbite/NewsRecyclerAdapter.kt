package com.example.newsbite

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.newsbite.data.model.Article
import com.example.newsbite.databinding.NewsRecyclerRowBinding
import com.squareup.picasso.Picasso

class NewsRecyclerAdapter(
    private val onBookmarkClick: (Article) -> Unit
) : PagingDataAdapter<Article, NewsRecyclerAdapter.NewsViewHolder>(ArticleDiffCallback()) {
    
    private var bookmarkedUrls: Set<String> = emptySet()
    
    fun setBookmarkedUrls(urls: Set<String>) {
        bookmarkedUrls = urls
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = NewsRecyclerRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NewsViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val article = getItem(position)
        // PagingDataAdapter can return null for placeholder items
        article?.let {
            val isBookmarked = bookmarkedUrls.contains(it.url)
            holder.bind(it, isBookmarked, onBookmarkClick)
        }
    }
    
    class NewsViewHolder(
        private val binding: NewsRecyclerRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(article: Article, isBookmarked: Boolean, onBookmarkClick: (Article) -> Unit) {
            binding.articleTitle.text = article.title
            binding.articleSource.text = article.source?.name
            
            Picasso.get()
                .load(article.urlToImage)
                .error(R.drawable.no_image_icon)
                .placeholder(R.drawable.no_image_icon)
                .into(binding.articleImageView)
            
            binding.bookmarkButton.setImageResource(
                if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
            
            binding.bookmarkButton.setOnClickListener {
                onBookmarkClick(article)
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
                    putExtra("title", article.title)
                    putExtra("description", article.description)
                }
                it.context.startActivity(intent)
            }
        }
    }
    
    companion object {
        private class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
            override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
                return oldItem.url == newItem.url
            }
            
            override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
                return oldItem == newItem
            }
        }
    }
}
