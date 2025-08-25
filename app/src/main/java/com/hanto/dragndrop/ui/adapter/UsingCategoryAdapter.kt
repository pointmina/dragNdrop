package com.hanto.dragndrop.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.UsingCategory
import com.hanto.dragndrop.databinding.ItemUsingCategoryBinding

class UsingCategoryAdapter(
    private val listener: UsingCategoryClickListener,
    private val callback: DragDropCallback
) : RecyclerViewDragAdapter<UsingCategory, UsingCategoryAdapter.UsingCategoryViewHolder>(
    DIFF_CALLBACK
) {

    override val isSwappable: Boolean = true
    private var selectedPosition = RecyclerView.NO_POSITION

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
        if (position < 0 || position >= currentList.size) {
            return
        }

        holder.bind(getItem(position), position == selectedPosition)
        setLayoutWidth(holder)
    }

    // Payload 지원 바인딩
    override fun onBindViewHolder(
        holder: UsingCategoryViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            holder.updateSelectionState(position == selectedPosition)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    private fun setLayoutWidth(holder: UsingCategoryViewHolder) {
        val displayMetrics = holder.itemView.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val targetWidth = (screenWidth * 0.21).toInt()
        holder.itemView.layoutParams.width = targetWidth
    }

    fun getItemPosition(usingCategory: UsingCategory): Int {
        return currentList.indexOfFirst { it.category.id == usingCategory.category.id }
    }

    fun selectItemAt(position: Int) {
        if (position >= 0 && position < currentList.size) {
            Log.d("UsingCategoryAdapter", "Selecting item at position: $position")

            val previousSelected = selectedPosition
            selectedPosition = position

            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected, "selection_changed")
            }
            notifyItemChanged(selectedPosition, "selection_changed")

            listener.onUsingCategoryClick(getItem(position))
        } else if (position == RecyclerView.NO_POSITION) {
            val previousSelected = selectedPosition
            if (previousSelected != RecyclerView.NO_POSITION) {
                selectedPosition = RecyclerView.NO_POSITION
                notifyItemChanged(previousSelected, "selection_changed")
                Log.d("UsingCategoryAdapter", "Deselecting all items")
            }
        }
    }

    fun selectItem(position: Int) {
        if (position == selectedPosition) return

        val previousSelected = selectedPosition
        selectedPosition = position

        if (previousSelected != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelected, "selection_changed")
        }
        notifyItemChanged(selectedPosition, "selection_changed")
    }

    override fun onRemove(item: UsingCategory) {
        callback.onCategoryRemoved(item.category.id)
        Log.d("UsingCategoryAdapter", "카테고리 삭제: ${item.category.categoryName}")
    }

    override fun onSwap(from: Int, to: Int) {
        Log.d("UsingCategoryAdapter", "onSwap: from=$from, to=$to")
        callback.onCategoriesSwapped(from, to)
    }

    override fun onAdd(item: UsingCategory) {
        val existingIndex = currentList.indexOfFirst { it.category.id == item.category.id }
        if (existingIndex == -1) {
            // 콜백을 통해 ViewModel에 추가 요청
            callback.onCategoryAdded(item.category)
            Log.d("UsingCategoryAdapter", "UsingCategory 추가됨: ${item.category.categoryName}")
        } else {
            // 이미 존재하는 경우 선택 상태로 변경
            selectItemAt(existingIndex)
            Log.d("UsingCategoryAdapter", "기존 UsingCategory 선택됨: ${item.category.categoryName}")
        }
    }

    inner class UsingCategoryViewHolder(private val binding: ItemUsingCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentCategory: UsingCategory? = null

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectItem(position)
                    listener.onUsingCategoryClick(getItem(position))
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
            currentCategory = usingCategory

            // 텍스트는 카테고리가 다를 때만 변경
            if (binding.tvCategoryName.text != usingCategory.category.categoryName) {
                binding.tvCategoryName.text = usingCategory.category.categoryName
            }

            updateSelectionState(isSelected)
        }

        fun updateSelectionState(isSelected: Boolean) {
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
                if (oldItem.category.id != newItem.category.id ||
                    oldItem.category.categoryName != newItem.category.categoryName
                ) {
                    return false
                }

                if (oldItem.selectedProducts.size != newItem.selectedProducts.size) {
                    return false
                }

                return oldItem.selectedProducts.zip(newItem.selectedProducts).all { (old, new) ->
                    old.id == new.id && old.prName == new.prName
                }
            }

            override fun getChangePayload(oldItem: UsingCategory, newItem: UsingCategory): Any? {
                return "selection_changed"
            }
        }
    }
}