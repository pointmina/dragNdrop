package com.hanto.dragndrop.ui.adapter

import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem


//새로운 콜백 인터페이스
interface DragDropCallback {
    fun onCategoryAdded(category: CategoryItem)
    fun onProductAdded(product: ProductItem)
    fun onCategoryRemoved(categoryId: String)
    fun onProductRemoved(productId: String)
    fun onCategoriesSwapped(from: Int, to: Int)
    fun onProductsSwapped(from: Int, to: Int)
}