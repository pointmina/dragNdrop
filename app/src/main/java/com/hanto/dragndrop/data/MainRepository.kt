package com.hanto.dragndrop.data

import android.util.Log
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.UsingCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val dao: MainDao
) {

    suspend fun getAllCategories(): List<CategoryItem> {
        return withContext(Dispatchers.IO) {
            dao.getAllCategories().map {
                CategoryItem(
                    id = it.id,
                    categoryName = it.categoryName
                )
            }
        }
    }

    suspend fun getProductsByCategoryId(categoryId: String): List<ProductItem> {
        return withContext(Dispatchers.IO) {
            dao.getProductsByCategory(categoryId).map {
                ProductItem(
                    id = it.id,
                    prName = it.prName,
                    categoryId = it.categoryId
                )
            }
        }
    }

    suspend fun getSavedSettings(): List<UsingCategory> {
        return withContext(Dispatchers.IO) {
            try {
                val setups = dao.getAllCategoryProductSetups()

                // 데이터가 없으면 빈 리스트 반환
                if (setups.isEmpty()) {
                    return@withContext emptyList()
                }

                // 카테고리별로 그룹화
                val groupedByCategory = setups.groupBy { it.categoryId }

                // 결과 변환
                val result = mutableListOf<UsingCategory>()

                for ((categoryId, setupList) in groupedByCategory) {
                    // 카테고리 정보 찾기
                    val categoryEntity = dao.getCategoryById(categoryId)
                    if (categoryEntity != null) {
                        val categoryItem = CategoryItem(
                            id = categoryEntity.id,
                            categoryName = categoryEntity.categoryName
                        )

                        // 제품 목록 추출 (type=1인 항목)
                        val productSetups = setupList.filter {
                            it.type == 1 && it.productId.isNotEmpty()
                        }.sortedBy { it.order }

                        // 제품 정보 조회
                        val products = mutableListOf<ProductItem>()
                        for (setup in productSetups) {
                            val productEntity = dao.getProductById(setup.productId)
                            if (productEntity != null) {
                                val product = ProductItem(
                                    id = productEntity.id,
                                    prName = productEntity.prName,
                                    categoryId = productEntity.categoryId
                                )
                                products.add(product)
                            }
                        }

                        // UsingCategory 객체 생성 및 추가
                        val usingCategory = UsingCategory(categoryItem, products.toMutableList())
                        result.add(usingCategory)
                    }
                }

                // 카테고리 순서대로 정렬
                result.sortBy { usingCategory ->
                    val categorySetup = groupedByCategory[usingCategory.category.id]
                        ?.find { it.type == 0 }?.order ?: Int.MAX_VALUE
                    categorySetup
                }

                return@withContext result
            } catch (e: Exception) {
                Log.e("RetailRepository", "설정 로드 실패", e)
                return@withContext emptyList()
            }
        }
    }

    suspend fun saveSettings(settings: List<UsingCategory>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 기존 설정 모두 삭제
                dao.deleteAllCategoryProductSetups()

                // 새 설정 저장
                settings.forEachIndexed { categoryIndex, usingCategory ->
                    val categoryId = usingCategory.category.id

                    // 카테고리 항목 저장
                    val categorySetup = CategoryProductSetup(
                        categoryId = categoryId,
                        productId = "",
                        order = categoryIndex,
                        type = 0
                    )
                    dao.insertCategoryProductSetup(categorySetup)

                    // 제품 항목 저장
                    usingCategory.selectedProducts.forEachIndexed { productIndex, product ->
                        val productSetup = CategoryProductSetup(
                            categoryId = categoryId,
                            productId = product.id,
                            order = productIndex,
                            type = 1
                        )
                        dao.insertCategoryProductSetup(productSetup)
                    }
                }

                true
            } catch (e: Exception) {
                Log.e("RetailRepository", "설정 저장 실패", e)
                false
            }
        }
    }

    suspend fun initDummyData() {
        withContext(Dispatchers.IO) {
            try {
                // 카테고리 더미 데이터
                val categories = listOf(
                    Category(id = "K001", categoryName = "한식"),
                    Category(id = "K002", categoryName = "일식"),
                    Category(id = "K003", categoryName = "중식"),
                    Category(id = "K004", categoryName = "양식"),
                    Category(id = "K005", categoryName = "음료")
                )

                // 제품 더미 데이터
                val products = listOf(
                    // 한식
                    Product(id = "P001", prName = "김치찌개", categoryId = "K001"),
                    Product(id = "P002", prName = "된장찌개", categoryId = "K001"),
                    Product(id = "P003", prName = "비빔밥", categoryId = "K001"),
                    // 일식
                    Product(id = "P004", prName = "초밥", categoryId = "K002"),
                    Product(id = "P005", prName = "라멘", categoryId = "K002"),
                    // 중식
                    Product(id = "P006", prName = "짜장면", categoryId = "K003"),
                    Product(id = "P007", prName = "탕수육", categoryId = "K003"),
                    // 양식
                    Product(id = "P008", prName = "스테이크", categoryId = "K004"),
                    Product(id = "P009", prName = "파스타", categoryId = "K004"),
                    // 음료
                    Product(id = "P010", prName = "아메리카노", categoryId = "K005"),
                    Product(id = "P011", prName = "생과일주스", categoryId = "K005")
                )

                // DB에 저장
                categories.forEach { dao.insertCategory(it) }
                products.forEach { dao.insertProduct(it) }

            } catch (e: Exception) {
                Log.e("RetailRepository", "더미 데이터 초기화 실패", e)
            }
        }
    }
}