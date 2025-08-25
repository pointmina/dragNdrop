package com.hanto.dragndrop.data.model

interface SaleItem {
    val id: String
}

data class CategoryItem(
    override val id: String,
    val categoryName: String
) : SaleItem

data class ProductItem(
    override val id: String,
    val prName: String,
    val categoryId: String
) : SaleItem

data class UsingCategory(
    val category: CategoryItem,
    val selectedProducts: MutableList<ProductItem> = mutableListOf()
)
