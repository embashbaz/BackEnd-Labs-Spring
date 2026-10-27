package com.example.Smart_E_Commerce_System.service.impl;


import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.category.CategoryRequest;
import com.example.Smart_E_Commerce_System.dto.category.CategoryResponse;
import com.example.Smart_E_Commerce_System.entity.Category;
import com.example.Smart_E_Commerce_System.exception.BusinessRuleException;
import com.example.Smart_E_Commerce_System.exception.DuplicateResourceException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.CategoryRepository;
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.service.CategoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // Constructor injection: dependencies are final and explicit
    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getById(Integer id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    @Override
    public PageResponse<CategoryResponse> getAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(CategoryResponse::from));
    }

    @Override
    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = findOrThrow(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Category '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(request.description());
        return CategoryResponse.from(category); // dirty checking saves on commit
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Category category = findOrThrow(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Cannot delete category " + id + ": it still has products");
        }
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
