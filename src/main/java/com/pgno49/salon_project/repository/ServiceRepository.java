package com.pgno49.salon_project.repository;

import com.pgno49.salon_project.model.Service;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class ServiceRepository {

    @Value("${data.file.basepath:src/main/resources/data/}")
    private String dataFileBasePath;

    private Path dataFilePath;
    private static final String SERVICE_FILE_NAME = "services.txt";
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
            this.dataFilePath = dataDir.resolve(SERVICE_FILE_NAME);
            if (Files.notExists(this.dataFilePath)) {
                Files.createFile(this.dataFilePath);
                System.out.println("Created data file: " + this.dataFilePath.toAbsolutePath());
            }

            loadServicesFromFile();
            System.out.println("ServiceRepository initialized. Data file path: " + this.dataFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error initializing ServiceRepository: " + e.getMessage());
            throw new RuntimeException("Could not initialize ServiceRepository", e);
        }
    }

    private List<Service> loadServicesFromFile() {
        List<Service> services = new ArrayList<>();
        long maxId = 0;
        if (Files.notExists(dataFilePath)) {
            System.err.println("Warning: Data file does not exist during load: " + dataFilePath);
            nextId.set(1);
            return services;
        }
        try (BufferedReader reader = Files.newBufferedReader(dataFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Service service = Service.fromCsvString(line, DELIMITER);
                if (service != null) {
                    services.add(service);
                    if (service.getId() > maxId) {
                        maxId = service.getId();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading services from file: " + dataFilePath + " - " + e.getMessage());
        }
        nextId.set(maxId + 1);
        return services;
    }

    private void saveServicesToFile(List<Service> services) {
        synchronized (this) {
            try (BufferedWriter writer = Files.newBufferedWriter(dataFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (Service service : services) {
                    writer.write(service.toCsvString(DELIMITER));
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("Error writing services to file: " + dataFilePath + " - " + e.getMessage());
                throw new RuntimeException("Could not save services to file", e);
            }
        }
    }

    public List<Service> findAll() {
        return loadServicesFromFile();
    }

    public Optional<Service> findById(long id) {
        return findAll().stream()
                .filter(service -> service.getId() == id)
                .findFirst();
    }

    public Service save(Service service) {
        List<Service> services = findAll();
        if (service.getId() <= 0) {
            service.setId(nextId.getAndIncrement());
            services.add(service);
        } else {
            boolean found = false;
            for (int i = 0; i < services.size(); i++) {
                if (services.get(i).getId() == service.getId()) {
                    services.set(i, service);
                    found = true;
                    break;
                }
            }
            if (!found) {
                services.add(service);
                if(service.getId() >= nextId.get()){ nextId.set(service.getId() + 1); }
            }
        }
        saveServicesToFile(services);
        return service;
    }

    public void deleteById(long id) {
        List<Service> services = findAll();
        List<Service> updatedServices = services.stream()
                .filter(service -> service.getId() != id)
                .collect(Collectors.toList());

        if (services.size() != updatedServices.size()) {
            saveServicesToFile(updatedServices);
        } else {
            System.out.println("Service with ID " + id + " not found for deletion.");
        }
    }
}