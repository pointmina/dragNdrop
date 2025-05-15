# Android Drag & Drop Example

https://github.com/user-attachments/assets/c6d93b58-e5f3-4566-89fb-a437863d3019

---

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" />
  <img src="https://img.shields.io/badge/Language-Kotlin-blue.svg" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-ff69b4.svg" />
  <img src="https://img.shields.io/badge/DI-Hilt-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Database-Room-orange.svg" />
</p>

---

This is a simple Android app demonstrating various **drag-and-drop features** using `RecyclerView`.  
It supports reordering items, moving between lists, and deleting by dragging to a trash area.

---

## ✨ Features

- ✅ Reorder items within a single `RecyclerView`
- ✅ Move items **between different RecyclerViews**
- ✅ Drag items to a **trash can** to delete them

---

## 🛠 Implementation Overview

The core logic is encapsulated in an abstract class called `RecyclerViewDragAdapter`,  
which provides reusable drag-and-drop behavior for adapters.

```kotlin
abstract class RecyclerViewDragAdapter<T, VH : RecyclerView.ViewHolder>(
    diffUtil: DiffUtil.ItemCallback<T>
) : ListAdapter<T, VH>(diffUtil) {

    abstract val isSwappable: Boolean

    val dragListener = object : View.OnDragListener {
        override fun onDrag(view: View?, event: DragEvent?): Boolean {
            // Handle drag events
            // ...
        }
    }

    abstract fun onAdd(item: T)
    abstract fun onRemove(item: T)
    abstract fun onSwap(from: Int, to: Int)
}
````
