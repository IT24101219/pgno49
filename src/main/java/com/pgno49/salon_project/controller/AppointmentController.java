package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.Appointment;
import com.pgno49.salon_project.model.Service;
import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.AppointmentService;
import com.pgno49.salon_project.service.ServiceService;
import com.pgno49.salon_project.service.UserService;
import com.pgno49.salon_project.util.QuickSortUtil; // Import QuickSortUtil
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;
    private final ServiceService serviceService;


    @GetMapping("/add")
    public String showAddAppointmentForm(Model model, HttpSession session) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.CUSTOMER, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "appointments");

        List<Service> services = serviceService.getAllServices();
        if (services.isEmpty()) {
            model.addAttribute("errorMessage", "Cannot add appointment: Services are not available.");
            return "redirect:/appointments?error=no_services";
        }

        Appointment appointment = new Appointment();
        List<User> customers;
        if (hasRole(loggedInUser, User.Role.CUSTOMER)) {
            appointment.setCustomerId(loggedInUser.getId());
            customers = List.of(loggedInUser);
        } else {
            customers = userService.getAllActiveCustomers();
        }

        if (customers.isEmpty() && !hasRole(loggedInUser, User.Role.CUSTOMER)){
            model.addAttribute("errorMessage", "Cannot add appointment: No active customers found.");
            return "redirect:/appointments?error=no_customers";
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("customers", customers);
        model.addAttribute("services", services);
        model.addAttribute("pageTitle", "Book New Appointment");
        model.addAttribute("allStatuses", Appointment.AppointmentStatus.values());

        return "appointments/appointment-form";
    }
}