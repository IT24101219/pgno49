package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/staff")
public class StaffController {

    private final UserService userService;

    @Autowired
    public StaffController(UserService userService) {
        this.userService = userService;
    }

    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    private boolean hasRole(User user, User.Role... allowedRoles) {
        if (user == null) return false;
        for (User.Role allowedRole : allowedRoles) {
            if (user.getRole() == allowedRole) return true;
        }
        return false;
    }

    @GetMapping({"", "/list"})
    public String listStaff(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        List<User> staffList = userService.getAllActiveStaffAndAdmins();

        model.addAttribute("staffList", staffList);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "staff");
        return "staff/staff-list";
    }

    @GetMapping("/delete/{id}")
    public String deleteStaff(@PathVariable("id") long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        if (loggedInUser.getId() == id) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own account.");
            return "redirect:/staff";
        }

        Optional<User> staffToDeleteOpt = userService.getUserById(id);
        if (staffToDeleteOpt.isEmpty() ||
                (staffToDeleteOpt.get().getRole() != User.Role.STAFF_APPROVED &&
                        staffToDeleteOpt.get().getRole() != User.Role.STAFF_PENDING &&
                        staffToDeleteOpt.get().getRole() != User.Role.MAIN_ADMIN) ) {
            redirectAttributes.addFlashAttribute("errorMessage", "Staff member not found or cannot be deleted.");
            return "redirect:/staff";
        }

        try {
            userService.deleteUserById(id, loggedInUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Staff member (ID: " + id + ") deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting staff member: " + e.getMessage());
        }
        return "redirect:/staff";
    }
}
