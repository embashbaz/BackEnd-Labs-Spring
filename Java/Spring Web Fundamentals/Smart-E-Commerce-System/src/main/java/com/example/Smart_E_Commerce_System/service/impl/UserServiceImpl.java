package com.example.Smart_E_Commerce_System.service.impl;


import com.example.Smart_E_Commerce_System.dto.PageResponse;
import com.example.Smart_E_Commerce_System.dto.user.UserCreateRequest;
import com.example.Smart_E_Commerce_System.dto.user.UserResponse;
import com.example.Smart_E_Commerce_System.dto.user.UserUpdateRequest;
import com.example.Smart_E_Commerce_System.entity.Role;
import com.example.Smart_E_Commerce_System.entity.User;
import com.example.Smart_E_Commerce_System.exception.BusinessRuleException;
import com.example.Smart_E_Commerce_System.exception.DuplicateResourceException;
import com.example.Smart_E_Commerce_System.exception.ResourceNotFoundException;
import com.example.Smart_E_Commerce_System.repository.OrderRepository;
import com.example.Smart_E_Commerce_System.repository.UserRepository;
import com.example.Smart_E_Commerce_System.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           OrderRepository orderRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalize(request.email());
        // @UniqueEmail already checked this; the repeat guards against races between requests
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role() != null ? request.role() : Role.CUSTOMER);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public UserResponse getById(Integer id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Override
    public PageResponse<UserResponse> getAll(Role role, Pageable pageable) {
        Page<User> page = (role == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);
        return PageResponse.from(page.map(UserResponse::from));
    }

    @Override
    @Transactional
    public UserResponse update(Integer id, UserUpdateRequest request) {
        User user = findOrThrow(id);
        String email = normalize(request.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("Email '" + email + "' is already registered");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(request.role());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        User user = findOrThrow(id);
        if (orderRepository.existsByUserId(id)) {
            throw new BusinessRuleException("Cannot delete user " + id + ": they have orders on record");
        }
        userRepository.delete(user); // their reviews are removed by ON DELETE CASCADE
    }

    private User findOrThrow(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
