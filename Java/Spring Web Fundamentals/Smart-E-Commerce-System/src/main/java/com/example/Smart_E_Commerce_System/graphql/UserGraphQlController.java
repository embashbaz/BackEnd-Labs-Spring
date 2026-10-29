package com.example.Smart_E_Commerce_System.graphql;

import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.user.UserCreateRequest;
import com.example.Smart_E_Commerce_System.dto.user.UserResponse;
import com.example.Smart_E_Commerce_System.dto.user.UserUpdateRequest;
import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class UserGraphQlController {

    private final UserService userService;

    public UserGraphQlController(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    public UserResponse user(@Argument Integer id) {
        return userService.getById(id);
    }

    @QueryMapping
    public PageResponse<UserResponse> users(@Argument Role role,
                                            @Argument int page, @Argument int size,
                                            @Argument String sortBy, @Argument Sort.Direction direction) {
        return userService.getAll(role, GraphQlPaging.of(page, size, sortBy, direction));
    }

    // @Valid runs the same Bean Validation as REST, including @UniqueEmail and @StrongPassword
    @MutationMapping
    public UserResponse createUser(@Argument @Valid UserCreateRequest input) {
        return userService.create(input);
    }

    @MutationMapping
    public UserResponse updateUser(@Argument Integer id, @Argument @Valid UserUpdateRequest input) {
        return userService.update(id, input);
    }

    @MutationMapping
    public boolean deleteUser(@Argument Integer id) {
        userService.delete(id);
        return true;
    }
}
