package com.hanto.dragndrop.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.UsingCategory
import com.hanto.dragndrop.databinding.FragmentHomeBinding
import com.hanto.dragndrop.ui.adapter.SaleAdapter
import com.hanto.dragndrop.ui.adapter.SaleItemClickListener
import com.hanto.dragndrop.ui.adapter.UsingCategoryAdapter
import com.hanto.dragndrop.ui.adapter.UsingProductAdapter
import com.hanto.dragndrop.ui.drag.DragManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(), SaleItemClickListener,
    UsingCategoryAdapter.UsingCategoryClickListener {

    private val TAG = "HomeFragment"

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    // DragManager
    private val dragManager = DragManager()

    private lateinit var categoryAdapter: SaleAdapter
    private lateinit var productAdapter: SaleAdapter
    private lateinit var usingCategoryAdapter: UsingCategoryAdapter
    private lateinit var usingProductAdapter: UsingProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")

        setupUI()
        setupAdapters()
        setupFlowObservers()
    }

    private fun setupUI() {
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
        }

        setupTrashcanDragListener()
    }

    private fun setupTrashcanDragListener() {
        binding.layoutTrashcan.setOnDragListener { view, event ->
            dragManager.handleDragEvent(view, event)
        }
        Log.d(TAG, "휴지통 드래그 리스너 설정 완료")
    }

    private fun setupAdapters() {
        categoryAdapter = SaleAdapter(this, viewModel)
        productAdapter = SaleAdapter(this, viewModel)
        usingCategoryAdapter = UsingCategoryAdapter(this, viewModel)
        usingProductAdapter = UsingProductAdapter(viewModel)

        setupRecyclerViews()
        Log.d(TAG, "모든 어댑터 설정 완료")
    }

    private fun setupRecyclerViews() {
        // 카테고리 RecyclerView
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)

            setOnDragListener { view, event ->
                dragManager.handleDragEvent(view, event)
            }

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        // 제품 RecyclerView
        binding.rvProduct.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)

            setOnDragListener { view, event ->
                dragManager.handleDragEvent(view, event)
            }

            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }

        // 사용 중인 카테고리 RecyclerView
        binding.rvUsingCategory.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = usingCategoryAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)

            setOnDragListener { view, event ->
                dragManager.handleDragEvent(view, event)
            }
        }

        // 사용 중인 제품 RecyclerView
        binding.rvUsingProduct.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = usingProductAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)

            setOnDragListener { view, event ->
                dragManager.handleDragEvent(view, event)
            }
        }

        // DragManager에 모든 RecyclerView 등록
        dragManager.registerRecyclerView(binding.rvCategory.id, binding.rvCategory)
        dragManager.registerRecyclerView(binding.rvProduct.id, binding.rvProduct)
        dragManager.registerRecyclerView(binding.rvUsingCategory.id, binding.rvUsingCategory)
        dragManager.registerRecyclerView(binding.rvUsingProduct.id, binding.rvUsingProduct)

        Log.d(TAG, "DragManager에 모든 RecyclerView 등록 완료")
    }

    private fun setupFlowObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 여러 Flow를 병렬로 수집
                launch {
                    // 카테고리 데이터 관찰
                    viewModel.categories.collect { categories ->
                        Log.d(TAG, "카테고리 데이터 업데이트: ${categories.size}개")
                        categoryAdapter.submitCategories(categories)
                    }
                }

                launch {
                    // 제품 데이터 관찰
                    viewModel.products.collect { products ->
                        Log.d(TAG, "제품 데이터 업데이트: ${products.size}개")
                        productAdapter.submitProducts(products)
                    }
                }

                launch {
                    // 사용 중인 카테고리 관찰
                    viewModel.usingCategories.collect { usingCategories ->
                        Log.d(TAG, "사용 중인 카테고리 업데이트: ${usingCategories.size}개")
                        usingCategoryAdapter.submitList(usingCategories)
                    }
                }

                launch {
                    // 사용 중인 제품 관찰
                    viewModel.usingProducts.collect { usingProducts ->
                        Log.d(TAG, "사용 중인 제품 업데이트: ${usingProducts.size}개")
                        usingProductAdapter.submitList(usingProducts)
                    }
                }

                launch {
                    // 선택된 카테고리 관찰
                    viewModel.selectedCategoryItem.collect { selectedCategory ->
                        selectedCategory?.let {
                            categoryAdapter.selectItem(it)
                            Log.d(TAG, "카테고리 선택: ${it.categoryName}")
                        }
                    }
                }

                launch {
                    // 사용 중인 카테고리 ID 관찰
                    viewModel.inUseCategoryIds.collect { inUseIds ->
                        Log.d(TAG, "사용 중인 카테고리 ID 업데이트: ${inUseIds.size}개")
                        categoryAdapter.updateInUseItems(inUseIds)
                    }
                }

                launch {
                    // 선택된 사용 중인 카테고리 인덱스 관찰
                    viewModel.selectedUsingCategoryIndex.collect { index ->
                        Log.d(TAG, "선택된 사용 중인 카테고리 인덱스: $index")
                        usingCategoryAdapter.setSelectedPosition(index)
                        if (index >= 0) {
                            binding.rvUsingCategory.smoothScrollToPosition(index)
                        }
                    }
                }

                launch {
                    // 저장 이벤트 관찰
                    viewModel.saveEvent.collect { success ->
                        val message = if (success) "저장 완료!" else "저장 실패!"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "저장 결과: $success")
                    }
                }
            }
        }
    }

    // SaleItemClickListener 구현
    override fun onCategoryClick(category: CategoryItem) {
        Log.d(TAG, "카테고리 클릭: ${category.categoryName}")
        viewModel.selectCategory(category)
    }

    override fun onProductClick(product: ProductItem) {
        Log.d(TAG, "제품 클릭: ${product.prName}")
        viewModel.addProductToSelectedCategory(product)
    }

    // UsingCategoryClickListener 구현
    override fun onUsingCategoryClick(usingCategory: UsingCategory) {
        Log.d(TAG, "사용 중인 카테고리 클릭: ${usingCategory.category.categoryName}")
        viewModel.selectUsingCategory(usingCategory)
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView - DragManager 정리")

        dragManager.clear()

        binding.rvCategory.adapter = null
        binding.rvProduct.adapter = null
        binding.rvUsingCategory.adapter = null
        binding.rvUsingProduct.adapter = null

        super.onDestroyView()
        _binding = null
    }
}