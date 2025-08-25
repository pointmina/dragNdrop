package com.hanto.dragndrop.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.databinding.ItemUsingProductBinding

class UsingProductAdapter(
    private val callback: DragDropCallback
) : RecyclerViewDragAdapter<ProductItem, UsingProductAdapter.UsingProductViewHolder>(DIFF_CALLBACK) {

    override val isSwappable: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsingProductViewHolder {
        val binding = ItemUsingProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UsingProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsingProductViewHolder, position: Int) {
        if (position < 0 || position >= currentList.size) {
            return
        }
        holder.bind(getItem(position))
    }

    // Payload 지원 바인딩
    override fun onBindViewHolder(
        holder: UsingProductViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            // 부분 업데이트가 필요한 경우 처리
            holder.updateState()
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onAdd(item: ProductItem) {
        callback.onProductAdded(item)
    }

    override fun onRemove(item: ProductItem) {
        callback.onProductRemoved(item.id)
        Log.d("UsingProductAdapter", "제품 삭제: ${item.prName}")
    }

    override fun onSwap(from: Int, to: Int) {
        callback.onProductsSwapped(from, to)
    }

    inner class UsingProductViewHolder(private val binding: ItemUsingProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentProduct: ProductItem? = null

        init {
            binding.root.setOnLongClickListener { view ->
                if ((view.parent as? RecyclerView)?.id == R.id.rv_using_product) {
                    startDragCompatible(view)
                }
                true
            }

            binding.root.setOnDragListener(dragListener)
        }

        fun bind(product: ProductItem) {
            currentProduct = product

            // 텍스트는 제품이 다를 때만 변경
            if (binding.tvProductName.text != product.prName) {
                binding.tvProductName.text = product.prName
            }
        }

        fun updateState() {
            // 필요한 경우 상태 업데이트 로직
            // 현재는 제품 어댑터에서 특별한 상태 변화 없음
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProductItem>() {
            override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
                return oldItem.id == newItem.id &&
                        oldItem.prName == newItem.prName &&
                        oldItem.categoryId == newItem.categoryId
            }

            override fun getChangePayload(oldItem: ProductItem, newItem: ProductItem): Any? {
                return "product_changed"
            }
        }
    }
}