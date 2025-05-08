package com.pgno49.salon_project.controller;

import com.pgno49.salon_project.model.Appointment;
import com.pgno49.salon_project.model.Service;
import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.service.AppointmentService;
import com.pgno49.salon_project.service.ServiceService;
import com.pgno49.salon_project.service.UserService;
import com.pgno49.salon_project.util.QuickSortUtil; 
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

    @Autowired
    public AppointmentController(AppointmentService appointmentService,
                                 UserService userService,
                                 ServiceService serviceService) {
        this.appointmentService = appointmentService;
        this.userService = userService;
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
    public String listAppointments(Model model, HttpSession session,
                                   @RequestParam(value = "filter", required = false) String filter) {
        User loggedInUser = getLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "appointments");
        if (filter != null && !filter.isEmpty()) {
            model.addAttribute("activeFilter", filter);
        }

        List<Appointment> allAppointments;
        try {
            if (hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
                allAppointments = appointmentService.getAllAppointments();
            } else if (hasRole(loggedInUser, User.Role.CUSTOMER)) {
                allAppointments = appointmentService.getAppointmentsByCustomerId(loggedInUser.getId());
            } else {
                return "redirect:/access-denied";
            }

            List<Appointment> filteredAppointments = allAppointments;
            if (filter != null && !filter.isEmpty()) {
                filteredAppointments = appointmentService.filterAppointments(allAppointments, filter);
            }

            if (filteredAppointments != null && !filteredAppointments.isEmpty()) {
                QuickSortUtil.sortAppointments(filteredAppointments);
            }

            Map<Long, User> userMap = userService.getAllUsersMap();
            Map<Long, Service> serviceMap = serviceService.getAllServicesMap();

            model.addAttribute("appointments", filteredAppointments);
            model.addAttribute("userMap", userMap);
            model.addAttribute("serviceMap", serviceMap);

        } catch (Exception e) {
            System.err.println("Error fetching data for appointment list: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error loading appointment data.");
            model.addAttribute("appointments", List.of());
            model.addAttribute("userMap", Map.of());
            model.addAttribute("serviceMap", Map.of());
        }
        return "appointments/appointment-list";
    }
  
  
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


    @GetMapping("/edit/{id}")
    public String showEditAppointmentForm(@PathVariable("id") long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("currentPage", "appointments");

        Optional<Appointment> appointmentOptional = appointmentService.getAppointmentById(id);
        if (appointmentOptional.isPresent()) {
            List<User> customers = userService.getAllActiveCustomers();
            List<Service> services = serviceService.getAllServices();

            model.addAttribute("appointment", appointmentOptional.get());
            model.addAttribute("customers", customers);
            model.addAttribute("services", services);
            model.addAttribute("pageTitle", "Edit Appointment (ID: " + id + ")");
            model.addAttribute("allStatuses", Appointment.AppointmentStatus.values());
            return "appointments/appointment-form";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment with ID " + id + " not found.");
            return "redirect:/appointments";
        }
    }

    @PostMapping("/save")
    public String saveAppointment(@ModelAttribute("appointment") Appointment appointment,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        boolean isNew = appointment.getId() <= 0;

        if (isNew && !hasRole(loggedInUser, User.Role.CUSTOMER, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return "redirect:/access-denied";
        }
        if (!isNew && !hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return "redirect:/access-denied";
        }
        if (hasRole(loggedInUser, User.Role.CUSTOMER) && !Objects.equals(loggedInUser.getId(), appointment.getCustomerId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Customers can only book or modify appointments for themselves.");
            return isNew ? "redirect:/appointments/add?error" : "redirect:/appointments";
        }

        try {
            appointmentService.saveAppointment(appointment);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment " + (isNew ? "booked" : "updated") + " successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid data: " + e.getMessage());
            String redirectUrl = isNew ? "/appointments/add?error" : "/appointments/edit/" + appointment.getId() + "?error";
            return "redirect:" + redirectUrl;
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving appointment: " + e.getMessage());
            return "redirect:/appointments?error";
        }
        return "redirect:/appointments";
    }


    @GetMapping("/delete/{id}")
    public String deleteAppointment(@PathVariable("id") long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        Optional<Appointment> appointmentOptional = appointmentService.getAppointmentById(id);
        if(appointmentOptional.isEmpty()){
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment with ID " + id + " not found for deletion.");
            return "redirect:/appointments";
        }
        Appointment appointment = appointmentOptional.get();

        boolean allowed = false;
        if (hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            allowed = true;
        } else if (hasRole(loggedInUser, User.Role.CUSTOMER) && Objects.equals(loggedInUser.getId(), appointment.getCustomerId())) {
            if(appointment.getStatus() == Appointment.AppointmentStatus.SCHEDULED) {
                allowed = true;
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "You can only cancel appointments that are currently scheduled.");
                return "redirect:/appointments";
            }
        }

        if (!allowed) {
            return "redirect:/access-denied";
        }

        try {
            appointmentService.deleteAppointment(id);
            Map<Long, User> userMap = userService.getAllUsersMap();
            String customerName = userMap.getOrDefault(appointment.getCustomerId(), new User()).getFullName();
            if(customerName == null || customerName.isBlank()) customerName = "Unknown";
            redirectAttributes.addFlashAttribute("successMessage", "Appointment (ID: " + id + " for "+customerName+") deleted/cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting appointment with ID " + id + ": " + e.getMessage());
        }
        return "redirect:/appointments";
    }

    @PostMapping("/updateStatus/{id}")
    public String updateStatus(@PathVariable("id") long id,
                               @RequestParam("status") Appointment.AppointmentStatus newStatus,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User loggedInUser = getLoggedInUser(session);
        if (!hasRole(loggedInUser, User.Role.STAFF_APPROVED, User.Role.MAIN_ADMIN)) {
            return (loggedInUser == null) ? "redirect:/login" : "redirect:/access-denied";
        }

        try {
            appointmentService.updateAppointmentStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment (ID: " + id + ") status updated to " + newStatus + ".");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/appointments";
    }
}
