package com.example.Smart_E_Commerce_System.validation;


import com.example.Smart_E_Commerce_System.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Spring creates this validator, so the repository is injected through the constructor.
 */
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    public UniqueEmailValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true;
        }
        return !userRepository.existsByEmailIgnoreCase(email.trim());
    }
}
