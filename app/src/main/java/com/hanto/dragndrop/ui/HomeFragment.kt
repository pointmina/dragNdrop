package com.hanto.dragndrop.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.UsingCategory
import com.hanto.dragndrop.databinding.FragmentHomeBinding
import com.hanto.dragndrop.ui.adapter.SaleAdapter
import com.hanto.dragndrop.ui.adapter.SaleItemClickListener
import com.hanto.dragndrop.ui.adapter.UsingCategoryAdapter
import com.hanto.dragndrop.ui.adapter.UsingProductAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(), SaleItemClickListener,
    UsingCategoryAdapter.UsingCategoryClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private lateinit var categoryAdapter: SaleAdapter
    private lateinit var productAdapter: SaleAdapter
    private lateinit var usingCategoryAdapter: UsingCategoryAdapter
    private lateinit var usingProductAdapter: UsingProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupAdapters()
        setupObservers()
    }

    private fun setupUI() {
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    private fun setupAdapters() {
        // 어댑터 생성
        categoryAdapter = SaleAdapter(this, viewModel)
        productAdapter = SaleAdapter(this, viewModel)
        usingCategoryAdapter = UsingCategoryAdapter(this, viewModel)
        usingProductAdapter = UsingProductAdapter(viewModel)

        // RecyclerView 설정
        setupRecyclerView(binding.rvCategory, categoryAdapter, LinearLayoutManager(requireContext()), true)
        setupRecyclerView(binding.rvProduct, productAdapter, LinearLayoutManager(requireContext()), true)
        setupRecyclerView(
            binding.rvUsingCategory,
            usingCategoryAdapter,
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false),
            false
        )
        setupRecyclerView(binding.rvUsingProduct, usingProductAdapter, GridLayoutManager(requireContext(), 3), false)

        // 드래그 리스너 설정
        binding.layoutTrashcan.setOnDragListener(usingCategoryAdapter.dragListener)
        binding.rvCategory.setOnDragListener(categoryAdapter.dragListener)
        binding.rvUsingCategory.setOnDragListener(usingCategoryAdapter.dragListener)
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        adapter: RecyclerView.Adapter<*>,
        layoutManager: RecyclerView.LayoutManager,
        addDivider: Boolean = true
    ) {
        recyclerView.apply {
            this.layoutManager = layoutManager
            this.adapter = adapter

            // 성능 최적화
            setHasFixedSize(true)
            setItemViewCacheSize(20)

            // 구분선 추가
            if (addDivider && layoutManager is LinearLayoutManager && layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
        }
    }

    private fun setupObservers() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitCategories(categories)
        }

        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitProducts(products)
        }

        viewModel.usingCategories.observe(viewLifecycleOwner) { usingCategories ->
            usingCategories?.let {
                usingCategoryAdapter.submitList(it)
            }
        }

        viewModel.usingProducts.observe(viewLifecycleOwner) { usingProducts ->
            usingProductAdapter.submitList(usingProducts)
        }

        viewModel.selectedCategoryItem.observe(viewLifecycleOwner) { selectedCategory ->
            selectedCategory?.let {
                categoryAdapter.selectItem(it)
            }
        }

        viewModel.inUseCategoryIds.observe(viewLifecycleOwner) { inUseIds ->
            categoryAdapter.updateInUseItems(inUseIds)
        }

        viewModel.selectedUsingCategoryIndex.observe(viewLifecycleOwner) { index ->
            usingCategoryAdapter.setSelectedPosition(index)
            if (index >= 0) {
                binding.rvUsingCategory.smoothScrollToPosition(index)
            }
        }

        viewModel.saveEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                val message = if (success) "저장 완료!" else "저장 실패!"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 사용자 클릭 이벤트
    override fun onCategoryClick(category: CategoryItem) {
        viewModel.selectCategory(category)
    }

    override fun onProductClick(product: ProductItem) {
        viewModel.addProductToSelectedCategory(product)
    }

    override fun onUsingCategoryClick(usingCategory: UsingCategory) {
        viewModel.selectUsingCategory(usingCategory)
    }

    override fun onDestroyView() {
        // RecyclerView 정리
        binding.rvCategory.adapter = null
        binding.rvProduct.adapter = null
        binding.rvUsingCategory.adapter = null
        binding.rvUsingProduct.adapter = null

        super.onDestroyView()
        _binding = null
    }
}