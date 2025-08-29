package com.hanto.dragndrop.ui.drag

import android.util.Log
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.R
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.UsingCategory

/**
 * 드래그 규칙 정의
 */
data class DragRule(
    val sourceId: Int,
    val targetId: Int,
    val isAllowed: Boolean,
    val needsConversion: Boolean = false
)

/**
 * 드래그되는 데이터 래퍼
 */
data class DragData(
    val item: Any,
    val sourceRecyclerViewId: Int,
    var sourcePosition: Int
)

/**
 * 드래그 액션 결과
 */
sealed class DragResult {
    data object Success : DragResult()
    data object Ignored : DragResult()
    data class Error(val message: String) : DragResult()
}

/**
 * 중앙 집중식 드래그 앤 드롭 관리자
 */
class DragManager {

    companion object {
        private const val TAG = "DragManager"

        // 드래그 규칙 테이블
        private val DRAG_RULES = listOf(
            // 카테고리 -> 사용 중인 카테고리 (허용)
            DragRule(
                sourceId = R.id.rv_category,
                targetId = R.id.rv_using_category,
                isAllowed = true,
                needsConversion = true
            ),

            // 제품 -> 사용 중인 제품 (허용)
            DragRule(
                sourceId = R.id.rv_product,
                targetId = R.id.rv_using_product,
                isAllowed = true
            )
        )
    }

    // RecyclerView 등록소
    private val recyclerViewRegistry = mutableMapOf<Int, RecyclerView>()

    /**
     * 드래그 이벤트 처리 메인 엔트리 포인트
     */
    fun handleDragEvent(view: View, event: DragEvent): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> true
            DragEvent.ACTION_DRAG_ENTERED -> handleDragEntered(view)
            DragEvent.ACTION_DRAG_EXITED -> handleDragExited(view)
            DragEvent.ACTION_DROP -> {
                val result = handleDrop(view, event)
                when (result) {
                    is DragResult.Success -> true
                    is DragResult.Ignored -> true
                    is DragResult.Error -> {
                        Log.e(TAG, "드롭 처리 오류: ${result.message}")
                        false
                    }
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> true
            else -> false
        }
    }

    private fun handleDragEntered(view: View): Boolean = true
    private fun handleDragExited(view: View): Boolean = true


    /**
     * 드롭 이벤트 처리
     */
    private fun handleDrop(targetView: View, event: DragEvent): DragResult {
        val dragData = extractDragData(event)
        if (dragData == null) {
            Log.e(TAG, "유효하지 않은 드래그 데이터")
            return DragResult.Error("유효하지 않은 드래그 데이터")
        }

        Log.d(
            TAG,
            "드롭 처리 시작 - 원래 아이템 정보: ${dragData.sourceRecyclerViewId}, sourcePosition: ${dragData.sourcePosition}"
        )

        return when {
            // 휴지통으로 드롭
            isTrashcanTarget(targetView) -> {
                Log.d(TAG, "휴지통 드롭 처리")
                handleTrashcanDrop(dragData)
            }

            // 같은 RecyclerView 내 순서 변경
            isSameRecyclerView(targetView, dragData.sourceRecyclerViewId) -> {
                Log.d(TAG, "같은 RecyclerView 내 순서 변경 처리")
                handleInternalSwap(targetView, dragData, event)
            }


            // 다른 RecyclerView로 이동
            else -> {
                Log.d(TAG, "다른 RecyclerView로 이동 처리")
                handleCrossRecyclerViewDrop(targetView, dragData)
            }
        }
    }

    /**
     * 드래그 데이터 추출
     */
    private fun extractDragData(event: DragEvent): DragData? {
        val sourceView = event.localState as? View
        if (sourceView == null) {
            Log.e(TAG, "sourceView가 null입니다")
            return null
        }

        val sourceRecyclerView = sourceView.parent as? RecyclerView
        if (sourceRecyclerView == null) {
            Log.e(TAG, "sourceRecyclerView가 null입니다")
            return null
        }

        val sourcePosition = sourceRecyclerView.getChildAdapterPosition(sourceView)
        if (sourcePosition < 0) {
            Log.e(TAG, "유효하지 않은 sourcePosition: $sourcePosition")
            return null
        }

        val sourceAdapter = sourceRecyclerView.adapter as? DragCapable
        if (sourceAdapter == null) {
            Log.e(TAG, "sourceAdapter가 DragCapable이 아닙니다")
            return null
        }

        val item = sourceAdapter.getItemForDrag(sourcePosition)
        if (item == null) {
            Log.e(TAG, "드래그할 아이템이 null입니다")
            return null
        }

        Log.d(TAG, "드래그 데이터 추출 성공 - RecyclerViewId: ${sourceRecyclerView.id}, Position: $sourcePosition")
        return DragData(item, sourceRecyclerView.id, sourcePosition)
    }

    /**
     * 휴지통 타겟 확인
     */
    private fun isTrashcanTarget(view: View): Boolean {
        return view.id == R.id.layout_trashcan || findParentWithId(view, R.id.layout_trashcan) != null
    }

    /**
     * 특정 ID를 가진 부모 찾기
     */
    private fun findParentWithId(view: View, targetId: Int): View? {
        var parent = view.parent as? View
        while (parent != null) {
            if (parent.id == targetId) return parent
            parent = parent.parent as? View
        }
        return null
    }

    /**
     * 같은 RecyclerView 확인
     */
    private fun isSameRecyclerView(targetView: View, sourceId: Int): Boolean {
        var targetRecyclerView: RecyclerView? = null

        if (targetView is RecyclerView) {
            targetRecyclerView = targetView
        } else {
            var parent = targetView.parent
            while (parent != null) {
                if (parent is RecyclerView) {
                    targetRecyclerView = parent
                    break
                }
                parent = parent.parent
            }
        }

        val result = targetRecyclerView?.id == sourceId
        Log.d(TAG, "isSameRecyclerView: targetId=${targetRecyclerView?.id}, sourceId=$sourceId, result=$result")
        return result
    }

    /**
     * 휴지통 드롭 처리
     */
    private fun handleTrashcanDrop(dragData: DragData): DragResult {
        return try {
            val sourceRecyclerView = recyclerViewRegistry[dragData.sourceRecyclerViewId]
            val sourceAdapter = sourceRecyclerView?.adapter as? DragCapable

            sourceAdapter?.onDragRemove(dragData.item)
            Log.d(TAG, "휴지통 드롭 완료")
            DragResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "휴지통 드롭 실패", e)
            DragResult.Error("휴지통 드롭 실패: ${e.message}")
        }
    }

    /**
     * 같은 RecyclerView 내 순서 변경 처리
     */
    private fun handleInternalSwap(
        targetView: View,
        dragData: DragData,
        event: DragEvent
    ): DragResult {
        return try {
            Log.d(
                TAG,
                "handleInternalSwap 시작 - sourcePosition=${dragData.sourcePosition}, targetView=$targetView"
            )

            val targetRecyclerView =
                targetView as? RecyclerView ?: targetView.parent as? RecyclerView
            val targetAdapter = targetRecyclerView?.adapter as? DragCapable

            if (!targetAdapter?.isSwappable()!!) {
                return DragResult.Ignored
            }

            val targetPosition = when (targetView) {
                is RecyclerView -> targetRecyclerView.findTargetPosition(event)
                else -> targetRecyclerView.getChildAdapterPosition(targetView)
            }
            Log.d(TAG, "계산된 targetPosition=$targetPosition")

            if (targetPosition >= 0 && targetPosition != dragData.sourcePosition) {
                Log.d(TAG, "내부 순서 변경 실행: ${dragData.sourcePosition} -> $targetPosition")
                targetAdapter.onDragSwap(dragData.sourcePosition, targetPosition)

                dragData.sourcePosition = targetPosition
            } else {
                Log.d(
                    TAG,
                    "순서 변경 조건 불충족 (targetPosition=$targetPosition, source=${dragData.sourcePosition})"
                )
            }

            Log.d(TAG, "handleInternalSwap 완료 - DragResult.Success")
            DragResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "내부 순서 변경 실패", e)
            DragResult.Error("순서 변경 실패: ${e.message}")
        }
    }



    /**
     * 다른 RecyclerView로 이동 처리
     */
    private fun handleCrossRecyclerViewDrop(targetView: View, dragData: DragData): DragResult {
        return try {
            val targetRecyclerView = targetView as? RecyclerView ?: targetView.parent as? RecyclerView ?: return DragResult.Ignored
            val targetAdapter = targetRecyclerView.adapter as? DragCapable ?: return DragResult.Ignored

            val rule = findDragRule(dragData.sourceRecyclerViewId, targetRecyclerView.id)
            if (rule?.isAllowed != true) {
                Log.d(TAG, "드래그 규칙에 의해 차단됨")
                return DragResult.Ignored
            }

            val targetItem = if (rule.needsConversion) {
                convertItemForTarget(dragData.item, targetRecyclerView.id)
            } else {
                dragData.item
            }

            if (targetItem != null) {
                targetAdapter.onDragAdd(targetItem)
                Log.d(TAG, "RecyclerView 간 이동 완료")
            }

            DragResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "RecyclerView 간 이동 실패", e)
            DragResult.Error("이동 실패: ${e.message}")
        }
    }

    /**
     * 드래그 규칙 찾기
     */
    private fun findDragRule(sourceId: Int, targetId: Int): DragRule? {
        return DRAG_RULES.find { it.sourceId == sourceId && it.targetId == targetId }
    }

    /**
     * 타입 변환 처리
     */
    private fun convertItemForTarget(item: Any, targetRecyclerViewId: Int): Any? {
        return when (targetRecyclerViewId) {
            R.id.rv_using_category -> {
                if (item is CategoryItem) {
                    UsingCategory(item, mutableListOf())
                } else item
            }
            else -> item
        }
    }

    /**
     * Fragment에서 RecyclerView 등록
     */
    fun registerRecyclerView(id: Int, recyclerView: RecyclerView) {
        recyclerViewRegistry[id] = recyclerView
        Log.d(TAG, "RecyclerView 등록: $id")
    }

    /**
     * 메모리 정리
     */
    fun clear() {
        recyclerViewRegistry.clear()
        Log.d(TAG, "DragManager 정리 완료")
    }
}