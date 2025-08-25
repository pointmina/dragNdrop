package com.hanto.dragndrop.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.data.MainRepository
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.UsingCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    // 왼쪽 패널 데이터
    private val _categories = MutableLiveData<List<CategoryItem>>()
    val categories: LiveData<List<CategoryItem>> = _categories

    private val _products = MutableLiveData<List<ProductItem>>()
    val products: LiveData<List<ProductItem>> = _products

    // 오른쪽 패널 데이터
    private val _usingCategories = MutableLiveData<List<UsingCategory>?>()
    val usingCategories: MutableLiveData<List<UsingCategory>?> = _usingCategories

    private val _selectedUsingCategoryItem = MutableLiveData<UsingCategory?>()
    val selectedUsingCategoryItem: LiveData<UsingCategory?> = _selectedUsingCategoryItem

    private val _usingProducts = MutableLiveData<List<ProductItem>>()
    val usingProducts: LiveData<List<ProductItem>> = _usingProducts

    // 선택된 카테고리
    private val _selectedCategoryItem = MutableLiveData<CategoryItem?>()
    val selectedCategoryItem: LiveData<CategoryItem?> = _selectedCategoryItem

    // 변경 여부 추적
    private val _hasChanges = MutableLiveData<Boolean>(false)
    val hasChanges: LiveData<Boolean> = _hasChanges

    // 사용 중인 아이템 ID 추적
    private val _inUseCategoryIds = MutableLiveData<Set<String>>(emptySet())
    val inUseCategoryIds: LiveData<Set<String>> = _inUseCategoryIds

    private val _inUseProductIds = MutableLiveData<Set<String>>(emptySet())
    val inUseProductIds: LiveData<Set<String>> = _inUseProductIds

    // 저장 이벤트
    private val _saveEvent = MutableLiveData<Event<Boolean>>()
    val saveEvent: LiveData<Event<Boolean>> = _saveEvent

    private val _selectionChangedEvent = MutableLiveData<Unit>()
    val selectionChangedEvent: LiveData<Unit> = _selectionChangedEvent

    //선택된 분류 아이템 인덱스
    private val _selectedUsingCategoryIndex = MutableLiveData<Int>()
    val selectedUsingCategoryIndex: LiveData<Int> = _selectedUsingCategoryIndex

    init {
        // 더미 데이터 초기화 후 데이터 로드
        initData()
    }

    private fun initData() {
        viewModelScope.launch {
            // 더미 데이터 초기화
            repository.initDummyData()

            // 카테고리 로드
            loadCategories()

            // 저장된 설정 로드
            loadSavedSettings()
        }

        // 초기에는 아무것도 선택하지 않음
        _selectedUsingCategoryItem.value = null
        _selectedCategoryItem.value = null
        _usingProducts.value = emptyList()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val result = repository.getAllCategories()
                _categories.value = result

            } catch (e: Exception) {
                Log.e("MainViewModel", "카테고리 로딩 실패", e)
            }
        }
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            try {
                val savedSettings = repository.getSavedSettings()

                _usingCategories.value = savedSettings
                _selectedUsingCategoryItem.value = savedSettings.firstOrNull()
                _usingProducts.value =
                    _selectedUsingCategoryItem.value?.selectedProducts ?: emptyList()

                // 사용 중인 아이템 ID 업데이트
                updateInUseItems()

                _hasChanges.value = false
            } catch (e: Exception) {
                Log.e("MainViewModel", "설정 로드 실패", e)
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
                // 선택된 카테고리에 속한 제품 로드
                val result = repository.getProductsByCategoryId(category.id)
                _products.value = result
                _selectedCategoryItem.value = category
            } catch (e: Exception) {
                Log.e("MainViewModel", "제품 로딩 실패", e)
            }
        }
    }

    // 카테고리를 사용 중 목록에 추가
    fun addCategoryToUsing(category: CategoryItem) {
        val currentList = _usingCategories.value?.toMutableList() ?: mutableListOf()

        // 이미 추가된 카테고리인지 확인
        val existingCategory = currentList.find { it.category.id == category.id }

        if (existingCategory != null) {
            // 이미 존재하는 카테고리면 선택만
            selectUsingCategory(existingCategory)
        } else {
            // 새로운 카테고리 추가 및 선택
            val newUsingCategory = UsingCategory(category)
            currentList.add(newUsingCategory)
            _usingCategories.value = currentList
            selectUsingCategory(newUsingCategory)

            // 변경 여부 표시
            _hasChanges.value = true

            // 사용 중인 아이템 ID 업데이트
            updateInUseItems()
        }
    }

    // 사용 중인 카테고리 선택
    fun selectUsingCategory(usingCategory: UsingCategory?) {
        _selectedUsingCategoryItem.value = usingCategory
        _usingProducts.value = usingCategory?.selectedProducts ?: emptyList()

        // 선택된 카테고리의 제품 ID 목록만 업데이트
        _inUseProductIds.value = usingCategory?.selectedProducts?.map { it.prName }?.toSet()
    }

    // 제품을 현재 선택된 카테고리에 추가
    fun addProductToSelectedCategory(product: ProductItem) {
        val selectedCategory = _selectedUsingCategoryItem.value ?: return

        // 이미 추가된 제품인지 확인
        if (selectedCategory.selectedProducts.none { it.id == product.id }) {
            try {
                // 새 목록 생성 (불변성 유지)
                val updatedProducts = selectedCategory.selectedProducts.toMutableList()
                updatedProducts.add(product)

                val updatedCategory = selectedCategory.copy(selectedProducts = updatedProducts)

                // 전체 using 카테고리 목록 갱신
                val currentList = _usingCategories.value?.toMutableList() ?: mutableListOf()
                val index =
                    currentList.indexOfFirst { it.category.id == selectedCategory.category.id }

                if (index >= 0) {
                    currentList[index] = updatedCategory

                    // UI 업데이트를 순서대로 진행
                    _usingCategories.value = ArrayList(currentList)
                    _selectedUsingCategoryItem.value = updatedCategory
                    _usingProducts.value = ArrayList(updatedProducts)

                    Log.d("MainViewModel", "제품 추가됨: ${product.prName}, 총 ${updatedProducts.size}개")
                }

                // 변경 여부 표시
                _hasChanges.value = true

                // 사용 중인 아이템 ID 업데이트
                updateInUseItems()
            } catch (e: Exception) {
                Log.e("MainViewModel", "제품 추가 중 오류", e)
            }
        }
    }

    // 카테고리 삭제 함수
    fun removeCategory(categoryId: String) {
        Log.d("MainViewModel", "카테고리 삭제 요청: $categoryId")

        // 현재 카테고리 목록 가져오기
        val currentCategories = _usingCategories.value?.toMutableList() ?: mutableListOf()

        // 삭제될 카테고리의 인덱스 확인
        val deletedIndex = currentCategories.indexOfFirst { it.category.id == categoryId }
        val isSelectedCategory = _selectedUsingCategoryItem.value?.category?.id == categoryId

        // 카테고리 삭제
        val updatedCategories = currentCategories.filter { it.category.id != categoryId }
        _usingCategories.value = updatedCategories

        // 선택된 카테고리가 삭제된 경우
        if (isSelectedCategory) {
            _selectedUsingCategoryIndex.value = RecyclerView.NO_POSITION
            _selectedUsingCategoryItem.value = null
        }

        // 변경 이벤트 발생
        _selectionChangedEvent.value = Unit

        // 변경 여부 표시
        _hasChanges.value = true

        // 사용 중인 아이템 ID 업데이트
        updateInUseItems()

        Log.d("MainViewModel", "카테고리 삭제 완료, 남은 카테고리: ${updatedCategories.size}개")
    }

    // 제품 삭제 함수
    fun removeProduct(productId: String) {
        Log.d("MainViewModel", "제품 삭제 요청: $productId")

        // 현재 선택된 카테고리 확인
        val selectedCategory = _selectedUsingCategoryItem.value ?: return

        // 제품 삭제
        val updatedProducts =
            selectedCategory.selectedProducts.filter { it.id != productId }.toMutableList()

        // 새 UsingCategory 객체 생성
        val updatedCategory = selectedCategory.copy(selectedProducts = updatedProducts)

        // UI 업데이트
        _selectedUsingCategoryItem.value = updatedCategory
        _usingProducts.value = ArrayList(updatedProducts)

        // 전체 using 카테고리 목록 갱신
        val currentList = _usingCategories.value?.toMutableList() ?: mutableListOf()
        val updatedList = currentList.map {
            if (it.category.id == selectedCategory.category.id) updatedCategory else it
        }
        _usingCategories.value = ArrayList(updatedList)

        // 변경 여부 표시
        _hasChanges.value = true

        // 사용 중인 아이템 ID 업데이트
        updateInUseItems()

        Log.d("MainViewModel", "제품 삭제 완료, 남은 제품: ${updatedProducts.size}개")
    }

    //카테고리 순서 변경
    fun swapUsingCategories(from: Int, to: Int) {
        _usingCategories.value?.let {
            val mutableList = it.toMutableList()
            Collections.swap(mutableList, from, to)
            _usingCategories.value = mutableList
        }

        _hasChanges.value = true
    }

    //제품 순서 변경
    fun swapUsingProducts(from: Int, to: Int) {
        _usingProducts.value?.let {
            val mutableList = it.toMutableList()
            Collections.swap(mutableList, from, to)
            _usingProducts.value = mutableList
        }

        _hasChanges.value = true
    }

    // 사용 중인 아이템 ID 업데이트
    private fun updateInUseItems() {
        val categories = _usingCategories.value ?: emptyList()

        // 사용 중인 카테고리 ID 수집
        val categoryIds = categories.map { it.category.id }.toSet()
        _inUseCategoryIds.value = categoryIds

        // 사용 중인 제품 ID 수집
        val productIds = categories.flatMap { it.selectedProducts }.map { it.id }.toSet()
        _inUseProductIds.value = productIds
    }

    // 저장 기능
    fun saveSettings() {
        if (_hasChanges.value != true) {
            _saveEvent.value = Event(true)
            return
        }

        viewModelScope.launch {
            try {
                val currentCategories = _usingCategories.value ?: emptyList()

                // 설정 저장
                val success = repository.saveSettings(currentCategories)

                if (success) {
                    // 저장 성공 후 변경 상태 초기화
                    _hasChanges.value = false
                    _saveEvent.value = Event(true)
                } else {
                    _saveEvent.value = Event(false)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "설정 저장 실패", e)
                _saveEvent.value = Event(false)
            }
        }
    }
}