package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;



@Controller
@RequestMapping("/profile")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }



    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }


    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }




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


    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User userFormData,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        try {

            User updatedUser = userService.updateUserProfile(
                    loggedInUser.getId(),
                    userFormData.getFullName(),
                    userFormData.getPhoneNumber()
            );


            session.setAttribute("loggedInUser", updatedUser);

            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");


        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid data: " + e.getMessage());

            return "redirect:/profile/edit?error";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating profile: " + e.getMessage());

            return "redirect:/profile?error";
        }


        return "redirect:/profile";
    }
}

}

