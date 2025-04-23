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

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SaleItem>() {
            override fun areItemsTheSame(oldItem: SaleItem, newItem: SaleItem): Boolean {
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: SaleItem, newItem: SaleItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onAdd(item: SaleItem) {
        when (item) {
            is CategoryItem -> {
                // ViewModel을 통해 UsingCategory 추가
                viewModel.addCategoryToUsing(item)
                Log.d("RetailSettingAdapter", "CategoryItem ${item.categoryName} 추가됨")
            }

            is ProductItem -> {
                // 필요한 경우 제품 추가 로직 구현
                viewModel.addProductToSelectedCategory(item)
                Log.d("RetailSettingAdapter", "PrItem ${item.prName} 추가됨")
            }

            else -> {
                Log.e("RetailSettingAdapter", "알 수 없는 아이템 타입")
            }
        }
    }

    override fun onRemove(item: SaleItem) {
        // 구현 필요 없음 - 이 어댑터에서는 제거 동작 없음
    }

    override fun onSwap(from: Int, to: Int) {
        // 구현 필요 없음 - 이 어댑터에서는 스와핑 동작 없음
    }

    // 아이템 선택 메소드
    fun selectItem(item: SaleItem) {
        val wasSelected = item.id == selectedItemId
        val previousSelectedId = selectedItemId

        selectedItemId = if (wasSelected) null else item.id

        if (previousSelectedId != null) {
            val previousPosition = currentItems.indexOfFirst { it.id == previousSelectedId }
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
        }

        val newPosition = currentItems.indexOfFirst { it.id == item.id }
        if (newPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(newPosition)
        }
    }

    // 아이템이 사용 중인지 확인하는 메소드
    private fun isItemInUse(id: String): Boolean {
        return inUseItemIds.contains(id)
    }

    // 사용 중인 아이템 ID 목록 업데이트
    fun updateInUseItems(ids: Set<String>) {
        inUseItemIds.clear()
        inUseItemIds.addAll(ids)
        notifyDataSetChanged()
    }

    // 제품이 현재 선택된 카테고리에 포함되어 있는지 확인하는 메소드 (추가)
    private fun isProductInSelectedCategory(prCode: String): Boolean {
        return selectedCategoryProductIds.contains(prCode)
    }

    // 선택된 카테고리 내 제품 ID 목록 업데이트 (추가)
    fun updateSelectedCategoryProducts(prCode: Set<String>) {
        selectedCategoryProductIds.clear()
        selectedCategoryProductIds.addAll(prCode)
        notifyDataSetChanged()
    }

    // 현재 아이템이 선택되었는지 확인하는 메소드
    private fun isSelected(item: SaleItem): Boolean {
        return item.id == selectedItemId
    }

    override fun getItemCount(): Int = currentItems.size

    override fun getItemViewType(position: Int): Int {
        return when (currentItems[position]) {
            is CategoryItem -> VIEW_TYPE_CATEGORY
            is ProductItem -> VIEW_TYPE_PRODUCT
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    // ID로 아이템 위치 찾기
    fun findPositionById(id: String): Int {
        return currentItems.indexOfFirst { it.id == id }
    }

    fun findCategoryById(id: String): CategoryItem? {
        return currentCategoryItems.find { it.id == id }
    }

    fun findProductById(id: String): ProductItem? {
        return currentItems.find { it is ProductItem && it.id == id } as? ProductItem
    }

    fun submitCategories(categoryItemList: List<CategoryItem>) {
        currentCategoryItems.clear()
        currentCategoryItems.addAll(categoryItemList)
        updateItems(categoryItemList)
    }

    fun submitProducts(products: List<ProductItem>) {
        updateItems(products)
    }


    private fun updateItems(newItems: List<SaleItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = currentItems.size
            override fun getNewListSize(): Int = newItems.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return currentItems[oldItemPosition].id == newItems[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return currentItems[oldItemPosition] == newItems[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
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
            binding.category = category
            binding.listener = listener
            binding.isSelected = isSelected
            binding.tvCategoryName.text = category.categoryName

            if (isInUse) {
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        android.R.color.holo_red_dark
                    )
                )
            } else {
                binding.tvCategoryName.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        android.R.color.black
                    )
                )
            }

            binding.root.setOnLongClickListener { view ->
                adapter.startDragCompatible(view)
                true
            }

            binding.executePendingBindings()
        }

        companion object {
            fun from(
                parent: ViewGroup,
                listener: SaleItemClickListener,
                adapter: SaleAdapter
            ): CategoryViewHolder {
                val binding = ItemCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
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

        fun bind(product: ProductItem, isSelected: Boolean, isInUse: Boolean) {
            binding.product = product
            binding.listener = listener
            binding.isSelected = isSelected

            // 로그 추가로 디버깅
            Log.d("ProductViewHolder", "Product: ${product.prName}, isSelected: $isSelected")

            if (isInUse) {
                binding.tvProduct.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        android.R.color.holo_red_dark
                    )
                )
            } else {
                binding.tvProduct.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        android.R.color.black
                    )
                )
            }

            // 배경색 직접 변경 (binding이 작동하지 않을 경우)
            if (isSelected) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.selected_color
                    )
                )
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        android.R.color.transparent
                    )
                )
            }

            binding.executePendingBindings()
        }


        companion object {
            fun from(
                parent: ViewGroup,
                listener: SaleItemClickListener,
                adapter: SaleAdapter
            ): ProductViewHolder {
                val binding = ItemProductBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
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
