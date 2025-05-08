package com.pgno49.salon_project.service;

import com.pgno49.salon_project.model.Appointment;
import com.pgno49.salon_project.model.Service;
import com.pgno49.salon_project.model.User;
import com.pgno49.salon_project.repository.AppointmentRepository;
import com.pgno49.salon_project.repository.UserRepository;
import com.pgno49.salon_project.repository.ServiceRepository;
import com.pgno49.salon_project.util.CustomAppointmentQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final CustomAppointmentQueue appointmentRequestQueue;

    private static final int APPOINTMENT_QUEUE_CAPACITY = 10;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              ServiceRepository serviceRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.appointmentRequestQueue = new CustomAppointmentQueue(APPOINTMENT_QUEUE_CAPACITY);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(long id) {
        return appointmentRepository.findById(id);
    }

    public List<Appointment> getAppointmentsByCustomerId(Long customerId) {
        return appointmentRepository.findByCustomerId(customerId);
    }

    public List<Appointment> getAppointmentsByServiceId(Long serviceId) {
        return appointmentRepository.findByServiceId(serviceId);
    }

    public Appointment saveAppointment(Appointment appointment) {
        if (appointment.getCustomerId() == null || appointment.getCustomerId() <= 0) {
            throw new IllegalArgumentException("Valid Customer ID must be provided.");
        }
        if (appointment.getServiceId() == null || appointment.getServiceId() <= 0) {
            throw new IllegalArgumentException("Valid Service ID must be provided.");
        }
        if (appointment.getAppointmentDateTime() == null) {
            throw new IllegalArgumentException("Appointment date and time must be provided.");
        }
        boolean isNewAppointment = (appointment.getId() <= 0);
        if (isNewAppointment && appointment.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("New appointment date and time must be in the future.");
        }


        userRepository.findById(appointment.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer (User) not found with ID: " + appointment.getCustomerId()));

        Service serviceForNewAppointment = serviceRepository.findById(appointment.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found with ID: " + appointment.getServiceId()));

        Appointment processedAppointmentRequest = appointment;

        if (isNewAppointment) {
            System.out.println("Attempting to enqueue new appointment request for customer ID: " + appointment.getCustomerId());
            appointmentRequestQueue.displayQueue();
            boolean enqueued = appointmentRequestQueue.insert(appointment);
            appointmentRequestQueue.displayQueue();

            if (!enqueued) {
                throw new RuntimeException("Booking request queue is currently full. Please try again shortly.");
            }
            processedAppointmentRequest = appointmentRequestQueue.remove();
            appointmentRequestQueue.displayQueue();

            if (processedAppointmentRequest == null) {
                throw new RuntimeException("Failed to retrieve booking request from queue. Please try again.");
            }
            System.out.println("Dequeued appointment request for customer ID: " + processedAppointmentRequest.getCustomerId() + " for processing.");
        }


        final long currentProcessedAppointmentId = processedAppointmentRequest.getId();
        final LocalDateTime proposedStartTime = processedAppointmentRequest.getAppointmentDateTime();
        final LocalDateTime proposedEndTime = proposedStartTime.plusMinutes(serviceForNewAppointment.getDurationInMinutes());

        List<Appointment> existingAppointments = appointmentRepository.findAll().stream()
                .filter(existing -> existing.getStatus() == Appointment.AppointmentStatus.SCHEDULED)
                .filter(existing -> currentProcessedAppointmentId <= 0 || existing.getId() != currentProcessedAppointmentId)
                .collect(Collectors.toList());

        boolean overlap = false;
        for (Appointment existing : existingAppointments) {
            Optional<Service> existingServiceOpt = serviceRepository.findById(existing.getServiceId());
            if (existingServiceOpt.isPresent()) {
                Service existingService = existingServiceOpt.get();
                LocalDateTime existingStartTime = existing.getAppointmentDateTime();
                LocalDateTime existingEndTime = existingStartTime.plusMinutes(existingService.getDurationInMinutes());

                if (proposedStartTime.isBefore(existingEndTime) && proposedEndTime.isAfter(existingStartTime)) {
                    overlap = true;
                    System.out.println("Conflict detected with existing appointment ID: " + existing.getId() +
                            " which runs from " + existingStartTime + " to " + existingEndTime +
                            ". Proposed: " + proposedStartTime + " to " + proposedEndTime);
                    break;
                }
            }
        }

        if (overlap) {
            throw new RuntimeException("Appointment conflict: The selected time slot overlaps with an existing appointment.");
        }

        return appointmentRepository.save(processedAppointmentRequest);
    }

    public void deleteAppointment(long id) {
        appointmentRepository.deleteById(id);
    }

    public Appointment updateAppointmentStatus(long id, Appointment.AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + id));
        appointment.setStatus(newStatus);
        return appointmentRepository.save(appointment);
    }


    public List<Appointment> filterAppointments(List<Appointment> appointments, String filter) {
        if (filter == null || filter.trim().isEmpty() || appointments == null) {
            return appointments;
        }

        String lowerCaseFilter = filter.toLowerCase();
        LocalDate today = LocalDate.now();

        return appointments.stream()
                .filter(app -> {
                    if (app == null) return false;
                    switch (lowerCaseFilter) {
                        case "completed":
                            return app.getStatus() == Appointment.AppointmentStatus.COMPLETED;
                        case "cancelled":
                            return app.getStatus() == Appointment.AppointmentStatus.CANCELLED;
                        case "scheduled":
                            return app.getStatus() == Appointment.AppointmentStatus.SCHEDULED;
                        case "today":

                            return app.getAppointmentDateTime() != null &&
                                    app.getAppointmentDateTime().toLocalDate().isEqual(today);
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }
}
