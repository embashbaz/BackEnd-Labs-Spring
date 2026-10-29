package com.example.Smart_E_Commerce_System.service;


import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.user.UserCreateRequest;
import com.example.Smart_E_Commerce_System.dto.user.UserResponse;
import com.example.Smart_E_Commerce_System.dto.user.UserUpdateRequest;
import com.example.Smart_E_Commerce_System.entity.Role;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    UserResponse getById(Integer id);

    PageResponse<UserResponse> getAll(Role role, Pageable pageable);

    UserResponse update(Integer id, UserUpdateRequest request);

    void delete(Integer id);
}
