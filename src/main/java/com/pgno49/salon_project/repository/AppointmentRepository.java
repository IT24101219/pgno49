package com.pgno49.salon_project.repository;

import com.pgno49.salon_project.model.Appointment;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class AppointmentRepository {

    @Value("${data.file.basepath:src/main/resources/data/}")
    private String dataFileBasePath;

    private Path dataFilePath;
    private static final String APPOINTMENT_FILE_NAME = "appointments.txt";
    private static final String DELIMITER = ",";

    private AtomicLong nextId = new AtomicLong(1);

    @PostConstruct
    private void initialize() {
        try {
            Path dataDir = Paths.get(dataFileBasePath);
            if (Files.notExists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("Created data directory: " + dataDir.toAbsolutePath());
            }
            this.dataFilePath = dataDir.resolve(APPOINTMENT_FILE_NAME);
            if (Files.notExists(this.dataFilePath)) {
                Files.createFile(this.dataFilePath);
                System.out.println("Created data file: " + this.dataFilePath.toAbsolutePath());
            }
            loadAppointmentsFromFile();
            System.out.println("AppointmentRepository initialized. Data file path: " + this.dataFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error initializing AppointmentRepository: " + e.getMessage());
            throw new RuntimeException("Could not initialize AppointmentRepository", e);
        }
    }


    private List<Appointment> loadAppointmentsFromFile() {
        List<Appointment> appointments = new ArrayList<>();
        long maxId = 0;
        if (Files.notExists(dataFilePath)) {
            System.err.println("Warning: Data file does not exist during load: " + dataFilePath);
            nextId.set(1);
            return appointments;
        }
        try (BufferedReader reader = Files.newBufferedReader(dataFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Appointment appointment = Appointment.fromCsvString(line, DELIMITER);
                if (appointment != null) {
                    appointments.add(appointment);
                    if (appointment.getId() > maxId) {
                        maxId = appointment.getId();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading appointments from file: " + dataFilePath + " - " + e.getMessage());
        }
        nextId.set(maxId + 1);
        return appointments;
    }


    private void saveAppointmentsToFile(List<Appointment> appointments) {
        synchronized (this) {
            try (BufferedWriter writer = Files.newBufferedWriter(dataFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (Appointment appointment : appointments) {
                    writer.write(appointment.toCsvString(DELIMITER));
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("Error writing appointments to file: " + dataFilePath + " - " + e.getMessage());
                throw new RuntimeException("Could not save appointments to file", e);
            }
        }
    }


    public List<Appointment> findAll() {
        return loadAppointmentsFromFile();
    }

    public Optional<Appointment> findById(long id) {
        return findAll().stream()
                .filter(appointment -> appointment.getId() == id)
                .findFirst();
    }


    public List<Appointment> findByCustomerId(Long customerId) {
        if (customerId == null) return List.of();
        return findAll().stream()
                .filter(appointment -> Objects.equals(appointment.getCustomerId(), customerId))
                .collect(Collectors.toList());
    }

    public List<Appointment> findByServiceId(Long serviceId) {
        if (serviceId == null) return List.of();
        return findAll().stream()
                .filter(appointment -> Objects.equals(appointment.getServiceId(), serviceId))
                .collect(Collectors.toList());
    }



    public Appointment save(Appointment appointment) {
        List<Appointment> appointments = findAll();
        if (appointment.getId() <= 0) {
            appointment.setId(nextId.getAndIncrement());
            if (appointment.getStatus() == null) {
                appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
            }
            appointments.add(appointment);
        } else {
            boolean found = false;
            for (int i = 0; i < appointments.size(); i++) {
                if (appointments.get(i).getId() == appointment.getId()) {
                    appointments.set(i, appointment);
                    found = true;
                    break;
                }
            }
            if (!found) {
                appointments.add(appointment);
                if(appointment.getId() >= nextId.get()){ nextId.set(appointment.getId() + 1); }
            }
        }
        saveAppointmentsToFile(appointments);
        return appointment;
    }


    public void deleteById(long id) {
        List<Appointment> appointments = findAll();
        List<Appointment> updatedAppointments = appointments.stream()
                .filter(appointment -> appointment.getId() != id)
                .collect(Collectors.toList());

        if (appointments.size() != updatedAppointments.size()) {
            saveAppointmentsToFile(updatedAppointments);
        } else {
            System.out.println("Appointment with ID " + id + " not found for deletion.");
        }
    }
}
