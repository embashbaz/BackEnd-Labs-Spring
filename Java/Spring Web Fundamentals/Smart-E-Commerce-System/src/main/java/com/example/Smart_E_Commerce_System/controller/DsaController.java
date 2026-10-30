package com.example.Smart_E_Commerce_System.controller;

import com.example.Smart_E_Commerce_System.dto.ApiResponse;
import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.product.ProductResponse;
import com.example.Smart_E_Commerce_System.service.DsaProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dsa/products")
@Tag(name = "Algorithms (DSA)", description = "Custom merge sort, binary search and pagination")
public class DsaController {

    private final DsaProductService dsaProductService;

    public DsaController(DsaProductService dsaProductService) {
        this.dsaProductService = dsaProductService;
    }

    /** Example: GET /api/dsa/products?sortBy=price&descending=true&page=0&size=5 */
    @GetMapping
    @Operation(summary = "Products sorted with merge sort and paginated in memory")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> sorted(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "false") boolean descending,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Products sorted",
                dsaProductService.sortProducts(sortBy, descending, page, size)));
    }

    /** Example: GET /api/dsa/products/5 */
    @GetMapping("/{id}")
    @Operation(summary = "Find a product by id using binary search")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Product found", dsaProductService.findById(id)));
    }
}
