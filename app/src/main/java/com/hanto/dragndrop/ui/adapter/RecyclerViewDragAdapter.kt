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
                when (it.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        return true
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        // 드래그가 뷰에 들어왔을 때의 시각적 피드백 (옵션)
                        return true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        return true
                    }

                    DragEvent.ACTION_DRAG_EXITED -> {
                        // 드래그가 뷰에서 나갔을 때의 시각적 피드백 (옵션)
                        return true
                    }

                    DragEvent.ACTION_DROP -> {
                        val sourceView = it.localState as View
                        val sourceRecyclerView = sourceView.parent as RecyclerView
                        val sourcePosition = sourceRecyclerView.getChildAdapterPosition(sourceView)

                        // 안전 검사
                        if (sourcePosition < 0) {
                            Log.e("DragDrop", "유효하지 않은 소스 위치: $sourcePosition")
                            return false
                        }

                        view?.let { targetView ->
                            var targetRecyclerView: RecyclerView? = targetView as? RecyclerView

                            if (targetRecyclerView == null) {
                                targetRecyclerView = targetView.parent as? RecyclerView
                            }

                            // 휴지통으로 드롭하는 경우 처리
                            if (targetView.id == R.id.layout_trashcan ||
                                (targetView.parent as? View)?.id == R.id.layout_trashcan
                            ) {
                                if (sourceRecyclerView.adapter is RecyclerViewDragAdapter<*, *>) {
                                    val sourceAdapter =
                                        sourceRecyclerView.adapter as RecyclerViewDragAdapter<T, VH>
                                    try {
                                        sourceAdapter.onRemove(sourceAdapter.currentList[sourcePosition])
                                    } catch (e: Exception) {
                                        Log.e("DragDrop", "제거 실패", e)
                                    }
                                }
                                return true
                            }

                            if (targetRecyclerView !is RecyclerView) {
                                return false
                            }

                            // 특정 리사이클러뷰 간 드래그 앤 드롭 제한
                            if (sourceRecyclerView.id == R.id.rv_using_category &&
                                targetRecyclerView.id == R.id.rv_using_product
                            ) {
                                return true
                            }

                            if (sourceRecyclerView.id == R.id.rv_using_product &&
                                targetRecyclerView.id == R.id.rv_using_category
                            ) {
                                return true
                            }

                            // 같은 RecyclerView 내에서 아이템 순서 변경
                            if (sourceRecyclerView.id == targetRecyclerView.id) {
                                if (isSwappable) {
                                    val targetPosition = if (targetView is RecyclerView) {
                                        (targetRecyclerView.adapter as RecyclerViewDragAdapter<*, *>).itemCount - 1
                                    } else {
                                        targetRecyclerView.getChildAdapterPosition(targetView)
                                    }

                                    if (targetPosition >= 0) {
                                        val sourceAdapter =
                                            sourceRecyclerView.adapter as RecyclerViewDragAdapter<T, VH>
                                        sourceAdapter.onSwap(sourcePosition, targetPosition)
                                    }
                                }
                            } else {
                                // 서로 다른 RecyclerView 간의 드래그 앤 드롭 처리

                                // rv_category에서 rv_using_category로의 드래그 처리
                                if (sourceRecyclerView.id == R.id.rv_category &&
                                    targetRecyclerView.id == R.id.rv_using_category
                                ) {

                                    try {
                                        // 소스 어댑터에서 아이템 가져오기
                                        val sourceAdapter = sourceRecyclerView.adapter
                                        val targetAdapter = targetRecyclerView.adapter

                                        if (sourceAdapter is SaleAdapter &&
                                            targetAdapter is UsingCategoryAdapter
                                        ) {

                                            // CategoryItem 가져오기
                                            val categoryItem =
                                                sourceAdapter.findCategoryById(sourceAdapter.currentItems[sourcePosition].id)

                                            if (categoryItem != null) {
                                                // CategoryItem을 UsingCategory로 변환
                                                val usingCategory =
                                                    UsingCategoryAdapter.createFromCategoryItem(
                                                        categoryItem
                                                    )

                                                // ViewModel을 통해 처리
                                                targetAdapter.onAdd(usingCategory)

                                                Log.d(
                                                    "DragDrop",
                                                    "카테고리를 UsingCategory로 추가: ${categoryItem.categoryName}"
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("DragDrop", "다른 리사이클러뷰로 이동 중 오류", e)

                                    }
                                }
                            }
                        }
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        // 드래그 종료 시 추가 로직 (옵션)
                        return true
                    }

                    else -> {}
                }
            }
            return true
        }
    }

    // API 레벨 호환성을 위한 드래그 시작 도우미 메서드
    fun startDragCompatible(view: View) {
        val data = ClipData.newPlainText("", "")
        val shadowBuilder = View.DragShadowBuilder(view)

        view.startDragAndDrop(data, shadowBuilder, view, 0)
    }

    abstract fun onAdd(item: T)
    abstract fun onRemove(item: T)
    abstract fun onSwap(from: Int, to: Int)
}