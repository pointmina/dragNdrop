package com.hanto.dragndrop.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.databinding.ItemUsingProductBinding
import com.hanto.dragndrop.ui.MainViewModel

class UsingProductAdapter(
    private val viewModel: MainViewModel
) : RecyclerViewDragAdapter<ProductItem, UsingProductAdapter.UsingProductViewHolder>(DIFF_CALLBACK) {

    override val isSwappable: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsingProductViewHolder {
        val binding = ItemUsingProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UsingProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsingProductViewHolder, position: Int) {
        if (position >= 0 && position < currentList.size) {
            holder.bind(getItem(position))
        }
    }

    override fun onAdd(item: ProductItem) {
        // 필요시 구현
    }

    override fun onRemove(item: ProductItem) {
        viewModel.removeProduct(item.id)
    }

    override fun onSwap(from: Int, to: Int) {
        viewModel.swapUsingProducts(from, to)
    }

    inner class UsingProductViewHolder(private val binding: ItemUsingProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

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
            binding.tvProductName.text = product.prName
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProductItem>() {
            override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
                return oldItem.prName == newItem.prName
            }
        }
    }
}