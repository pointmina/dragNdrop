package com.hanto.dragndrop.ui

import android.os.Bundle
import android.util.Log
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


    private val TAG = "HomeFragment"

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
        Log.d(TAG, "onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setLayout()
        setupAdapters()
        setupDragListeners()
        setupObservers()
    }

    private fun setLayout() {
        binding.btnSave.setOnClickListener {
            viewModel.saveSettings()
        }
    }

    private fun setupAdapters() {

        // 왼쪽 상단 어댑터 초기화
        categoryAdapter = SaleAdapter(this, viewModel)
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            addHorizontalDivider()
        }

        // 왼쪽 하단 어댑터 초기화
        productAdapter = SaleAdapter(this, viewModel)
        binding.rvProduct.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productAdapter
            addHorizontalDivider()
        }

        // 오른쪽 - 사용 중인 분류 어댑터 설정
        usingCategoryAdapter = UsingCategoryAdapter(this, viewModel)
        binding.rvUsingCategory.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = usingCategoryAdapter
            setOnDragListener(usingCategoryAdapter.dragListener)
        }

        // 오른쪽 - 사용 중인 제품 어댑터
        usingProductAdapter = UsingProductAdapter(viewModel)
        binding.rvUsingProduct.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = usingProductAdapter
            setOnDragListener(usingProductAdapter.dragListener)
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


    private fun setupObservers() {
        // ViewModel의 LiveData 관찰
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitCategories(categories)
        }

        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitProducts(products)
        }

        // 사용 중인 제품 데이터 관찰 - UsingCategoryAdapter
        viewModel.usingCategories.observe(viewLifecycleOwner) { usingCategories ->
            if (usingCategories != null) {
                usingCategoryAdapter.submitList(usingCategories)
            }
        }

        // 사용 중인 제품 데이터 관찰 - UsingProductAdapter
        viewModel.usingProducts.observe(viewLifecycleOwner) { usingProducts ->
            usingProductAdapter.submitList(usingProducts)
        }

        viewModel.saveEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { success ->
                if (success) {
                    Toast.makeText(requireContext(), "저장 완료 !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "저장 실패 !", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 왼쪽 패널 카테고리의 선택 상태 관찰 (추가)
        viewModel.selectedCategoryItem.observe(viewLifecycleOwner) { selectedKind ->
            selectedKind?.let {
                // 카테고리 어댑터에서 해당 항목 선택 처리
                val position = categoryAdapter.findPositionById(selectedKind.id)
                if (position != RecyclerView.NO_POSITION) {
                    categoryAdapter.selectItem(selectedKind)
                }
            }
        }

        // 사용 중인 카테고리 ID 관찰
        viewModel.inUseCategoryIds.observe(viewLifecycleOwner) { inUseIds ->
            categoryAdapter.updateInUseItems(inUseIds)
        }

        // 선택 변경 이벤트 관찰
        viewModel.selectedUsingCategoryIndex.observe(viewLifecycleOwner) { index ->
            if (index != RecyclerView.NO_POSITION) {
                usingCategoryAdapter.selectItemAt(index)
                binding.rvUsingCategory.smoothScrollToPosition(index)
            } else {
                usingCategoryAdapter.selectItemAt(RecyclerView.NO_POSITION)
            }
        }

        viewModel.selectionChangedEvent.observe(viewLifecycleOwner) {
            viewModel.selectedUsingCategoryIndex.value?.let { index ->
                if (index >= 0) {
                    // 어댑터에게 명시적으로 선택 업데이트 지시
                    usingCategoryAdapter.selectItemAt(index)

                    // 스크롤 위치 조정
                    binding.rvUsingCategory.scrollToPosition(index)
                    usingCategoryAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupDragListeners() {
        binding.layoutTrashcan.setOnDragListener(usingCategoryAdapter.dragListener)
        binding.rvCategory.setOnDragListener(categoryAdapter.dragListener)
        binding.rvUsingCategory.setOnDragListener(usingCategoryAdapter.dragListener)
    }

    override fun onCategoryClick(category: CategoryItem) {
        // 카테고리 클릭 처리
        viewModel.selectCategory(category)
        categoryAdapter.selectItem(category)
    }

    override fun onProductClick(product: ProductItem) {
        // 제품 클릭 처리 - 선택된 카테고리에 추가
        productAdapter.selectItem(product)
        viewModel.addProductToSelectedCategory(product)
    }

    override fun onUsingCategoryClick(usingCategory: UsingCategory) {
        // 사용 중인 카테고리 클릭 처리
        viewModel.selectUsingCategory(usingCategory)

        val position = usingCategoryAdapter.getItemPosition(usingCategory)
        if (position != RecyclerView.NO_POSITION) {
            usingCategoryAdapter.selectItem(position)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}