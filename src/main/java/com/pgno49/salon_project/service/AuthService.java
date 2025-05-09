package com.pgno49.salon_project.service;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.staff-signup.passcode:ADMIN_PASS}")
    private String requiredAdminPasscode;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerCustomer(String username, String email, String phoneNumber, String password, String fullName) {
        validateRegistrationInput(username, email, phoneNumber, password, fullName);
        checkUniqueness(username, email, phoneNumber);

        String hashedPassword = passwordEncoder.encode(password);

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPhoneNumber(phoneNumber.trim());
        newUser.setPasswordHash(hashedPassword);
        newUser.setFullName(fullName.trim());
        newUser.setRole(User.Role.CUSTOMER);
        newUser.setStatus(User.AccountStatus.ACTIVE);

        return userRepository.save(newUser);
    }


    public User registerStaff(String username, String email, String phoneNumber, String password, String fullName, String adminPasscode) {
        if (!requiredAdminPasscode.equals(adminPasscode)) {
            throw new IllegalArgumentException("Invalid Admin Passcode provided for staff registration.");
        }
        validateRegistrationInput(username, email, phoneNumber, password, fullName);
        checkUniqueness(username, email, phoneNumber);

        String hashedPassword = passwordEncoder.encode(password);

        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setEmail(email.trim().toLowerCase());
        newUser.setPhoneNumber(phoneNumber.trim());
        newUser.setPasswordHash(hashedPassword);
        newUser.setFullName(fullName.trim());
        newUser.setRole(User.Role.STAFF_PENDING);
        newUser.setStatus(User.AccountStatus.PENDING_APPROVAL);

        return userRepository.save(newUser);
    }


    public User login(String username, String rawPassword) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }

        Optional<User> userOptional = userRepository.findByUsername(username.trim());
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid username or password.");
        }
        User user = userOptional.get();

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password.");
        }

        switch (user.getStatus()) {
            case ACTIVE: return user;
            case PENDING_APPROVAL: throw new RuntimeException("Account pending approval. Please wait.");
            case REJECTED: throw new RuntimeException("Account registration was rejected. Please contact administration.");
            case SUSPENDED: throw new RuntimeException("Account is suspended. Please contact administration.");
            default: throw new RuntimeException("Account is inactive or in an unknown state.");
        }
    }

    private void validateRegistrationInput(String username, String email, String phoneNumber, String password, String fullName) {
        if (!StringUtils.hasText(username)) throw new IllegalArgumentException("Username cannot be empty.");
        if (!StringUtils.hasText(email) || !email.contains("@")) throw new IllegalArgumentException("Valid email cannot be empty.");
        if (!StringUtils.hasText(phoneNumber)) throw new IllegalArgumentException("Phone number cannot be empty.");
        if (!StringUtils.hasText(password)) throw new IllegalArgumentException("Password cannot be empty.");
        if (!StringUtils.hasText(fullName)) throw new IllegalArgumentException("Full name cannot be empty.");
    }

    private void checkUniqueness(String username, String email, String phoneNumber) {
        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim().toLowerCase();
        String trimmedPhone = phoneNumber.trim();

        Optional<User> existingByUsername = userRepository.findByUsername(trimmedUsername);
        if (existingByUsername.isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }

        Optional<User> existingByEmail = userRepository.findByEmail(trimmedEmail);
        if (existingByEmail.isPresent()) {
            if (existingByEmail.get().getStatus() == User.AccountStatus.SUSPENDED) {
                throw new IllegalArgumentException("Cannot register: An account associated with email '" + email + "' is currently suspended.");
            } else {
                throw new IllegalArgumentException("Email '" + email + "' is already registered.");
            }
        }

        Optional<User> existingByPhone = userRepository.findByPhoneNumber(trimmedPhone);
        if (existingByPhone.isPresent()) {
            if (existingByPhone.get().getStatus() == User.AccountStatus.SUSPENDED) {
                throw new IllegalArgumentException("Cannot register: An account associated with phone number '" + phoneNumber + "' is currently suspended.");
            } else {
                throw new IllegalArgumentException("Phone number '" + phoneNumber + "' is already registered.");
            }
        }
    }

}