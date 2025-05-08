package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Ensure necessary imports are present
// import java.util.Arrays; // Not needed in this version

@Controller
@RequestMapping("/profile") // Base path for user profile actions
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- Authorization Helpers ---
    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
    // --- End Helpers ---


    @GetMapping("")
    public String showMyProfile(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", loggedInUser);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "profile");
        return "profile";
    }

    @GetMapping("/edit")
    public String showEditProfileForm(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        model.addAttribute("user", loggedInUser);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "profile-edit");
        return "profile-form";
    }
}