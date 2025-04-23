package com.hanto.dragndrop.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MainDao {
    // 모든 카테고리 조회
    @Query("SELECT * FROM category ORDER BY categoryName")
    suspend fun getAllCategories(): List<Category>

    // ID로 카테고리 조회
    @Query("SELECT * FROM category WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: String): Category?

    // 카테고리에 속한 제품 조회
    @Query("SELECT * FROM product WHERE categoryId = :categoryId ORDER BY prName")
    suspend fun getProductsByCategory(categoryId: String): List<Product>

    // ID로 제품 조회
    @Query("SELECT * FROM product WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): Product?

    // 모든 설정 조회 (카테고리 및 제품)
    @Query("SELECT * FROM category_product_setup ORDER BY categoryId, type, `order`")
    suspend fun getAllCategoryProductSetups(): List<CategoryProductSetup>

    // 모든 설정 삭제
    @Query("DELETE FROM category_product_setup")
    suspend fun deleteAllCategoryProductSetups()

    // 설정 추가
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryProductSetup(setup: CategoryProductSetup)

    // 더미 데이터 - 카테고리 추가
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    // 더미 데이터 - 제품 추가
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)
}
