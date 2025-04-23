package com.hanto.dragndrop.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class Category(
    @PrimaryKey val id: String,
    val categoryName: String
)

@Entity(tableName = "product")
data class Product(
    @PrimaryKey val id: String,
    val prName: String,
    val categoryId: String
)

@Entity(
    tableName = "category_product_setup",
    primaryKeys = ["categoryId", "productId"]
)
data class CategoryProductSetup(
    val categoryId: String,
    val productId: String, // 카테고리만 저장할 때는 빈 문자열
    val order: Int,        // 순서
    val type: Int          // 0: 카테고리, 1: 제품
)