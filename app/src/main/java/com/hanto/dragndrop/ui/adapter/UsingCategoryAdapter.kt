package com.hanto.dragndrop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.UsingCategory
import com.hanto.dragndrop.databinding.ItemUsingCategoryBinding
import com.hanto.dragndrop.ui.MainViewModel

class UsingCategoryAdapter(
    private val listener: UsingCategoryClickListener,
    private val viewModel: MainViewModel
) : RecyclerViewDragAdapter<UsingCategory, UsingCategoryAdapter.UsingCategoryViewHolder>(
    DIFF_CALLBACK
) {

    override val isSwappable: Boolean = true
    private var selectedPosition = -1

    interface UsingCategoryClickListener {
        fun onUsingCategoryClick(usingCategory: UsingCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsingCategoryViewHolder {
        val binding = ItemUsingCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UsingCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsingCategoryViewHolder, position: Int) {
        if (position >= 0 && position < currentList.size) {
            holder.bind(getItem(position), position == selectedPosition)
            setLayoutWidth(holder)
        }
    }

    private fun setLayoutWidth(holder: UsingCategoryViewHolder) {
        val displayMetrics = holder.itemView.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val targetWidth = (screenWidth * 0.21).toInt()
        holder.itemView.layoutParams.width = targetWidth
    }

    fun setSelectedPosition(position: Int) {
        if (selectedPosition != position) {
            val previousSelected = selectedPosition
            selectedPosition = position

            if (previousSelected >= 0) {
                notifyItemChanged(previousSelected)
            }
            if (position >= 0) {
                notifyItemChanged(position)
            }
        }
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun getItemPosition(usingCategory: UsingCategory): Int {
        return currentList.indexOfFirst { it.category.id == usingCategory.category.id }
    }

    fun selectItemAt(position: Int) {
        setSelectedPosition(position)
        if (position >= 0 && position < currentList.size) {
            listener.onUsingCategoryClick(getItem(position))
        }
    }

    fun selectItemAtSilent(position: Int) {
        setSelectedPosition(position)
    }

    fun selectItem(position: Int) {
        setSelectedPosition(position)
    }

    override fun onAdd(item: UsingCategory) {
        viewModel.addCategoryToUsing(item.category)
    }

    override fun onRemove(item: UsingCategory) {
        viewModel.removeCategory(item.category.id)
    }

    override fun onSwap(from: Int, to: Int) {
        viewModel.swapUsingCategories(from, to)
    }

    inner class UsingCategoryViewHolder(private val binding: ItemUsingCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectItemAt(position)
                }
            }

            binding.root.setOnLongClickListener { view ->
                if ((view.parent as? RecyclerView)?.id == R.id.rv_using_category) {
                    startDragCompatible(view)
                }
                true
            }

            binding.root.setOnDragListener(dragListener)
        }

        fun bind(usingCategory: UsingCategory, isSelected: Boolean) {
            binding.tvCategoryName.text = usingCategory.category.categoryName

            val backgroundColor = if (isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.selected_color)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            }
            binding.root.setBackgroundColor(backgroundColor)
        }
    }

    companion object {
        fun createFromCategoryItem(categoryItem: CategoryItem): UsingCategory {
            return UsingCategory(categoryItem, mutableListOf())
        }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<UsingCategory>() {
            override fun areItemsTheSame(oldItem: UsingCategory, newItem: UsingCategory): Boolean {
                return oldItem.category.id == newItem.category.id
            }

            override fun areContentsTheSame(
                oldItem: UsingCategory,
                newItem: UsingCategory
            ): Boolean {
                return oldItem.category.categoryName == newItem.category.categoryName &&
                        oldItem.selectedProducts.size == newItem.selectedProducts.size
            }
        }
    }
}