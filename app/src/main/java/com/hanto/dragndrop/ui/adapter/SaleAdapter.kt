package com.hanto.dragndrop.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.SaleItem
import com.hanto.dragndrop.databinding.ItemCategoryBinding
import com.hanto.dragndrop.databinding.ItemProductBinding
import com.hanto.dragndrop.ui.drag.DragCapable
import com.hanto.dragndrop.ui.drag.DragHelper

private const val VIEW_TYPE_CATEGORY = 0
private const val VIEW_TYPE_PRODUCT = 1

class SaleAdapter(
    private val listener: SaleItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DragCapable {

    private val TAG = "SaleAdapter"

    // 데이터 관리
    private val items = mutableListOf<SaleItem>()
    private val categoryItems = mutableListOf<CategoryItem>()

    // 상태 관리
    private var selectedItemId: String? = null
    private var inUseItemIds = setOf<String>()

    override fun isSwappable(): Boolean = false

    override fun getItemForDrag(position: Int): Any? {
        return if (isValidPosition(position)) {
            items[position]
        } else null
    }

    override fun onDragAdd(item: Any) {
        Log.d(TAG, "onDragAdd 호출되었지만 처리하지 않음: $item")
    }

    override fun onDragRemove(item: Any) {
        Log.d(TAG, "onDragRemove 호출되었지만 처리하지 않음: $item")
    }

    override fun onDragSwap(fromPosition: Int, toPosition: Int) {
        Log.d(TAG, "SaleAdapter는 순서 변경 불가")
    }

    private fun isValidPosition(position: Int): Boolean {
        return position >= 0 && position < items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> CategoryViewHolder.from(parent, listener, this)
            VIEW_TYPE_PRODUCT -> ProductViewHolder.from(parent, listener, this)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is CategoryViewHolder -> {
                val categoryItem = item as CategoryItem
                holder.bind(
                    category = categoryItem,
                    isSelected = categoryItem.id == selectedItemId,
                    isInUse = inUseItemIds.contains(categoryItem.id)
                )
            }

            is ProductViewHolder -> {
                val productItem = item as ProductItem
                holder.bind(
                    product = productItem,
                    isSelected = productItem.id == selectedItemId,
                    isInUse = false
                )
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CategoryItem -> VIEW_TYPE_CATEGORY
            is ProductItem -> VIEW_TYPE_PRODUCT
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    /**
     * 카테고리 목록 제출
     */
    fun submitCategories(categories: List<CategoryItem>) {
        Log.d(TAG, "submitCategories 호출 - 아이템 수: ${categories.size}")

        categoryItems.clear()
        categoryItems.addAll(categories)

        items.clear()
        items.addAll(categories)
        notifyDataSetChanged()
    }

    /**
     * 제품 목록 제출
     */
    fun submitProducts(products: List<ProductItem>) {
        Log.d(TAG, "submitProducts 호출 - 아이템 수: ${products.size}")

        items.clear()
        items.addAll(products)
        notifyDataSetChanged()
    }

    /**
     * 아이템 선택 처리
     */
    fun selectItem(item: SaleItem) {
        val previousSelectedId = selectedItemId
        selectedItemId = item.id

        val previousPosition = findItemPositionById(previousSelectedId)
        if (previousPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPosition)
        }


        val newPosition = findItemPositionById(item.id)
        if (newPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(newPosition)
        }
    }

    /**
     * 사용 중인 아이템 업데이트
     */
    fun updateInUseItems(ids: Set<String>) {
        if (inUseItemIds != ids) {
            inUseItemIds = ids
            notifyDataSetChanged()
        }
    }

    private fun findItemPositionById(id: String?): Int {
        return items.indexOfFirst { it.id == id }
    }

    fun findCategoryById(id: String): CategoryItem? {
        return categoryItems.find { it.id == id }
    }

    /**
     * 카테고리 ViewHolder
     */
    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val listener: SaleItemClickListener,
        private val adapter: SaleAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.items[0] as CategoryItem
                    listener.onCategoryClick(item)
                }
            }

            binding.root.setOnLongClickListener { view ->
                if (DragHelper.isDragCapable(view)) {
                    val success = DragHelper.startDrag(view)
                    Log.d("CategoryViewHolder", "드래그 시작: $success")
                    success
                } else {
                    false
                }
            }
        }

        fun bind(category: CategoryItem, isSelected: Boolean, isInUse: Boolean) {
            binding.tvCategoryName.text = category.categoryName

            // 선택 상태 배경
            val backgroundColor = if (isSelected) {
                R.color.row_activated
            } else {
                android.R.color.transparent
            }
            
            binding.root.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, backgroundColor)
            )

            // 사용 중인 항목 텍스트 색상
            val textColor = if (isInUse) {
                android.R.color.holo_red_dark
            } else {
                android.R.color.black
            }
            binding.tvCategoryName.setTextColor(
                ContextCompat.getColor(binding.root.context, textColor)
            )

            // DataBinding 설정
            binding.category = category
            binding.listener = listener
            binding.executePendingBindings()
        }

        companion object {
            fun from(
                parent: ViewGroup,
                listener: SaleItemClickListener,
                adapter: SaleAdapter
            ): CategoryViewHolder {
                val binding = ItemCategoryBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return CategoryViewHolder(binding, listener, adapter)
            }
        }
    }

    /**
     * 제품 ViewHolder
     */
    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val listener: SaleItemClickListener,
        private val adapter: SaleAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.items[position] as ProductItem
                    listener.onProductClick(item)
                }
            }

            binding.root.setOnLongClickListener { view ->
                if (DragHelper.isDragCapable(view)) {
                    DragHelper.startDrag(view)
                } else {
                    false
                }
            }
        }

        fun bind(product: ProductItem, isSelected: Boolean, isInUse: Boolean) {
            binding.tvProduct.text = product.prName

            val backgroundColor = if (isSelected) {
                R.color.selected_color
            } else {
                android.R.color.transparent
            }
            binding.root.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, backgroundColor)
            )

            // DataBinding 설정
            binding.product = product
            binding.listener = listener
            binding.executePendingBindings()
        }

        companion object {
            fun from(
                parent: ViewGroup,
                listener: SaleItemClickListener,
                adapter: SaleAdapter
            ): ProductViewHolder {
                val binding = ItemProductBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return ProductViewHolder(binding, listener, adapter)
            }
        }
    }
}

interface SaleItemClickListener {
    fun onCategoryClick(category: CategoryItem)
    fun onProductClick(product: ProductItem)
}