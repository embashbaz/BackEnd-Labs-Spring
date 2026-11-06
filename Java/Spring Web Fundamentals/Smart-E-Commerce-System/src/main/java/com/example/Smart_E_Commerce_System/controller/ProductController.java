package com.example.Smart_E_Commerce_System.controller;


import com.example.Smart_E_Commerce_System.dto.ApiResponse;
import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductFilter;
import com.example.Smart_E_Commerce_System.dto.product.ProductRequest;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;
import com.example.Smart_E_Commerce_System.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Manage, search, filter and sort products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a product", description = "Also creates its inventory row with initialStock (default 0)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", productService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @Parameter(description = "Product id", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Product retrieved", productService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Search products",
            description = "All filters are optional and combine with AND. Supports pagination and sorting, "
                    + "e.g. ?categoryId=1&minPrice=10&maxPrice=500&name=mouse&page=0&size=10&sort=price,desc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matching products"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter or sort property"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "minPrice greater than maxPrice")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> search(
            @Parameter(description = "Only products in this category", example = "1")
            @RequestParam(required = false) Integer categoryId,
            @Parameter(description = "Minimum price (inclusive)", example = "10.00")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)", example = "500.00")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Case-insensitive partial match on name", example = "mouse")
            @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {

        ProductFilter filter = new ProductFilter(categoryId, minPrice, maxPrice, name);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved", productService.search(filter, pageable)));
    }

    @GetMapping("/by-category")
    @Operation(summary = "Products in a category, by category name",
            description = "Derived query ProductRepository.findByCategoryNameIgnoreCase: Spring builds the SQL "
                    + "from the method name, joining products to categories. Case-insensitive exact name. "
                    + "Paginated and sortable, e.g. ?name=Electronics&page=0&size=10&sort=price,asc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products in the category"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing name or invalid sort property"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No category with that name")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getByCategoryName(
            @Parameter(description = "Category name (case-insensitive)", example = "Electronics")
            @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved",
                productService.getByCategoryName(name, pageable)));
    }

    @GetMapping("/by-price")
    @Operation(summary = "Products in a price range",
            description = "Derived query ProductRepository.findByPriceBetween (both bounds inclusive). "
                    + "Paginated and sortable, e.g. ?min=10&max=100&sort=price,asc")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products in the range"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing bound or invalid sort property"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "min greater than max")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getByPriceRange(
            @Parameter(description = "Minimum price (inclusive)", example = "10.00") @RequestParam BigDecimal min,
            @Parameter(description = "Maximum price (inclusive)", example = "100.00") @RequestParam BigDecimal max,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Products retrieved",
                productService.getByPriceRange(min, max, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product", description = "initialStock is ignored on update")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or category not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @Parameter(description = "Product id", example = "1") @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product", description = "Fails if the product appears in any order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product is referenced by orders")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Product id", example = "1") @PathVariable Integer id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }
}