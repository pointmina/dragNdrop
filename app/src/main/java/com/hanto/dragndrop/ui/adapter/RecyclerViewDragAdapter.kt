package com.hanto.dragndrop.ui.adapter

import android.content.ClipData
import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.util.PerformanceOptimizer

abstract class RecyclerViewDragAdapter<T, VH : RecyclerView.ViewHolder>(
    diffUtil: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffUtil) {

    abstract val isSwappable: Boolean

    // 드래그 이벤트 디바운서 추가
    private val dragDebouncer = PerformanceOptimizer.DragEventDebouncer()

    val dragListener = object : View.OnDragListener {
        override fun onDrag(view: View?, event: DragEvent?): Boolean {
            event?.let {
                when (it.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        return true
                    }

                    DragEvent.ACTION_DRAG_ENTERED -> {
                        return true
                    }

                    DragEvent.ACTION_DRAG_LOCATION -> {
                        // 위치 변경 이벤트는 디바운싱 적용
                        dragDebouncer.debounce {
                            handleDragLocation(view, event)
                        }
                        return true
                    }

                    DragEvent.ACTION_DRAG_EXITED -> {
                        return true
                    }

                    DragEvent.ACTION_DROP -> {
                        // 드롭 이벤트는 즉시 처리
                        dragDebouncer.cancel()
                        return handleDropOptimized(view, event)
                    }

                    DragEvent.ACTION_DRAG_ENDED -> {
                        dragDebouncer.cancel()
                        return true
                    }

                    else -> {}
                }
            }
            return true
        }
    }

    // 드래그 위치 처리 (디바운싱됨)
    private fun handleDragLocation(view: View?, event: DragEvent) {
        // 필요한 경우 시각적 피드백 처리
        // 성능을 위해 최소화
    }

    private fun handleDropOptimized(view: View?, event: DragEvent): Boolean {
        val sourceView = event.localState as? View ?: return false
        val sourceRecyclerView = sourceView.parent as? RecyclerView ?: return false
        val sourcePosition = sourceRecyclerView.getChildAdapterPosition(sourceView)

        // 안전 검사 수정 - SaleAdapter의 currentItems 기준으로 확인
        if (sourcePosition < 0) {
            Log.e("DragDrop", "유효하지 않은 소스 위치: $sourcePosition")
            return false
        }

        // SaleAdapter인 경우 currentItems 크기로 확인
        val sourceAdapter = sourceRecyclerView.adapter
        val maxSize = when (sourceAdapter) {
            is SaleAdapter -> sourceAdapter.currentItems.size
            else -> currentList.size
        }

        if (sourcePosition >= maxSize) {
            Log.e("DragDrop", "소스 위치가 범위 초과: $sourcePosition >= $maxSize")
            return false
        }

        view?.let { targetView ->
            if (isTrashCanTarget(targetView)) {
                return handleTrashCanDrop(sourceRecyclerView, sourcePosition)
            }

            // RecyclerView 드롭 처리
            val targetRecyclerView = findTargetRecyclerView(targetView) ?: return false
            return processRecyclerViewDropOptimized(
                sourceRecyclerView, targetRecyclerView,
                sourcePosition, targetView
            )
        }
        return false
    }

    // 휴지통 타겟 확인
    private fun isTrashCanTarget(view: View): Boolean {
        return view.id == R.id.layout_trashcan ||
                (view.parent as? View)?.id == R.id.layout_trashcan
    }

    // 휴지통 드롭 처리
    private fun handleTrashCanDrop(sourceRecyclerView: RecyclerView, sourcePosition: Int): Boolean {
        val sourceAdapter = sourceRecyclerView.adapter as? RecyclerViewDragAdapter<*, *>
        return try {
            if (sourceAdapter != null) {
                // SaleAdapter인 경우 currentItems 사용
                val itemToRemove = when (sourceAdapter) {
                    is SaleAdapter -> {
                        if (sourcePosition < sourceAdapter.currentItems.size) {
                            sourceAdapter.currentItems[sourcePosition]
                        } else null
                    }

                    else -> {
                        if (sourcePosition < sourceAdapter.currentList.size) {
                            sourceAdapter.currentList[sourcePosition]
                        } else null
                    }
                }

                if (itemToRemove != null) {
                    @Suppress("UNCHECKED_CAST")
                    val adapter = sourceAdapter as RecyclerViewDragAdapter<T, VH>
                    adapter.onRemove(itemToRemove as T)
                    true
                } else {
                    Log.e("DragDrop", "제거할 아이템을 찾을 수 없음: position=$sourcePosition")
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DragDrop", "휴지통 드롭 실패", e)
            false
        }
    }

    // 타겟 RecyclerView 찾기
    private fun findTargetRecyclerView(view: View): RecyclerView? {
        return view as? RecyclerView ?: view.parent as? RecyclerView
    }

    private fun processRecyclerViewDropOptimized(
        sourceRv: RecyclerView,
        targetRv: RecyclerView,
        sourcePosition: Int,
        targetView: View
    ): Boolean {
        if (!isValidDrop(sourceRv.id, targetRv.id)) {
            return true
        }

        // 같은 RecyclerView 내에서 순서 변경
        if (sourceRv.id == targetRv.id && isSwappable) {
            val targetPosition = calculateTargetPosition(targetRv, targetView)
            if (targetPosition >= 0 && targetPosition != sourcePosition) {
                val sourceAdapter = sourceRv.adapter as? RecyclerViewDragAdapter<T, VH>
                sourceAdapter?.onSwap(sourcePosition, targetPosition)
            }
            return true
        }

        // 다른 RecyclerView 간 이동 처리
        return handleCrossRecyclerViewDrop(sourceRv, targetRv, sourcePosition)
    }

    // 드롭 유효성 검사
    private fun isValidDrop(sourceId: Int, targetId: Int): Boolean {
        return when {
            sourceId == R.id.rv_using_category && targetId == R.id.rv_using_product -> false
            sourceId == R.id.rv_using_product && targetId == R.id.rv_using_category -> false
            else -> true
        }
    }

    private fun calculateTargetPosition(targetRv: RecyclerView, targetView: View): Int {
        return if (targetView is RecyclerView) {
            maxOf(0, (targetRv.adapter?.itemCount ?: 1) - 1)
        } else {
            targetRv.getChildAdapterPosition(targetView)
        }
    }

    // 다른 RecyclerView 간 드롭 처리
    private fun handleCrossRecyclerViewDrop(
        sourceRv: RecyclerView,
        targetRv: RecyclerView,
        sourcePosition: Int
    ): Boolean {
        // rv_category에서 rv_using_category로의 드래그만 처리
        if (sourceRv.id == R.id.rv_category && targetRv.id == R.id.rv_using_category) {
            return handleCategoryToUsingCategoryDrop(sourceRv, targetRv, sourcePosition)
        }
        return true
    }

    // 카테고리 → 사용중 카테고리 드롭 처리
    private fun handleCategoryToUsingCategoryDrop(
        sourceRv: RecyclerView,
        targetRv: RecyclerView,
        sourcePosition: Int
    ): Boolean {
        return try {
            val sourceAdapter = sourceRv.adapter as? SaleAdapter
            val targetAdapter = targetRv.adapter as? UsingCategoryAdapter

            if (sourceAdapter != null && targetAdapter != null &&
                sourcePosition < sourceAdapter.currentItems.size
            ) {

                val draggedItem = sourceAdapter.currentItems[sourcePosition]
                val categoryItem = sourceAdapter.findCategoryById(draggedItem.id)

                categoryItem?.let {
                    val usingCategory = UsingCategoryAdapter.createFromCategoryItem(it)
                    targetAdapter.onAdd(usingCategory)
                    Log.d("DragDrop", "카테고리를 UsingCategory로 추가: ${it.categoryName}")
                    true
                } ?: false
            } else {
                Log.e(
                    "DragDrop",
                    "어댑터 null 또는 위치 오류: sourceAdapter=$sourceAdapter, targetAdapter=$targetAdapter, position=$sourcePosition"
                )
                false
            }
        } catch (e: Exception) {
            Log.e("DragDrop", "크로스 RecyclerView 드롭 실패", e)
            false
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