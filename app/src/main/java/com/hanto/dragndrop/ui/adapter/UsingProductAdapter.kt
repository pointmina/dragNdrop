package com.hanto.dragndrop.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.databinding.ItemUsingProductBinding
import com.hanto.dragndrop.ui.drag.DragCapable
import com.hanto.dragndrop.ui.drag.DragHelper
import com.hanto.dragndrop.ui.MainViewModel
import java.util.Collections

class UsingProductAdapter(
    private val viewModel: MainViewModel
) : RecyclerView.Adapter<UsingProductAdapter.UsingProductViewHolder>(), DragCapable {

    private val TAG = "UsingProductAdapter"

    // 데이터 관리
    private val items = mutableListOf<ProductItem>()

    override fun isSwappable(): Boolean = true

    override fun getItemForDrag(position: Int): Any? {
        return if (isValidPosition(position)) {
            items[position]
        } else null
    }

    override fun onDragAdd(item: Any) {
        if (item is ProductItem) {
            val existingIndex = items.indexOfFirst { it.id == item.id }
            if (existingIndex == -1) {
                items.add(item)
                notifyItemInserted(items.size - 1)
                viewModel.addProductToSelectedCategory(item)
                Log.d(TAG, "ProductItem 추가: ${item.prName}")
            } else {
                Log.d(TAG, "이미 존재하는 ProductItem: ${item.prName}")
            }
        }
    }

    override fun onDragRemove(item: Any) {
        if (item is ProductItem) {
            val position = items.indexOfFirst { it.id == item.id }
            if (position >= 0) {
                items.removeAt(position)
                notifyItemRemoved(position)
                viewModel.removeProduct(item.id)
                Log.d(TAG, "ProductItem 제거: ${item.prName}")
            }
        }
    }

    override fun onDragSwap(fromPosition: Int, toPosition: Int) {
        if (isValidPosition(fromPosition) && isValidPosition(toPosition)) {
            Collections.swap(items, fromPosition, toPosition)
            notifyItemMoved(fromPosition, toPosition)
            viewModel.swapUsingProducts(fromPosition, toPosition)
            Log.d(TAG, "ProductItem 순서 변경: $fromPosition -> $toPosition")
        } else {
            Log.e(TAG, "유효하지 않은 위치로 스와프 시도: $fromPosition -> $toPosition (총 ${items.size}개)")
        }
    }

    private fun isValidPosition(position: Int): Boolean {
        return position >= 0 && position < items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsingProductViewHolder {
        val binding = ItemUsingProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UsingProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UsingProductViewHolder, position: Int) {
        if (!isValidPosition(position)) return

        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * 목록 업데이트
     */
    fun submitList(newItems: List<ProductItem>) {
        Log.d(TAG, "submitList 호출 - 아이템 수: ${newItems.size}")

        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()

        Log.d(TAG, "submitList 완료")
    }

    /**
     * ViewHolder 구현
     */
    inner class UsingProductViewHolder(
        private val binding: ItemUsingProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // 드래그 시작 리스너
            binding.root.setOnLongClickListener { view ->
                if (DragHelper.isDragCapable(view)) {
                    val success = DragHelper.startDrag(view)
                    Log.d("UsingProductViewHolder", "드래그 시작: $success")
                    success
                } else {
                    false
                }
            }
        }

        fun bind(product: ProductItem) {
            binding.tvProductName.text = product.prName
            Log.d("UsingProductViewHolder", "제품 바인딩: ${product.prName}")
        }
    }
}