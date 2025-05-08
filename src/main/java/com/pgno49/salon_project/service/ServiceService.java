package com.pgno49.salon_project.service;

import com.pgno49.salon_project.model.Service;
import com.pgno49.salon_project.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ServiceService {

    private final ServiceRepository serviceRepository;

    @Autowired
    public ServiceService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Optional<Service> getServiceById(long id) {
        return serviceRepository.findById(id);
    }

    public Service saveService(Service service) {

        if (service.getName() == null || service.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be empty.");
        }
        if (service.getPrice() == null || service.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Service price cannot be null or negative.");
        }
        if (service.getDurationInMinutes() <= 0) {
            throw new IllegalArgumentException("Service duration must be positive.");
        }

        return serviceRepository.save(service);
    }

    public void deleteService(long id) {

        serviceRepository.deleteById(id);
    }

    public List<Service> searchServices(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllServices();
        }
        String lowerCaseQuery = query.toLowerCase();
        List<Service> allServices = getAllServices();

        return allServices.stream()
                .filter(service -> (service.getName() != null && service.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (service.getDescription() != null && service.getDescription().toLowerCase().contains(lowerCaseQuery)))
                .collect(Collectors.toList());
    }

    public Map<Long, Service> getAllServicesMap() {
        return getAllServices().stream()
                .collect(Collectors.toMap(Service::getId, service -> service));
    }
}