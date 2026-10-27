package com.example.Smart_E_Commerce_System.service.impl;


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
import com.example.Smart_E_Commerce_System.repository.ProductRepository;
import com.example.Smart_E_Commerce_System.repository.spec.ProductSpecifications;
import com.example.Smart_E_Commerce_System.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
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

    @Override
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

    @Override
    @Transactional
    public ProductResponse update(Integer id, ProductRequest request) {
        Product product = findOrThrow(id);
        applyRequest(product, request);
        return ProductResponse.from(productRepository.saveAndFlush(product)); // flush so updatedAt is set
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        // Inventory row is removed by ON DELETE CASCADE in the DB.
        // Products already in orders are protected by ON DELETE RESTRICT -> 409 via handler.
        productRepository.delete(findOrThrow(id));
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
