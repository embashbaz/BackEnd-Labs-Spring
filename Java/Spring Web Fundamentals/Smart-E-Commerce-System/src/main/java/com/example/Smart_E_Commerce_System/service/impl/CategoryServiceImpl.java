package com.example.Smart_E_Commerce_System.service.impl;

import com.example.Smart_E_Commerce_System.config.CacheNames;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Caching:
 * - getById cached per id; getAll cached per page request (categories are few and read constantly).
 * - Any change clears all cached category pages, because one change can affect every page.
 * - update also clears cached products: ProductResponse includes the category, so renaming a
 *   category would otherwise leave products showing the old name.
 */
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
    @CacheEvict(cacheNames = CacheNames.CATEGORY_LISTS, allEntries = true)
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
    @Cacheable(cacheNames = CacheNames.CATEGORIES, key = "#id")
    public CategoryResponse getById(Integer id) {
        return CategoryResponse.from(findOrThrow(id));
    }

    // Pageable works as a key: two requests with the same page, size and sort are equal.
    @Override
    @Cacheable(cacheNames = CacheNames.CATEGORY_LISTS, key = "#pageable")
    public PageResponse<CategoryResponse> getAll(Pageable pageable) {
        return PageResponse.from(categoryRepository.findAll(pageable).map(CategoryResponse::from));
    }

    // Not cached: used by GraphQL batch loading with varying id sets (poor cache hit rate).
    @Override
    public Map<Integer, CategoryResponse> getByIds(Collection<Integer> ids) {
        return categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, CategoryResponse::from));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORIES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_LISTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
    })
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

    // Products need no eviction: deleting a category that still has products is refused.
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.CATEGORIES, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.CATEGORY_LISTS, allEntries = true)
    })
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