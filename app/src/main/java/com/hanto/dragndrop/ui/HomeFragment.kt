package com.hanto.dragndrop.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hanto.dragndrop.data.model.CategoryItem
import com.hanto.dragndrop.data.model.ProductItem
import com.hanto.dragndrop.data.model.UsingCategory
import com.hanto.dragndrop.databinding.FragmentHomeBinding
import com.hanto.dragndrop.ui.adapter.DragDropCallback
import com.hanto.dragndrop.ui.adapter.SaleAdapter
import com.hanto.dragndrop.ui.adapter.SaleItemClickListener
import com.hanto.dragndrop.ui.adapter.UsingCategoryAdapter
import com.hanto.dragndrop.ui.adapter.UsingProductAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(), SaleItemClickListener,
    UsingCategoryAdapter.UsingCategoryClickListener {

    private val TAG = "HomeFragment"

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by viewModels()

    private lateinit var categoryAdapter: SaleAdapter
    private lateinit var productAdapter: SaleAdapter
    private lateinit var usingCategoryAdapter: UsingCategoryAdapter
    private lateinit var usingProductAdapter: UsingProductAdapter

    // Observer 참조 관리 (메모리 누수 방지)
    private val observers = mutableListOf<Observer<*>>()

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

        setLayout()
        setupAdaptersOptimized()
        setupDragListeners()
        setupObserversWithLifecycle()
    }

    private fun setLayout() {
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    private fun setupAdaptersOptimized() {
        // 콜백 인터페이스 구현
        val dragDropCallback = object : DragDropCallback {
            override fun onCategoryAdded(category: CategoryItem) {
                viewModel.addCategoryToUsing(category)
            }

            override fun onProductAdded(product: ProductItem) {
                viewModel.addProductToSelectedCategory(product)
            }

            override fun onCategoryRemoved(categoryId: String) {
                viewModel.removeCategory(categoryId)
            }

            override fun onProductRemoved(productId: String) {
                viewModel.removeProduct(productId)
            }

            override fun onCategoriesSwapped(from: Int, to: Int) {
                viewModel.swapUsingCategories(from, to)
            }

            override fun onProductsSwapped(from: Int, to: Int) {
                viewModel.swapUsingProducts(from, to)
            }
        }

        // 완전히 콜백 방식으로 전환
        categoryAdapter = SaleAdapter(this, dragDropCallback)
        productAdapter = SaleAdapter(this, dragDropCallback)
        usingCategoryAdapter = UsingCategoryAdapter(this, dragDropCallback)
        usingProductAdapter = UsingProductAdapter(dragDropCallback)

        binding.rvCategory.apply {
            optimizeRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            addHorizontalDivider()
        }

        binding.rvProduct.apply {
            optimizeRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            addHorizontalDivider()
        }

        binding.rvUsingCategory.apply {
            optimizeRecyclerView(this)
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = usingCategoryAdapter
            setOnDragListener(usingCategoryAdapter.dragListener)
        }

        binding.rvUsingProduct.apply {
            optimizeRecyclerView(this)
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = usingProductAdapter
            setOnDragListener(usingProductAdapter.dragListener)
        }
    }

    private fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isNestedScrollingEnabled = false
            itemAnimator = null
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            recycledViewPool.apply {
                setMaxRecycledViews(0, 10) // CategoryViewHolder
                setMaxRecycledViews(1, 15) // ProductViewHolder
            }
        }
    }

    private fun RecyclerView.addHorizontalDivider() {
        addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )
    }

    private fun setupObserversWithLifecycle() {
        // Observer를 명시적으로 관리
        val categoriesObserver = Observer<List<CategoryItem>> { categories ->
            categoryAdapter.submitCategories(categories)
        }

        val productsObserver = Observer<List<ProductItem>> { products ->
            productAdapter.submitProducts(products)
        }

        val usingCategoriesObserver = Observer<List<UsingCategory>?> { usingCategories ->
            usingCategories?.let { usingCategoryAdapter.submitList(it) }
        }

        val usingProductsObserver = Observer<List<ProductItem>> { usingProducts ->
            usingProductAdapter.submitList(usingProducts)
        }

        val saveEventObserver = EventObserver<Boolean> { success ->
            val message = if (success) "저장 완료 !" else "저장 실패 !"
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        val selectedCategoryObserver = Observer<CategoryItem?> { selectedKind ->
            selectedKind?.let {
                val position = categoryAdapter.findPositionById(it.id)
                if (position != RecyclerView.NO_POSITION) {
                    categoryAdapter.selectItem(it) 
                }
            }
        }

        val inUseCategoryIdsObserver = Observer<Set<String>> { inUseIds ->
            categoryAdapter.updateInUseItems(inUseIds) 
        }

        val selectedUsingCategoryIndexObserver = Observer<Int> { index ->
            if (index != RecyclerView.NO_POSITION) {
                usingCategoryAdapter.selectItemAt(index)
                binding.rvUsingCategory.smoothScrollToPosition(index)
            } else {
                usingCategoryAdapter.selectItemAt(RecyclerView.NO_POSITION)
            }
        }

        val selectionChangedObserver = Observer<Unit> {
            viewModel.selectedUsingCategoryIndex.value?.let { index ->
                if (index >= 0) {
                    usingCategoryAdapter.selectItemAt(index)
                    binding.rvUsingCategory.scrollToPosition(index)
                }
            }
        }

        // Observer 등록
        viewModel.categories.observe(viewLifecycleOwner, categoriesObserver)
        viewModel.products.observe(viewLifecycleOwner, productsObserver)
        viewModel.usingCategories.observe(viewLifecycleOwner, usingCategoriesObserver)
        viewModel.usingProducts.observe(viewLifecycleOwner, usingProductsObserver)
        viewModel.saveEvent.observe(viewLifecycleOwner, saveEventObserver)
        viewModel.selectedCategoryItem.observe(viewLifecycleOwner, selectedCategoryObserver)
        viewModel.inUseCategoryIds.observe(viewLifecycleOwner, inUseCategoryIdsObserver)
        viewModel.selectedUsingCategoryIndex.observe(viewLifecycleOwner, selectedUsingCategoryIndexObserver)
        viewModel.selectionChangedEvent.observe(viewLifecycleOwner, selectionChangedObserver)

        // Observer 참조 저장 (명시적 해제를 위해)
        observers.addAll(listOf(
            categoriesObserver, productsObserver, usingCategoriesObserver,
            usingProductsObserver, saveEventObserver, selectedCategoryObserver,
            inUseCategoryIdsObserver, selectedUsingCategoryIndexObserver, selectionChangedObserver
        ))
    }

    private fun setupDragListeners() {
        binding.layoutTrashcan.setOnDragListener(usingCategoryAdapter.dragListener)
        binding.rvCategory.setOnDragListener(categoryAdapter.dragListener)
        binding.rvUsingCategory.setOnDragListener(usingCategoryAdapter.dragListener)
    }

    override fun onCategoryClick(category: CategoryItem) {
        viewModel.selectCategory(category)
        categoryAdapter.selectItem(category)
    }

    override fun onProductClick(product: ProductItem) {
        productAdapter.selectItem(product) 
        viewModel.addProductToSelectedCategory(product)
    }

    override fun onUsingCategoryClick(usingCategory: UsingCategory) {
        viewModel.selectUsingCategory(usingCategory)

        val position = usingCategoryAdapter.getItemPosition(usingCategory)
        if (position != RecyclerView.NO_POSITION) {
            usingCategoryAdapter.selectItem(position)
        }
    }

    override fun onDestroyView() {
        // RecyclerView 어댑터 해제 (메모리 누수 방지)
        binding.rvCategory.adapter = null
        binding.rvProduct.adapter = null
        binding.rvUsingCategory.adapter = null
        binding.rvUsingProduct.adapter = null

        // Observer들 참조 해제
        observers.clear()

        super.onDestroyView()
        _binding = null
    }
}