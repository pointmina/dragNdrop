package com.hanto.dragndrop.ui.drag

import android.content.ClipData
import android.graphics.Rect
import android.view.DragEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

/**
 * 드래그 앤 드롭 기능을 제공하는 어댑터가 구현해야 하는 인터페이스
 */
interface DragCapable {

    /**
     * 드래그 가능 여부 (순서 변경 지원 여부)
     */
    fun isSwappable(): Boolean

    /**
     * 특정 위치의 아이템을 드래그용 데이터로 반환
     * @param position 드래그할 아이템의 위치
     * @return 드래그할 아이템 객체 (null이면 드래그 불가)
     */
    fun getItemForDrag(position: Int): Any?

    /**
     * 외부에서 아이템이 드롭되었을 때 처리
     * @param item 추가할 아이템
     */
    fun onDragAdd(item: Any)

    /**
     * 아이템을 제거 요청받았을 때 처리 (휴지통 드롭 등)
     * @param item 제거할 아이템
     */
    fun onDragRemove(item: Any)

    /**
     * 같은 어댑터 내에서 아이템 순서 변경
     * @param fromPosition 원래 위치
     * @param toPosition 이동할 위치
     */
    fun onDragSwap(fromPosition: Int, toPosition: Int)

    /**
     * 어댑터의 총 아이템 수 반환
     */
    fun getItemCount(): Int
}

/**
 * 드래그 시작을 위한 유틸리티 클래스
 */
object DragHelper {

    /**
     * API 레벨에 관계없이 드래그를 시작
     * @param view 드래그를 시작할 뷰
     * @param dragData 드래그할 데이터 (선택사항)
     * @return 드래그 시작 성공 여부
     */
    fun startDrag(view: View, dragData: String = ""): Boolean {
        return try {
            val data = ClipData.newPlainText("drag_data", dragData)
            val shadowBuilder = View.DragShadowBuilder(view)

            view.startDragAndDrop(data, shadowBuilder, view, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 뷰가 드래그 가능한지 확인
     * @param view 확인할 뷰
     * @return 드래그 가능 여부
     */
    fun isDragCapable(view: View): Boolean {
        return view.isEnabled && view.isVisible
    }
}


/**
 * RecyclerView 안에서 드롭 좌표에 해당하는 adapter position을 찾는 확장 함수
 */
fun RecyclerView.findTargetPosition(event: DragEvent): Int {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        val rect = Rect()
        child.getHitRect(rect)
        if (rect.contains(event.x.toInt(), event.y.toInt())) {
            return getChildAdapterPosition(child)
        }
    }

    return (adapter?.itemCount ?: 1) - 1
}
