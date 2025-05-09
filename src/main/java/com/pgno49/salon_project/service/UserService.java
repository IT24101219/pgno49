package com.pgno49.salon_project.service;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllActiveCustomers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.Role.CUSTOMER &&
                        user.getStatus() == User.AccountStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<User> getAllActiveStaffAndAdmins() {
        return userRepository.findAll().stream()
                .filter(user -> (user.getRole() == User.Role.STAFF_APPROVED || user.getRole() == User.Role.MAIN_ADMIN) &&
                        user.getStatus() == User.AccountStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public void deleteUserById(long userIdToDelete, long loggedInUserId) {
        if (userIdToDelete == loggedInUserId) {
            throw new RuntimeException("Users cannot delete their own account.");
        }

        User userToDelete = userRepository.findById(userIdToDelete)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userIdToDelete));


        if (userToDelete.getRole() == User.Role.MAIN_ADMIN) {
            throw new RuntimeException("The MAIN_ADMIN account cannot be deleted.");
        }




        userRepository.deleteById(userIdToDelete);
        System.out.println("Deleted user with ID: " + userIdToDelete);
    }


    public User updateUserProfile(long userId, String newFullName, String newPhoneNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));


        if (!StringUtils.hasText(newFullName)) {
            throw new IllegalArgumentException("Full name cannot be empty.");
        }
        if (!StringUtils.hasText(newPhoneNumber)) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }

        String trimmedPhoneNumber = newPhoneNumber.trim();
        if (!Objects.equals(user.getPhoneNumber(), trimmedPhoneNumber)) {
            Optional<User> existingUserWithPhone = userRepository.findByPhoneNumber(trimmedPhoneNumber);
            if (existingUserWithPhone.isPresent() && existingUserWithPhone.get().getId() != userId) {
                throw new IllegalArgumentException("Phone number '" + newPhoneNumber + "' is already registered to another user.");
            }
            user.setPhoneNumber(trimmedPhoneNumber);
        }

        user.setFullName(newFullName.trim());

        return userRepository.save(user);
    }

    public void suspendUser(long userIdToSuspend, long adminUserId) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Performing admin user not found."));


        if (adminUser.getRole() != User.Role.MAIN_ADMIN) {
            throw new RuntimeException("User does not have permission to suspend accounts.");
        }


        if (userIdToSuspend == adminUserId) {
            throw new RuntimeException("Cannot suspend your own account.");
        }

        User userToSuspend = userRepository.findById(userIdToSuspend)
                .orElseThrow(() -> new RuntimeException("User to suspend not found with ID: " + userIdToSuspend));


        if (userToSuspend.getRole() == User.Role.MAIN_ADMIN) {
            throw new RuntimeException("Cannot suspend the MAIN_ADMIN account.");
        }


        if (userToSuspend.getStatus() == User.AccountStatus.SUSPENDED) {
            throw new RuntimeException("User account is already suspended.");
        }


        userToSuspend.setStatus(User.AccountStatus.SUSPENDED);
        userRepository.save(userToSuspend);
        System.out.println("Suspended user with ID: " + userIdToSuspend);
    }

    public void unsuspendUser(long userIdToUnsuspend, long adminUserId) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Performing admin user not found."));


        if (adminUser.getRole() != User.Role.MAIN_ADMIN) {
            throw new RuntimeException("User does not have permission to unsuspend accounts.");
        }

        User userToUnsuspend = userRepository.findById(userIdToUnsuspend)
                .orElseThrow(() -> new RuntimeException("User to unsuspend not found with ID: " + userIdToUnsuspend));


        if (userToUnsuspend.getStatus() != User.AccountStatus.SUSPENDED) {
            throw new RuntimeException("User account is not currently suspended.");
        }


        userToUnsuspend.setStatus(User.AccountStatus.ACTIVE);
        userRepository.save(userToUnsuspend);
        System.out.println("Unsuspended user with ID: " + userIdToUnsuspend);
    }
}