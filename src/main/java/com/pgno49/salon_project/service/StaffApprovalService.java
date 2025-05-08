package com.pgno49.salon_project.service;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StaffApprovalService {

    private final UserRepository userRepository;

    @Autowired
    public StaffApprovalService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public List<User> getPendingStaffRequests() {
        return userRepository.findUsersByRoleAndStatus(User.Role.STAFF_PENDING, User.AccountStatus.PENDING_APPROVAL);
    }


    public void approveStaffRequest(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found for approval."));


        if (user.getRole() != User.Role.STAFF_PENDING || user.getStatus() != User.AccountStatus.PENDING_APPROVAL) {
            throw new RuntimeException("User with ID " + userId + " is not pending staff approval.");
        }


        user.setRole(User.Role.STAFF_APPROVED);
        user.setStatus(User.AccountStatus.ACTIVE);

        userRepository.save(user);
        System.out.println("Approved staff request for user ID: " + userId);
    }


    public void rejectStaffRequest(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User with ID " + userId + " not found for rejection."));


        if (user.getRole() != User.Role.STAFF_PENDING || user.getStatus() != User.AccountStatus.PENDING_APPROVAL) {
            throw new RuntimeException("User with ID " + userId + " is not pending staff approval.");
        }


        user.setStatus(User.AccountStatus.REJECTED);



        userRepository.save(user);
        System.out.println("Rejected staff request for user ID: " + userId);
    }
}