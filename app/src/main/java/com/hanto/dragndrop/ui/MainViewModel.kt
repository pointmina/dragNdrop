package com.hanto.dragndrop.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.data.MainRepository
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.UsingCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val TAG = "MainViewModel"

    // 왼쪽 패널 상태
    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products: StateFlow<List<ProductItem>> = _products.asStateFlow()

    // 오른쪽 패널 상태
    private val _usingCategories = MutableStateFlow<List<UsingCategory>>(emptyList())
    val usingCategories: StateFlow<List<UsingCategory>> = _usingCategories.asStateFlow()

    private val _selectedUsingCategoryItem = MutableStateFlow<UsingCategory?>(null)
    val selectedUsingCategoryItem: StateFlow<UsingCategory?> =
        _selectedUsingCategoryItem.asStateFlow()

    private val _usingProducts = MutableStateFlow<List<ProductItem>>(emptyList())
    val usingProducts: StateFlow<List<ProductItem>> = _usingProducts.asStateFlow()

    // 선택된 카테고리
    private val _selectedCategoryItem = MutableStateFlow<CategoryItem?>(null)
    val selectedCategoryItem: StateFlow<CategoryItem?> = _selectedCategoryItem.asStateFlow()

    // 변경 여부 추적
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    // 사용 중인 아이템 ID (계산된 상태)
    val inUseCategoryIds: StateFlow<Set<String>> = _usingCategories
        .map { categories -> categories.map { it.category.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val inUseProductIds: StateFlow<Set<String>> = _usingCategories
        .map { categories ->
            categories.flatMap { it.selectedProducts }.map { it.id }.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    // 선택된 사용 중인 카테고리 인덱스 (계산된 상태)
    val selectedUsingCategoryIndex: StateFlow<Int> = combine(
        _usingCategories,
        _selectedUsingCategoryItem
    ) { categories, selected ->
        if (selected == null) RecyclerView.NO_POSITION
        else categories.indexOfFirst { it.category.id == selected.category.id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecyclerView.NO_POSITION
    )

    // 이벤트 (일회성)
    private val _saveEvent = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val saveEvent: SharedFlow<Boolean> = _saveEvent.asSharedFlow()

    private val _selectionChangedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val selectionChangedEvent: SharedFlow<Unit> = _selectionChangedEvent.asSharedFlow()

    init {
        initData()
    }

    private fun initData() {
        viewModelScope.launch {
            // 더미 데이터 초기화 후 데이터 로드
            repository.initDummyData()

            // 카테고리 로드
            loadCategories()

            // 저장된 설정 로드
            loadSavedSettings()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val result = repository.getAllCategories()
                _categories.value = result
                Log.d(TAG, "카테고리 로드 완료: ${result.size}개")
            } catch (e: Exception) {
                Log.e(TAG, "카테고리 로딩 실패", e)
            }
        }
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            try {
                val savedSettings = repository.getSavedSettings()
                _usingCategories.value = savedSettings

                val firstCategory = savedSettings.firstOrNull()
                _selectedUsingCategoryItem.value = firstCategory
                _usingProducts.value = firstCategory?.selectedProducts ?: emptyList()

                _hasChanges.value = false
                Log.d(TAG, "저장된 설정 로드 완료: ${savedSettings.size}개 카테고리")
            } catch (e: Exception) {
                Log.e(TAG, "설정 로드 실패", e)
            }
        }
    }

    // 카테고리 선택
    fun selectCategory(category: CategoryItem?) {
        if (category == null) {
            _products.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val result = repository.getProductsByCategoryId(category.id)
                _products.value = result
                _selectedCategoryItem.value = category
                Log.d(TAG, "카테고리 선택: ${category.categoryName}, 제품 ${result.size}개")
            } catch (e: Exception) {
                Log.e(TAG, "제품 로딩 실패", e)
            }
        }
    }

    // 카테고리를 사용 중 목록에 추가
    fun addCategoryToUsing(category: CategoryItem) {
        val currentList = _usingCategories.value.toMutableList()
        val existingCategory = currentList.find { it.category.id == category.id }

        if (existingCategory != null) {
            selectUsingCategory(existingCategory)
        } else {
            val newUsingCategory = UsingCategory(category)
            currentList.add(newUsingCategory)
            _usingCategories.value = currentList
            selectUsingCategory(newUsingCategory)
            _hasChanges.value = true
            Log.d(TAG, "카테고리 추가: ${category.categoryName}")
        }
    }

    // 사용 중인 카테고리 선택
    fun selectUsingCategory(usingCategory: UsingCategory?) {
        _selectedUsingCategoryItem.value = usingCategory
        _usingProducts.value = usingCategory?.selectedProducts ?: emptyList()
        Log.d(TAG, "사용 중인 카테고리 선택: ${usingCategory?.category?.categoryName}")
    }

    // 제품을 현재 선택된 카테고리에 추가
    fun addProductToSelectedCategory(product: ProductItem) {
        val selectedCategory = _selectedUsingCategoryItem.value ?: return

        if (selectedCategory.selectedProducts.none { it.id == product.id }) {
            val updatedProducts = selectedCategory.selectedProducts.toMutableList().apply {
                add(product)
            }

            val updatedCategory = selectedCategory.copy(selectedProducts = updatedProducts)

            val currentList = _usingCategories.value.toMutableList()
            val index = currentList.indexOfFirst { it.category.id == selectedCategory.category.id }

            if (index >= 0) {
                currentList[index] = updatedCategory
                _usingCategories.value = currentList
                _selectedUsingCategoryItem.value = updatedCategory
                _usingProducts.value = updatedProducts
                _hasChanges.value = true

                Log.d(TAG, "제품 추가됨: ${product.prName}, 총 ${updatedProducts.size}개")
            }
        }
    }

    // 카테고리 삭제
    fun removeCategory(categoryId: String) {
        Log.d(TAG, "카테고리 삭제 요청: $categoryId")

        val currentCategories = _usingCategories.value.toMutableList()
        val isSelectedCategory = _selectedUsingCategoryItem.value?.category?.id == categoryId

        val updatedCategories = currentCategories.filter { it.category.id != categoryId }
        _usingCategories.value = updatedCategories

        if (isSelectedCategory) {
            _selectedUsingCategoryItem.value = null
            _usingProducts.value = emptyList()
        }

        _selectionChangedEvent.tryEmit(Unit)
        _hasChanges.value = true

        Log.d(TAG, "카테고리 삭제 완료, 남은 카테고리: ${updatedCategories.size}개")
    }

    // 제품 삭제
    fun removeProduct(productId: String) {
        Log.d(TAG, "제품 삭제 요청: $productId")

        val selectedCategory = _selectedUsingCategoryItem.value ?: return

        val updatedProducts = selectedCategory.selectedProducts
            .filter { it.id != productId }
            .toMutableList()

        val updatedCategory = selectedCategory.copy(selectedProducts = updatedProducts)

        _selectedUsingCategoryItem.value = updatedCategory
        _usingProducts.value = updatedProducts

        val currentList = _usingCategories.value.toMutableList()
        val updatedList = currentList.map {
            if (it.category.id == selectedCategory.category.id) updatedCategory else it
        }
        _usingCategories.value = updatedList

        _hasChanges.value = true
        Log.d(TAG, "제품 삭제 완료, 남은 제품: ${updatedProducts.size}개")
    }

    // 카테고리 순서 변경
    fun swapUsingCategories(from: Int, to: Int) {
        val currentList = _usingCategories.value.toMutableList()
        Collections.swap(currentList, from, to)
        _usingCategories.value = currentList
        _hasChanges.value = true
        Log.d(TAG, "카테고리 순서 변경: $from -> $to")
    }

    // 제품 순서 변경
    fun swapUsingProducts(from: Int, to: Int) {
        val selectedCategory = _selectedUsingCategoryItem.value ?: return
        val updatedProducts = selectedCategory.selectedProducts.toMutableList()
        Collections.swap(updatedProducts, from, to)

        val updatedCategory = selectedCategory.copy(selectedProducts = updatedProducts)

        _selectedUsingCategoryItem.value = updatedCategory
        _usingProducts.value = updatedProducts

        // 전체 목록도 업데이트
        val currentList = _usingCategories.value.toMutableList()
        val updatedList = currentList.map {
            if (it.category.id == selectedCategory.category.id) updatedCategory else it
        }
        _usingCategories.value = updatedList

        _hasChanges.value = true
        Log.d(TAG, "제품 순서 변경: $from -> $to")
    }

    // 저장 기능
    fun saveSettings() {
        if (!_hasChanges.value) {
            _saveEvent.tryEmit(true)
            return
        }

        viewModelScope.launch {
            try {
                val success = repository.saveSettings(_usingCategories.value)

                if (success) {
                    _hasChanges.value = false
                    _saveEvent.tryEmit(true)
                    Log.d(TAG, "설정 저장 성공")
                } else {
                    _saveEvent.tryEmit(false)
                    Log.e(TAG, "설정 저장 실패")
                }
            } catch (e: Exception) {
                Log.e(TAG, "설정 저장 중 오류", e)
                _saveEvent.tryEmit(false)
            }
        }
    }
}