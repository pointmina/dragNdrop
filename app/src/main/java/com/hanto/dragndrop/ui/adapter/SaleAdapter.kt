package com.hanto.dragndrop.ui.adapter

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
import com.hanto.dragndrop.ui.MainViewModel

private const val VIEW_TYPE_CATEGORY = 0
private const val VIEW_TYPE_PRODUCT = 1

class SaleAdapter(
    private val listener: SaleItemClickListener,
    private val viewModel: MainViewModel
) : RecyclerViewDragAdapter<SaleItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    override val isSwappable: Boolean = true
    val currentItems = mutableListOf<SaleItem>()
    private val currentCategoryItems = mutableListOf<CategoryItem>()

    private var selectedItemId: String? = null
    private val inUseItemIds = mutableSetOf<String>()

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
                holder.bind(item, isSelected(item), false)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaleItem>() {
            override fun areItemsTheSame(oldItem: SaleItem, newItem: SaleItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: SaleItem, newItem: SaleItem): Boolean {
                if (oldItem::class != newItem::class) return false

                return when {
                    oldItem is CategoryItem && newItem is CategoryItem ->
                        oldItem.categoryName == newItem.categoryName
                    oldItem is ProductItem && newItem is ProductItem ->
                        oldItem.prName == newItem.prName
                    else -> false
                }
            }
        }
    }

    override fun onAdd(item: SaleItem) {
        when (item) {
            is CategoryItem -> viewModel.addCategoryToUsing(item)
            is ProductItem -> viewModel.addProductToSelectedCategory(item)
        }
    }

    override fun onRemove(item: SaleItem) {
        // 구현 필요 없음
    }

    override fun onSwap(from: Int, to: Int) {
        // 구현 필요 없음
    }

    // 선택 관리
    fun selectItem(item: SaleItem) {
        selectedItemId = item.id
        notifyDataSetChanged()
    }

    fun updateInUseItems(ids: Set<String>) {
        inUseItemIds.clear()
        inUseItemIds.addAll(ids)
        notifyDataSetChanged()
    }

    private fun isItemInUse(id: String): Boolean = inUseItemIds.contains(id)
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

    fun submitCategories(categoryItemList: List<CategoryItem>) {
        currentCategoryItems.clear()
        currentCategoryItems.addAll(categoryItemList)
        updateItems(categoryItemList)
    }

    fun submitProducts(products: List<ProductItem>) {
        updateItems(products)
    }

    private fun updateItems(newItems: List<SaleItem>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = currentItems.size
            override fun getNewListSize(): Int = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return currentItems[oldItemPosition].id == newItems[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return currentItems[oldItemPosition] == newItems[newItemPosition]
            }
        })

        currentItems.clear()
        currentItems.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class CategoryViewHolder(
        private val binding: ItemCategoryBinding,
        private val listener: SaleItemClickListener,
        private val adapter: SaleAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryItem, isSelected: Boolean, isInUse: Boolean) {
            binding.tvCategoryName.text = category.categoryName

            val textColor = if (isInUse) android.R.color.holo_red_dark else android.R.color.black
            val backgroundColor = if (isSelected) R.color.selected_color else android.R.color.transparent

            binding.tvCategoryName.setTextColor(ContextCompat.getColor(binding.root.context, textColor))
            binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, backgroundColor))

            binding.category = category
            binding.listener = listener
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnLongClickListener { view ->
                adapter.startDragCompatible(view)
                true
            }
        }

        companion object {
            fun from(parent: ViewGroup, listener: SaleItemClickListener, adapter: SaleAdapter): CategoryViewHolder {
                val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return CategoryViewHolder(binding, listener, adapter)
            }
        }
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val listener: SaleItemClickListener,
        private val adapter: SaleAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductItem, isSelected: Boolean, isInUse: Boolean) {
            binding.tvProduct.text = product.prName

            val backgroundColor = if (isSelected) R.color.selected_color else android.R.color.transparent
            binding.root.setBackgroundColor(ContextCompat.getColor(binding.root.context, backgroundColor))

            binding.product = product
            binding.listener = listener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup, listener: SaleItemClickListener, adapter: SaleAdapter): ProductViewHolder {
                val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return ProductViewHolder(binding, listener, adapter)
            }
        }
    }
}

interface SaleItemClickListener {
    fun onCategoryClick(category: CategoryItem)
    fun onProductClick(product: ProductItem)
}