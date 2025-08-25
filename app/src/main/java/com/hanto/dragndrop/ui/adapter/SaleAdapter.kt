package com.hanto.dragndrop.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.SaleItem
import com.hanto.dragndrop.databinding.ItemCategoryBinding
import com.hanto.dragndrop.databinding.ItemProductBinding

private const val VIEW_TYPE_CATEGORY = 0
private const val VIEW_TYPE_PRODUCT = 1

class SaleAdapter(
    private val listener: SaleItemClickListener,
    private val callback: DragDropCallback
) : RecyclerViewDragAdapter<SaleItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    private val TAG = "SaleAdapter"

    override val isSwappable: Boolean = true
    val currentItems = mutableListOf<SaleItem>()
    private val currentCategoryItems = mutableListOf<CategoryItem>()

    private var selectedItemId: String? = null
    private val inUseItemIds = mutableSetOf<String>()
    private val selectedCategoryProductIds = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> CategoryViewHolder.from(parent, listener, this)
            VIEW_TYPE_PRODUCT -> ProductViewHolder.from(parent, listener, this)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> {
                val item = currentItems[position] as CategoryItem
                holder.bind(item, isSelected(item), isItemInUse(item.id))
            }

            is ProductViewHolder -> {
                val item = currentItems[position] as ProductItem
                holder.bind(item, isSelected(item), isProductInSelectedCategory(item.id))
            }
        }
    }

    // 개선된 Payload 기반 부분 업데이트
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (holder) {
                is CategoryViewHolder -> {
                    val item = currentItems[position] as CategoryItem
                    holder.updateSelectionState(isSelected(item), isItemInUse(item.id))
                }

                is ProductViewHolder -> {
                    val item = currentItems[position] as ProductItem
                    holder.updateSelectionState(
                        isSelected(item),
                        isProductInSelectedCategory(item.id)
                    )
                }
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    companion object {
        // Payload 상수 정의
        const val PAYLOAD_SELECTION_CHANGED = "selection_changed"
        const val PAYLOAD_IN_USE_CHANGED = "in_use_changed"

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaleItem>() {
            override fun areItemsTheSame(oldItem: SaleItem, newItem: SaleItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SaleItem, newItem: SaleItem): Boolean {
                // 명시적 타입 체크로 equals() 경고 해결
                if (oldItem::class != newItem::class) return false

                return when {
                    oldItem is CategoryItem && newItem is CategoryItem ->
                        oldItem.id == newItem.id && oldItem.categoryName == newItem.categoryName

                    oldItem is ProductItem && newItem is ProductItem ->
                        oldItem.id == newItem.id &&
                                oldItem.prName == newItem.prName &&
                                oldItem.categoryId == newItem.categoryId

                    else -> false
                }
            }

            override fun getChangePayload(oldItem: SaleItem, newItem: SaleItem): Any? {
                return PAYLOAD_SELECTION_CHANGED
            }
        }
    }

    override fun onAdd(item: SaleItem) {
        when (item) {
            is CategoryItem -> {
                callback.onCategoryAdded(item)
                Log.d(TAG, "CategoryItem ${item.categoryName} 추가됨")
            }

            is ProductItem -> {
                callback.onProductAdded(item)
                Log.d(TAG, "ProductItem ${item.prName} 추가됨")
            }
        }
    }

    override fun onRemove(item: SaleItem) {
        // 구현 필요 없음 - 이 어댑터에서는 제거 동작 없음
    }

    override fun onSwap(from: Int, to: Int) {
        // 구현 필요 없음 - 이 어댑터에서는 스와핑 동작 없음
    }

    fun selectItemOptimized(item: SaleItem) {
        val previousSelectedId = selectedItemId

        // 토글 방식 제거 - 항상 선택 상태로 설정
        selectedItemId = item.id

        Log.d(
            TAG,
            "selectItem: ${item.id}, previousSelected: $previousSelectedId, newSelected: $selectedItemId"
        )

        // 이전 선택 아이템 해제
        if (previousSelectedId != null && previousSelectedId != item.id) {
            val previousPosition = currentItems.indexOfFirst { it.id == previousSelectedId }
            if (previousPosition != RecyclerView.NO_POSITION) {
                Log.d(TAG, "이전 선택 해제: position=$previousPosition")
                notifyItemChanged(previousPosition, PAYLOAD_SELECTION_CHANGED)
            }
        }

        // 새 선택 아이템 적용 (이전과 다른 경우만)
        if (previousSelectedId != item.id) {
            val newPosition = currentItems.indexOfFirst { it.id == item.id }
            if (newPosition != RecyclerView.NO_POSITION) {
                Log.d(TAG, "새 선택 적용: position=$newPosition")
                notifyItemChanged(newPosition, PAYLOAD_SELECTION_CHANGED)
            }
        }
    }

    fun updateInUseItemsOptimized(ids: Set<String>) {
        val oldInUseIds = inUseItemIds.toSet()
        inUseItemIds.clear()
        inUseItemIds.addAll(ids)

        // 변경된 아이템만 업데이트
        currentItems.forEachIndexed { index, item ->
            val wasInUse = oldInUseIds.contains(item.id)
            val isInUse = ids.contains(item.id)

            if (wasInUse != isInUse) {
                notifyItemChanged(index, PAYLOAD_IN_USE_CHANGED)
            }
        }
    }

    // 기존 메서드명 유지 (호환성)
    fun selectItem(item: SaleItem) = selectItemOptimized(item)
    fun updateInUseItems(ids: Set<String>) = updateInUseItemsOptimized(ids)

    private fun isItemInUse(id: String): Boolean = inUseItemIds.contains(id)
    private fun isProductInSelectedCategory(prCode: String): Boolean =
        selectedCategoryProductIds.contains(prCode)

    private fun isSelected(item: SaleItem): Boolean = item.id == selectedItemId

    override fun getItemCount(): Int = currentItems.size

    override fun getItemViewType(position: Int): Int {
        return when (currentItems[position]) {
            is CategoryItem -> VIEW_TYPE_CATEGORY
            is ProductItem -> VIEW_TYPE_PRODUCT
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    fun findPositionById(id: String): Int = currentItems.indexOfFirst { it.id == id }
    fun findCategoryById(id: String): CategoryItem? = currentCategoryItems.find { it.id == id }
    fun findProductById(id: String): ProductItem? =
        currentItems.find { it is ProductItem && it.id == id } as? ProductItem

    fun submitCategories(categoryItemList: List<CategoryItem>) {
        currentCategoryItems.clear()
        currentCategoryItems.addAll(categoryItemList)
        updateItemsOptimized(categoryItemList)
    }

    fun submitProducts(products: List<ProductItem>) {
        updateItemsOptimized(products)
    }

    fun updateSelectedCategoryProducts(prCode: Set<String>) {
        selectedCategoryProductIds.clear()
        selectedCategoryProductIds.addAll(prCode)
        notifyDataSetChanged()
    }

    private fun updateItemsOptimized(newItems: List<SaleItem>) {
        val oldItems = currentItems.toList()
        currentItems.clear()
        currentItems.addAll(newItems)

        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldItems[oldItemPosition].id == newItems[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldItem = oldItems[oldItemPosition]
                val newItem = newItems[newItemPosition]

                return when {
                    oldItem is CategoryItem && newItem is CategoryItem ->
                        oldItem.categoryName == newItem.categoryName

                    oldItem is ProductItem && newItem is ProductItem ->
                        oldItem.prName == newItem.prName && oldItem.categoryId == newItem.categoryId

                    else -> oldItem == newItem
                }
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                return PAYLOAD_SELECTION_CHANGED
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
    }

    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val listener: SaleItemClickListener,
        private val adapter: SaleAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCategory: CategoryItem? = null

        fun bind(category: CategoryItem, isSelected: Boolean, isInUse: Boolean) {
            currentCategory = category

            Log.d("CategoryViewHolder", "bind: ${category.categoryName}, isSelected: $isSelected")

            // 텍스트는 카테고리가 다를 때만 변경
            if (binding.tvCategoryName.text != category.categoryName) {
                binding.tvCategoryName.text = category.categoryName
            }

            updateSelectionState(isSelected, isInUse)

            binding.category = category
            binding.listener = listener
            binding.isSelected = isSelected
            binding.executePendingBindings()
        }

        // 부분 업데이트 메서드
        fun updateSelectionState(isSelected: Boolean, isInUse: Boolean) {
            Log.d(
                "CategoryViewHolder",
                "updateSelectionState: isSelected=$isSelected, isInUse=$isInUse"
            )

            val textColor = if (isInUse) {
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.black)
            }

            val backgroundColor = if (isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.selected_color)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            }

            binding.tvCategoryName.setTextColor(textColor)
            binding.root.setBackgroundColor(backgroundColor)

            Log.d("CategoryViewHolder", "배경색 변경 완료: ${if (isSelected) "선택됨" else "선택해제"}")
        }

        init {
            binding.root.setOnLongClickListener { view ->
                adapter.startDragCompatible(view)
                true
            }
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

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val listener: SaleItemClickListener,
        private val adapter: SaleAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentProduct: ProductItem? = null

        fun bind(product: ProductItem, isSelected: Boolean, isInUse: Boolean) {
            currentProduct = product

            // 텍스트는 제품이 다를 때만 변경
            if (binding.tvProduct.text != product.prName) {
                binding.tvProduct.text = product.prName
            }

            updateSelectionState(isSelected, isInUse)

            binding.product = product
            binding.listener = listener
            binding.isSelected = isSelected
            binding.executePendingBindings()
        }

        // 부분 업데이트 메서드
        fun updateSelectionState(isSelected: Boolean, isInUse: Boolean) {
            val textColor = if (isInUse) {
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.black)
            }

            val backgroundColor = if (isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.selected_color)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            }

            binding.tvProduct.setTextColor(textColor)
            binding.root.setBackgroundColor(backgroundColor)
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