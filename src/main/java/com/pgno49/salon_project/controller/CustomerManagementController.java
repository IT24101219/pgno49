package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/customers")
public class CustomerManagementController {

    private final UserService userService;

    @Autowired
    public CustomerManagementController(UserService userService) {
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

    @GetMapping({"", "/list"}) // Maps to /customers and /customers/list
    public String listCustomers(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        // Authorization Check
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }


        List<User> customerList = userService.getAllUsers().stream()
                .filter(user -> user.getRole() == User.Role.CUSTOMER)
                .toList();

        model.addAttribute("customerList", customerList);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "customers"); // For navbar active state
        // Flag to check if the current user is MAIN_ADMIN (for delete button visibility)
        model.addAttribute("isMainAdmin", loggedInUser.getRole() == User.Role.MAIN_ADMIN);
        return "admin/customer-list"; // Path to the new template
    }



    @PostMapping("/delete/{userId}")
    public String deleteCustomer(@PathVariable("userId") long userId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);

        if (!hasRole(loggedInUser, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        User customerToDelete = userService.getUserById(userId).orElse(null);
        if(customerToDelete == null || customerToDelete.getRole() != User.Role.CUSTOMER) {
            redirectAttributes.addFlashAttribute("errorMessage", "Customer not found or specified user is not a customer.");
            return "redirect:/customers";
        }


        try {
            userService.deleteUserById(userId, loggedInUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Customer (ID: " + userId + ") deleted successfully.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting customer: " + e.getMessage());
        }
        return "redirect:/customers";
    }

}