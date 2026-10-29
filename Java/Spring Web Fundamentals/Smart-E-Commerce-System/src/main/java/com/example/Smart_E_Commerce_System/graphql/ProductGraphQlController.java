package com.example.Smart_E_Commerce_System.graphql;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.category.CategoryResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductFilter;
import com.example.Smart_E_Commerce_System.dto.product.ProductRequest;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;
import com.example.Smart_E_Commerce_System.service.CategoryService;
import com.example.Smart_E_Commerce_System.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ProductGraphQlController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductGraphQlController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @QueryMapping
    public ProductResponse product(@Argument Integer id) {
        return productService.getById(id);
    }

    @QueryMapping
    public PageResponse<ProductResponse> products(@Argument ProductFilter filter,
                                                  @Argument int page, @Argument int size,
                                                  @Argument String sortBy, @Argument Sort.Direction direction) {
        ProductFilter safeFilter = (filter != null) ? filter : new ProductFilter(null, null, null, null);
        return productService.search(safeFilter, GraphQlPaging.of(page, size, sortBy, direction));
    }

    /**
     * Resolves Product.category for ALL products in the response with a single
     * query (WHERE id IN (...)), instead of one query per product (the N+1 problem).
     * Only runs if the client actually asks for the category field.
     */
    @BatchMapping(typeName = "Product", field = "category")
    public List<CategoryResponse> category(List<ProductResponse> products) {
        Set<Integer> ids = products.stream()
                .map(ProductResponse::categoryId)
                .collect(Collectors.toSet());
        Map<Integer, CategoryResponse> byId = categoryService.getByIds(ids);
        return products.stream()
                .map(p -> byId.get(p.categoryId()))
                .toList(); // same order as the input list
    }

    @MutationMapping
    public ProductResponse createProduct(@Argument @Valid ProductRequest input) {
        return productService.create(input);
    }

    @MutationMapping
    public ProductResponse updateProduct(@Argument Integer id, @Argument @Valid ProductRequest input) {
        return productService.update(id, input);
    }

    @MutationMapping
    public boolean deleteProduct(@Argument Integer id) {
        productService.delete(id);
        return true;
    }
}
