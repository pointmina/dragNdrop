package com.hanto.dragndrop.ui.adapter

import android.content.ClipData
import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R

abstract class RecyclerViewDragAdapter<T, VH : RecyclerView.ViewHolder>(
    diffUtil: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffUtil) {

    abstract val isSwappable: Boolean

    val dragListener = object : View.OnDragListener {
        override fun onDrag(view: View?, event: DragEvent?): Boolean {
            event?.let {
                return when (it.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> true
                    DragEvent.ACTION_DRAG_LOCATION -> true
                    DragEvent.ACTION_DRAG_EXITED -> true

                    DragEvent.ACTION_DROP -> {
                        handleDrop(view, event)
                    }

                    DragEvent.ACTION_DRAG_ENDED -> true
                    else -> true
                }
            }
            return true
        }
    }

    private fun handleDrop(view: View?, event: DragEvent): Boolean {
        val sourceView = event.localState as? View ?: return false
        val sourceRecyclerView = sourceView.parent as? RecyclerView ?: return false
        val sourcePosition = sourceRecyclerView.getChildAdapterPosition(sourceView)

        if (sourcePosition < 0) {
            return false
        }

        view?.let { targetView ->
            // 휴지통으로 드롭
            if (isTrashCan(targetView)) {
                return handleTrashDrop(sourceRecyclerView, sourcePosition)
            }

            val targetRecyclerView = findRecyclerView(targetView) ?: return false

            // 같은 RecyclerView 내에서 순서 변경
            if (sourceRecyclerView.id == targetRecyclerView.id && isSwappable) {
                val targetPosition = getTargetPosition(targetRecyclerView, targetView)
                if (targetPosition >= 0 && targetPosition != sourcePosition) {
                    onSwap(sourcePosition, targetPosition)
                }
                return true
            }

            // 카테고리 → 사용중 카테고리 이동
            if (sourceRecyclerView.id == R.id.rv_category &&
                targetRecyclerView.id == R.id.rv_using_category
            ) {
                return handleCategoryToUsing(sourceRecyclerView, sourcePosition)
            }
        }
        return true
    }

    private fun isTrashCan(view: View): Boolean {
        return view.id == R.id.layout_trashcan ||
                (view.parent as? View)?.id == R.id.layout_trashcan
    }

    private fun findRecyclerView(view: View): RecyclerView? {
        return view as? RecyclerView ?: view.parent as? RecyclerView
    }

    private fun getTargetPosition(recyclerView: RecyclerView, targetView: View): Int {
        return if (targetView is RecyclerView) {
            maxOf(0, (recyclerView.adapter?.itemCount ?: 1) - 1)
        } else {
            recyclerView.getChildAdapterPosition(targetView)
        }
    }

    private fun handleTrashDrop(sourceRecyclerView: RecyclerView, sourcePosition: Int): Boolean {
        return try {
            val sourceAdapter = sourceRecyclerView.adapter as? RecyclerViewDragAdapter<*, *>
            if (sourceAdapter != null && sourcePosition < sourceAdapter.currentList.size) {
                @Suppress("UNCHECKED_CAST")
                val adapter = sourceAdapter as RecyclerViewDragAdapter<T, VH>
                adapter.onRemove(adapter.currentList[sourcePosition])
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DragDrop", "휴지통 드롭 실패", e)
            false
        }
    }

    private fun handleCategoryToUsing(
        sourceRecyclerView: RecyclerView,
        sourcePosition: Int
    ): Boolean {
        return try {
            val sourceAdapter = sourceRecyclerView.adapter as? SaleAdapter
            if (sourceAdapter != null && sourcePosition < sourceAdapter.currentItems.size) {
                val draggedItem = sourceAdapter.currentItems[sourcePosition]
                val categoryItem = sourceAdapter.findCategoryById(draggedItem.id)

                categoryItem?.let {
                    val usingCategory = UsingCategoryAdapter.createFromCategoryItem(it)
                    (this as? UsingCategoryAdapter)?.onAdd(usingCategory)
                    true
                } ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DragDrop", "카테고리 이동 실패", e)
            false
        }
    }

    fun startDragCompatible(view: View) {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = View.DragShadowBuilder(view)
        view.startDragAndDrop(data, shadowBuilder, view, 0)
    }

    abstract fun onAdd(item: T)
    abstract fun onRemove(item: T)
    abstract fun onSwap(from: Int, to: Int)
}