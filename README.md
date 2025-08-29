# Android Drag & Drop Example


https://github.com/user-attachments/assets/df738a4e-c59e-4679-8fe5-5327a2d8a7b8


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

## Implementation Highlights

### **DragCapable Interface**
Core abstraction enabling any RecyclerView adapter to participate in drag operations:

```kotlin
interface DragCapable {
    fun isSwappable(): Boolean
    fun getItemForDrag(position: Int): Any?
    fun onDragAdd(item: Any)
    fun onDragRemove(item: Any)
    fun onDragSwap(fromPosition: Int, toPosition: Int)
}
```

### **Centralized Drag Management**
DragManager handles complex cross-adapter interactions with rule-based validation:

```kotlin
class DragManager {
    companion object {
        private val DRAG_RULES = listOf(
            DragRule(
                sourceId = R.id.rv_category,
                targetId = R.id.rv_using_category,
                isAllowed = true,
                needsConversion = true // CategoryItem -> UsingCategory
            ),
            DragRule(
                sourceId = R.id.rv_product,
                targetId = R.id.rv_using_product,
                isAllowed = true
            )
        )
    }
}
```

## Setup & Usage

### **Requirements**
- Android API 24+ (Android 7.0)
- Kotlin 1.9+
- Gradle 8.0+

### **Dependencies**
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("androidx.fragment:fragment-ktx:1.6.2")
implementation("com.google.dagger:hilt-android:2.48")
implementation("androidx.room:room-ktx:2.6.1")
```
