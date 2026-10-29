package com.example.Smart_E_Commerce_System.graphql;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.category.CategoryRequest;
import com.example.Smart_E_Commerce_System.dto.category.CategoryResponse;
import com.example.Smart_E_Commerce_System.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL entry point for categories. Delegates to the same CategoryService
 * as the REST controller, so REST and GraphQL share all business logic.
 */
@Controller
public class CategoryGraphQlController {

    private final CategoryService categoryService;

    public CategoryGraphQlController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @QueryMapping
    public CategoryResponse category(@Argument Integer id) {
        return categoryService.getById(id);
    }

    @QueryMapping
    public PageResponse<CategoryResponse> categories(@Argument int page, @Argument int size,
                                                     @Argument String sortBy, @Argument Sort.Direction direction) {
        return categoryService.getAll(GraphQlPaging.of(page, size, sortBy, direction));
    }

    @MutationMapping
    public CategoryResponse createCategory(@Argument @Valid CategoryRequest input) {
        return categoryService.create(input);
    }

    @MutationMapping
    public CategoryResponse updateCategory(@Argument Integer id, @Argument @Valid CategoryRequest input) {
        return categoryService.update(id, input);
    }

    @MutationMapping
    public boolean deleteCategory(@Argument Integer id) {
        categoryService.delete(id);
        return true;
    }
}
