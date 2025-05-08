package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.Service;
import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.ServiceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/services")
public class ServiceController {

    private final ServiceService serviceService;

    @Autowired
    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
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

    @GetMapping({"", "/list"})
    public String listServices(@RequestParam(value = "query", required = false) String query, Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);

        if (loggedInUser == null) return "redirect:/login";

        List<Service> services;
        if (query != null && !query.trim().isEmpty()) {
            services = serviceService.searchServices(query);
            model.addAttribute("searchQuery", query);
        } else {
            services = serviceService.getAllServices();
        }
        model.addAttribute("services", services);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "services");

        model.addAttribute("canManageServices", hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN));
        return "services/service-list";
    }

    @GetMapping("/add")
    public String showAddServiceForm(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        model.addAttribute("service", new Service());
        model.addAttribute("pageTitle", "Add New Service");
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "services");
        return "services/service-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditServiceForm(@PathVariable("id") long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        Optional<Service> serviceOptional = serviceService.getServiceById(id);
        if (serviceOptional.isPresent()) {
            model.addAttribute("service", serviceOptional.get());
            model.addAttribute("pageTitle", "Edit Service (ID: " + id + ")");
            model.addAttribute("loggedInUser", loggedInUser);
            model.addAttribute("currentPage", "services");
            return "services/service-form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Service with ID " + id + " not found.");
            return "redirect:/services";
        }
    }

    @PostMapping("/save")
    public String saveService(@ModelAttribute("service") Service service, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }
        boolean isNew = service.getId() <= 0;
        try {
            Service savedService = serviceService.saveService(service);
            redirectAttributes.addFlashAttribute("successMessage", "Service '" + savedService.getName() + "' " + (isNew ? "added" : "updated") + " successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving service: " + e.getMessage());
            String redirectUrl = isNew ? "/services/add?error" : "/services/edit/" + service.getId() + "?error";
            return "redirect:" + redirectUrl;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/services";
        }
        return "redirect:/services";
    }

    @GetMapping("/delete/{id}")
    public String deleteService(@PathVariable("id") long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        try {
            Optional<Service> serviceOpt = serviceService.getServiceById(id);
            if (serviceOpt.isPresent()) {

                serviceService.deleteService(id);
                redirectAttributes.addFlashAttribute("successMessage", "Service '" + serviceOpt.get().getName() + "' deleted successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Service not found.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting service: " + e.getMessage());
        }
        return "redirect:/services";
    }
}