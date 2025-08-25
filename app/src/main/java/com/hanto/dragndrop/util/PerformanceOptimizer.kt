package com.hanto.dragndrop.util

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView

class PerformanceOptimizer {

    class DragEventDebouncer(private val delayMs: Long = 16L) {
        private val handler = Handler(Looper.getMainLooper())
        private var pendingRunnable: Runnable? = null

        fun debounce(action: () -> Unit) {
            pendingRunnable?.let { handler.removeCallbacks(it) }

            val runnable = Runnable { action() }
            pendingRunnable = runnable
            handler.postDelayed(runnable, delayMs)
        }

        fun cancel() {
            pendingRunnable?.let {
                handler.removeCallbacks(it)
                pendingRunnable = null
            }
        }
    }

    class BatchUpdateManager {
        private val pendingUpdates = mutableListOf<() -> Unit>()
        private val handler = Handler(Looper.getMainLooper())
        private var isUpdateScheduled = false

        fun addUpdate(update: () -> Unit) {
            pendingUpdates.add(update)
            scheduleUpdate()
        }

        private fun scheduleUpdate() {
            if (!isUpdateScheduled) {
                isUpdateScheduled = true
                handler.post {
                    executeBatch()
                    isUpdateScheduled = false
                }
            }
        }

        private fun executeBatch() {
            val updates = pendingUpdates.toList()
            pendingUpdates.clear()

            updates.forEach { it() }
        }
    }

    companion object {
        fun optimizeRecyclerView(recyclerView: RecyclerView) {
            recyclerView.apply {
                // 고정 크기 설정
                setHasFixedSize(true)

                setItemViewCacheSize(20)

                // 네스티드 스크롤링 비활성화 (불필요한 경우)
                isNestedScrollingEnabled = false

                // 드래그 중 아이템 애니메이션 비활성화
                itemAnimator = null

                recycledViewPool.apply {
                    setMaxRecycledViews(0, 10)
                    setMaxRecycledViews(1, 15)
                }

                // 오버스크롤 효과 비활성화 (성능 향상)
                overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            }
        }

        fun <T> optimizedListCopy(original: List<T>): MutableList<T> {
            return when {
                original.isEmpty() -> mutableListOf<T>()
                original.size < 10 -> ArrayList<T>(original)
                else -> ArrayList<T>(original.size).apply { addAll(original) }
            }
        }

        fun optimizeViewHolderBinding(holder: RecyclerView.ViewHolder) {
            holder.itemView.isClickable = true
            holder.itemView.isLongClickable = true
        }
    }
}