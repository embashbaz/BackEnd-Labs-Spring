package com.example.Smart_E_Commerce_System.service.impl;


import com.example.Smart_E_Commerce_System.config.CacheNames;
import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductFilter;
import com.example.Smart_E_Commerce_System.dto.product.ProductRequest;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;
import com.example.Smart_E_Commerce_System.entity.Category;
import com.example.Smart_E_Commerce_System.entity.Inventory;
import com.example.Smart_E_Commerce_System.entity.Product;
import com.example.Smart_E_Commerce_System.exception.BusinessRuleException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.CategoryRepository;
import com.example.Smart_E_Commerce_System.repository.InventoryRepository;
import com.example.Smart_E_Commerce_System.repository.OrderItemRepository;
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.repository.spec.ProductSpecifications;
import com.example.Smart_E_Commerce_System.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Caching:
 * - getById is cached per product id.
 * - search is NOT cached: every filter/page/sort combination would be its own entry,
 *   so few requests would ever hit the cache, and every product change would have to clear them all.
 * - Any product change also clears the reports cache (top products show product names,
 *   low stock depends on new products' initial stock).
 */
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              InventoryRepository inventoryRepository,
                              OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // A new product has a new id, so nothing in the products cache can be stale.
    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.REPORTS, allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        Product saved = productRepository.save(product);

        // Every product gets an inventory row (1:1, shares the product's id)
        Inventory inventory = new Inventory();
        inventory.setProduct(saved);
        inventory.setQuantityOnHand(request.initialStock() != null ? request.initialStock() : 0);
        inventoryRepository.save(inventory);

        return ProductResponse.from(saved);
    }

    // First call for an id runs the method and stores the result; later calls return the stored copy.
    // A not-found exception is not cached, so a product created later is found normally.
    @Override
    @Cacheable(cacheNames = CacheNames.PRODUCTS, key = "#id")
    public ProductResponse getById(Integer id) {
        return ProductResponse.from(findOrThrow(id));
    }

    @Override
    public PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new BusinessRuleException("minPrice cannot be greater than maxPrice");
        }
        return PageResponse.from(
                productRepository.findAll(ProductSpecifications.fromFilter(filter), pageable)
                        .map(ProductResponse::from));
    }

    /**
     * Derived query: Spring reads findByCategoryNameIgnoreCase as
     * "products whose category.name equals this value, ignoring case" and generates the join.
     * Checks the category exists first, so a typo gives 404 instead of an empty page.
     * Not cached, for the same reason as search: many name/page/sort combinations.
     */
    @Override
    public PageResponse<ProductResponse> getByCategoryName(String categoryName, Pageable pageable) {
        String name = categoryName.trim();
        if (!categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceNotFoundException("Category", name);
        }
        return PageResponse.from(
                productRepository.findByCategoryNameIgnoreCase(name, pageable).map(ProductResponse::from));
    }

    /** Derived query: findByPriceBetween generates "price BETWEEN :min AND :max" (both inclusive). */
    @Override
    public PageResponse<ProductResponse> getByPriceRange(BigDecimal min, BigDecimal max, Pageable pageable) {
        if (min.compareTo(max) > 0) {
            throw new BusinessRuleException("min cannot be greater than max");
        }
        return PageResponse.from(
                productRepository.findByPriceBetween(min, max, pageable).map(ProductResponse::from));
    }

    // Remove the old copy; the next getById loads the updated product.
    // Eviction happens only if the method succeeds (a failed update changes nothing).
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.REPORTS, allEntries = true)
    })
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request);
        return ProductResponse.from(productRepository.saveAndFlush(product)); // flush so updatedAt is set
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.REPORTS, allEntries = true)
    })
    public void delete(Integer id) {
        Product product = findOrThrow(id);
        // Check first for a clear message; ON DELETE RESTRICT in the DB still guards against
        // an order being placed for this product between this check and the delete.
        if (orderItemRepository.existsByProductId(id)) {
            throw new BusinessRuleException("Cannot delete product " + id + ": it appears in orders");
        }
        // Inventory row is removed by ON DELETE CASCADE in the DB.
        productRepository.delete(product);
        productRepository.flush();
    }

    private void applyRequest(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
        product.setCategory(category);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
    }

    private Product findOrThrow(Integer id) {
        return productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }
}