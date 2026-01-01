package com.example.newsbite

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newsbite.databinding.LoadStateFooterBinding

class NewsLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<NewsLoadStateAdapter.LoadStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = LoadStateFooterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LoadStateViewHolder(binding, retry)
    }

    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    class LoadStateViewHolder(
        private val binding: LoadStateFooterBinding,
        private val retry: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.retryButton.setOnClickListener { retry() }
        }

        fun bind(loadState: LoadState) {
            binding.progressBar.isVisible = loadState is LoadState.Loading
            binding.errorLayout.isVisible = loadState is LoadState.Error

            if (loadState is LoadState.Error) {
                binding.errorMessage.text = loadState.error.localizedMessage
                    ?: binding.root.context.getString(R.string.msg_error_generic)
            }
        }
    }
}
