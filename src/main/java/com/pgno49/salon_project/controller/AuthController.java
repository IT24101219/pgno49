package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String showLoginPage(Model model,
                                @RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                @RequestParam(value = "staff_pending", required = false) String staffPending) {
        if (error != null && !model.containsAttribute("loginError")) {
            model.addAttribute("loginError", "Invalid username or password.");
        }
        if (logout != null && !model.containsAttribute("logoutMessage")) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }
        if (registered != null && !model.containsAttribute("registrationSuccess")) {
            model.addAttribute("registrationSuccess", "Registration successful! Please log in.");
        }
        if (staffPending != null && !model.containsAttribute("registrationPending")) {
            model.addAttribute("registrationPending", "Staff registration submitted. Please log in after approval.");
        }
        model.addAttribute("currentPage", "login");
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            User loggedInUser = authService.login(username, password);
            session.setAttribute("loggedInUser", loggedInUser);
            System.out.println("Login successful for user: " + loggedInUser.getUsername() + " Role: " + loggedInUser.getRole());
            return "redirect:/";

        } catch (RuntimeException e) {
            System.err.println("Login failed for user: " + username + " - " + e.getMessage());
            redirectAttributes.addFlashAttribute("loginError", e.getMessage());

            if (e.getMessage() != null) {
                if (e.getMessage().contains("pending approval")) { return "redirect:/pending-approval"; }
                if (e.getMessage().contains("rejected")) { return "redirect:/approval-rejected"; }
            }
            return "redirect:/login?error";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object userAttribute = session.getAttribute("loggedInUser");
            if (userAttribute instanceof User loggedInUser) {
                System.out.println("Logging out user: " + loggedInUser.getUsername());
            } else {
                System.out.println("Logging out user: No user found in session or attribute type mismatch.");
            }
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("logoutMessage", "You have been logged out successfully.");
        return "redirect:/login?logout";
    }

    @GetMapping("/signup")
    public String showSignupPage(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null && !model.containsAttribute("signupError")) {
            model.addAttribute("signupError", "Registration failed. Please check details and try again.");
        }
        model.addAttribute("currentPage", "signup");
        return "signup";
    }

    @PostMapping("/signup")
    public String processSignup(@RequestParam String username,
                                @RequestParam String email,
                                @RequestParam String phoneNumber,
                                @RequestParam String password,
                                @RequestParam String fullName,
                                RedirectAttributes redirectAttributes) {
        try {
            authService.registerCustomer(username, email, phoneNumber, password, fullName);
            redirectAttributes.addFlashAttribute("registrationSuccess", "Registration successful! Please log in.");
            return "redirect:/login?registered";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("signupError", e.getMessage());
            return "redirect:/signup?error";
        }
    }

    @GetMapping("/signup-staff")
    public String showStaffSignupPage(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null && !model.containsAttribute("signupError")) {
            model.addAttribute("signupError", "Staff registration failed. Please check details and try again.");
        }
        model.addAttribute("currentPage", "signup-staff");
        return "signup-staff";
    }

    @PostMapping("/signup-staff")
    public String processStaffSignup(@RequestParam String username,
                                     @RequestParam String email,
                                     @RequestParam String phoneNumber,
                                     @RequestParam String password,
                                     @RequestParam String fullName,
                                     @RequestParam String adminPasscode,
                                     RedirectAttributes redirectAttributes) {
        try {
            authService.registerStaff(username, email, phoneNumber, password, fullName, adminPasscode);
            redirectAttributes.addFlashAttribute("registrationPending", "Staff registration submitted and awaiting approval.");
            return "redirect:/login?staff_pending";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("signupError", e.getMessage());
            return "redirect:/signup-staff?error";
        }
    }

    @GetMapping("/pending-approval")
    public String showPendingPage(Model model) {
        model.addAttribute("currentPage", "pending-approval");
        return "pending-approval";
    }

    @GetMapping("/approval-rejected")
    public String showRejectedPage(Model model) {
        model.addAttribute("currentPage", "approval-rejected");
        return "approval-rejected";
    }

    @GetMapping("/access-denied")
    public String showAccessDeniedPage(Model model) {
        model.addAttribute("currentPage", "access-denied");
        return "access-denied";
    }
}