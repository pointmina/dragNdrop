package com.hanto.dragndrop.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.UsingCategory
import com.hanto.dragndrop.databinding.ItemUsingCategoryBinding
import com.hanto.dragndrop.ui.drag.DragCapable
import com.hanto.dragndrop.ui.drag.DragHelper
import com.hanto.dragndrop.ui.MainViewModel
import java.util.Collections

class UsingCategoryAdapter(
    private val listener: UsingCategoryClickListener,
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<UsingCategoryAdapter.UsingCategoryViewHolder>(), DragCapable {

    private val TAG = "UsingCategoryAdapter"

    // 데이터 관리
    private val items = mutableListOf<UsingCategory>()
    private var selectedPosition = RecyclerView.NO_POSITION

    interface UsingCategoryClickListener {
        fun onUsingCategoryClick(usingCategory: UsingCategory)
    }

    // DragCapable 구현
    override fun isSwappable(): Boolean = true

    override fun getItemForDrag(position: Int): Any? {
        return if (isValidPosition(position)) {
            items[position]
        } else null
    }

    override fun onDragAdd(item: Any) {
        when (item) {
            is UsingCategory -> {
                val existingIndex = items.indexOfFirst { it.category.id == item.category.id }
                if (existingIndex == -1) {
                    items.add(item)
                    notifyItemInserted(items.size - 1)
                    viewModel.addCategoryToUsing(item.category)
                    Log.d(TAG, "UsingCategory 추가: ${item.category.categoryName}")
                } else {
                    setSelectedPosition(existingIndex)
                    viewModel.selectUsingCategory(item)
                    Log.d(TAG, "기존 UsingCategory 선택: ${item.category.categoryName}")
                }
            }

            is CategoryItem -> {
                val usingCategory = UsingCategory(item, mutableListOf())
                onDragAdd(usingCategory)
            }
        }
    }

    override fun onDragRemove(item: Any) {
        if (item is UsingCategory) {
            val position = items.indexOfFirst { it.category.id == item.category.id }
            if (position >= 0) {
                items.removeAt(position)
                notifyItemRemoved(position)

                // 선택된 위치 조정
                if (selectedPosition >= position) {
                    selectedPosition = if (selectedPosition == position) {
                        RecyclerView.NO_POSITION
                    } else {
                        selectedPosition - 1
                    }
                }

                viewModel.removeCategory(item.category.id)
                Log.d(TAG, "UsingCategory 제거: ${item.category.categoryName}")
            }
        }
    }

    override fun onDragSwap(fromPosition: Int, toPosition: Int) {
        if (isValidPosition(fromPosition) && isValidPosition(toPosition)) {
            Collections.swap(items, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)

            viewModel.swapUsingCategories(fromPosition, toPosition)
        }
    }

    private fun isValidPosition(position: Int): Boolean {
        return position >= 0 && position < items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsingCategoryViewHolder {
        val binding = ItemUsingCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UsingCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsingCategoryViewHolder, position: Int) {
        if (!isValidPosition(position)) return

        holder.bind(items[position], position == selectedPosition)
        setDynamicLayoutWidth(holder)
    }

    override fun getItemCount(): Int = items.size

    /**
     * 목록 업데이트 (기존 submitList 대체)
     */
    fun submitList(newItems: List<UsingCategory>) {
        Log.d(TAG, "submitList 호출 - 아이템 수: ${newItems.size}")

        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()

        Log.d(TAG, "submitList 완료")
    }

    /**
     * 화면 크기에 따라 동적으로 아이템 너비 설정
     */
    private fun setDynamicLayoutWidth(holder: UsingCategoryViewHolder) {
        val displayMetrics = holder.itemView.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val targetWidth = (screenWidth * 0.21).toInt()

        holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
            width = targetWidth
        }
    }

    /**
     * 선택된 위치 설정
     */
    fun setSelectedPosition(position: Int) {
        if (selectedPosition != position) {
            val previousSelected = selectedPosition
            selectedPosition = position

            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position)
            }

            Log.d(TAG, "선택 위치 변경: $previousSelected -> $position")
        }
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun getItemPosition(usingCategory: UsingCategory): Int {
        return items.indexOfFirst { it.category.id == usingCategory.category.id }
    }

    fun selectItemAt(position: Int) {
        if (isValidPosition(position)) {
            setSelectedPosition(position)
            listener.onUsingCategoryClick(items[position])
        }
    }

    fun selectItemAtSilent(position: Int) {
        setSelectedPosition(position)
    }

    fun selectItem(position: Int) {
        setSelectedPosition(position)
    }

    /**
     * ViewHolder 구현
     */
    inner class UsingCategoryViewHolder(
        private val binding: ItemUsingCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (isValidPosition(position)) {
                    selectItemAt(position)
                }
            }

            binding.root.setOnLongClickListener { view ->
                if (DragHelper.isDragCapable(view)) {
                    val success = DragHelper.startDrag(view)
                    Log.d("UsingCategoryViewHolder", "드래그 시작: $success")
                    success
                } else {
                    false
                }
            }
        }

        fun bind(usingCategory: UsingCategory, isSelected: Boolean) {
            binding.tvCategoryName.text = usingCategory.category.categoryName

            val backgroundColor = if (isSelected) {
                R.color.selected_color
            } else {
                android.R.color.transparent
            }
            binding.root.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, backgroundColor)
            )
        }
    }

    companion object {
        /**
         * CategoryItem을 UsingCategory로 변환하는 헬퍼 메소드
         */
        fun createFromCategoryItem(categoryItem: CategoryItem): UsingCategory {
            return UsingCategory(categoryItem, mutableListOf())
        }
    }
}