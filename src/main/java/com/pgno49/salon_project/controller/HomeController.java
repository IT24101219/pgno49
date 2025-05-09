package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {


    private User getLoggedInUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "home");
        return "index";
    }
}