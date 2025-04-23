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

// 사용 중인 카테고리와 제품을 위한 데이터 클래스
data class UsingCategory(
    val category: CategoryItem,
    val selectedProducts: MutableList<ProductItem> = mutableListOf()
)
