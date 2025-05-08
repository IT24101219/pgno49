package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.StaffApprovalService;
import com.pgno49.salon_project.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin") // Base path for admin-related functions
public class StaffAdminController {

    private final StaffApprovalService staffApprovalService;
    private final UserService userService;

    @Autowired
    public StaffAdminController(StaffApprovalService staffApprovalService, UserService userService) {
        this.staffApprovalService = staffApprovalService;
        this.userService = userService;
    }

    @GetMapping("/manage-users")
    public String showUserManagementPage(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        // Authorization Check: Only MAIN_ADMIN
        if (!hasRole(loggedInUser, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        List<User> allUsers = userService.getAllUsers();

        model.addAttribute("allUsers", allUsers);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "manage-users");
        return "admin/user-management";
    }

    @PostMapping("/users/suspend/{userId}")
    public String suspendUser(@PathVariable("userId") long userId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        try {
            userService.suspendUser(userId, loggedInUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User ID " + userId + " suspended successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error suspending user: " + e.getMessage());
        }
        return "redirect:/admin/manage-users";
    }

    @PostMapping("/users/unsuspend/{userId}")
    public String unsuspendUser(@PathVariable("userId") long userId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        try {
            userService.unsuspendUser(userId, loggedInUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "User ID " + userId + " reactivated successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error reactivating user: " + e.getMessage());
        }
        return "redirect:/admin/manage-users";
    }
}
