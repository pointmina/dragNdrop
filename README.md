# Android Drag & Drop Example

https://github.com/user-attachments/assets/c6d93b58-e5f3-4566-89fb-a437863d3019

---


## Introduction

A simple Android application demonstrating **drag-and-drop functionality between RecyclerViews**.

---

## Features

* Drag categories to add them to the active list
* Drag products to add them to the selected category
* Reorder items by dragging
* Delete items by dragging to the trash
* Save and load settings using Room Database

---

## Implementation Overview

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
            return true
        }
    }

    abstract fun onAdd(item: T)
    abstract fun onRemove(item: T)
    abstract fun onSwap(from: Int, to: Int)
}
```

