package com.hanto.dragndrop.ui.adapter

import android.annotation.SuppressLint
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
import com.hanto.dragndrop.ui.MainViewModel

// 사용 중인 카테고리 어댑터
class UsingCategoryAdapter(
    private val listener: UsingCategoryClickListener,
    private val viewModel: MainViewModel
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
            Log.d("UsingKindAdapter", "Selecting item at position: $position")

            // 이전 선택 해제
            val previousSelected = selectedPosition
            selectedPosition = position

            // UI 갱신
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)

            listener.onUsingCategoryClick(getItem(position))
        } else if (position == RecyclerView.NO_POSITION) {

            val previousSelected = selectedPosition
            if (previousSelected != RecyclerView.NO_POSITION) {
                selectedPosition = RecyclerView.NO_POSITION
                notifyItemChanged(previousSelected)

                Log.d("UsingKindAdapter", "Deselecting all items")
            }
        }
    }

    fun selectItem(position: Int) {
        if (position == selectedPosition) return

        val previousSelected = selectedPosition
        selectedPosition = position

        if (previousSelected != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelected)
        }
        notifyItemChanged(selectedPosition)
    }

    override fun onRemove(item: UsingCategory) {
        viewModel.removeCategory(item.category.id)
        Log.d("UsingCategoryAdapter", "카테고리 삭제: ${item.category.categoryName}")
    }

    override fun onSwap(from: Int, to: Int) {
        Log.d("UsingCategoryAdapter", "onSwap: from=$from, to=$to")
        viewModel.swapUsingCategories(from, to)
    }

    // UsingCategoryAdapter의 onAdd 함수 수정
    override fun onAdd(item: UsingCategory) {
        val existingIndex = currentList.indexOfFirst { it.category.id == item.category.id }
        if (existingIndex == -1) {
            viewModel.addCategoryToUsing(item.category)
            Log.d("UsingCategoryAdapter", "UsingCategory 추가됨: ${item.category.categoryName}")
        } else {
            // 이미 존재하는 경우 선택 상태로 변경
            viewModel.selectUsingCategory(item)
            Log.d("UsingCategoryAdapter", "기존 UsingCategory 선택됨: ${item.category.categoryName}")
        }

    }


    inner class UsingCategoryViewHolder(private val binding: ItemUsingCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    selectItem(position)
                    listener.onUsingCategoryClick(getItem(position))
                }
            }

            // 분류 아이템에서는 드래그 시작 가능
            binding.root.setOnLongClickListener { view ->
                // rv_using_category에서만 드래그 가능
                if ((view.parent as? RecyclerView)?.id == R.id.rv_using_category) {
                    startDragCompatible(view)
                }
                true
            }

            binding.root.setOnDragListener(dragListener)
        }

        fun bind(usingCategory: UsingCategory, isSelected: Boolean) {
            binding.tvCategoryName.text = usingCategory.category.categoryName

            if (isSelected) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.selected_color)
                )
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                )
            }
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

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(
                oldItem: UsingCategory,
                newItem: UsingCategory
            ): Boolean {
                return oldItem.category == newItem.category &&
                        oldItem.selectedProducts == newItem.selectedProducts
            }
        }
    }
}