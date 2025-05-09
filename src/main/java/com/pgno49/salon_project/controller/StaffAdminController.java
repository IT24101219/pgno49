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
@RequestMapping("/admin")
public class StaffAdminController {

    private final StaffApprovalService staffApprovalService;
    private final UserService userService;

    @Autowired
    public StaffAdminController(StaffApprovalService staffApprovalService, UserService userService) {
        this.staffApprovalService = staffApprovalService;
        this.userService = userService;
    }

    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    private boolean hasRole(User user, User.Role... allowedRoles) {
        if (user == null) return false;
        for (User.Role allowedRole : allowedRoles) {
            if (user.getRole() == allowedRole) return true;
            if (allowedRole == User.Role.STAFF_APPROVED && user.getRole() == User.Role.MAIN_ADMIN) return true;
        }
        return false;
    }

    @GetMapping("/staff-approval")
    public String showStaffApprovalPage(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        List<User> pendingRequests = staffApprovalService.getPendingStaffRequests();

        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "staff-approval");
        return "admin/staff-approval";
    }

    @PostMapping("/staff-approval/approve/{userId}")
    public String approveStaff(@PathVariable("userId") long userId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        try {
            staffApprovalService.approveStaffRequest(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Staff request for user ID " + userId + " approved successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving request: " + e.getMessage());
        }
        return "redirect:/admin/staff-approval";
    }

    @PostMapping("/staff-approval/reject/{userId}")
    public String rejectStaff(@PathVariable("userId") long userId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        try {
            staffApprovalService.rejectStaffRequest(userId);
            redirectAttributes.addFlashAttribute("successMessage", "Staff request for user ID " + userId + " rejected successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error rejecting request: " + e.getMessage());
        }
        return "redirect:/admin/staff-approval";
    }

    @GetMapping("/manage-users")
    public String showUserManagementPage(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);

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
