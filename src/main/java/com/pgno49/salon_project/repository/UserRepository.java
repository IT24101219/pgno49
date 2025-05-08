package com.pgno49.salon_project.repository;

import com.pgno49.salon_project.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
public class UserRepository {

    @Value("${data.file.basepath:src/main/resources/data/}")
    private String dataFileBasePath;

    private Path dataFilePath;
    private static final String USER_FILE_NAME = "users.txt";
    private static final String DELIMITER = ",";

    private AtomicLong nextId = new AtomicLong(1);

    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.initial-admin.username:admin}")
    private String initialAdminUsername;
    @Value("${app.security.initial-admin.email:admin@salon.local}")
    private String initialAdminEmail;
    @Value("${app.security.initial-admin.phone:0000000000}")
    private String initialAdminPhone;
    @Value("${app.security.initial-admin.password:admin123}")
    private String initialAdminPassword;
    @Value("${app.security.initial-admin.fullname:Main Admin}")
    private String initialAdminFullName;

    @Autowired
    public UserRepository(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    private void initialize() {
        try {
            Path dataDir = Paths.get(dataFileBasePath);
            if (Files.notExists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("Created data directory: " + dataDir.toAbsolutePath());
            }
            this.dataFilePath = dataDir.resolve(USER_FILE_NAME);
            boolean fileExists = Files.exists(this.dataFilePath);

            if (!fileExists) {
                Files.createFile(this.dataFilePath);
                System.out.println("Created data file: " + this.dataFilePath.toAbsolutePath());
            }

            List<User> users = loadUsersFromFile();

            if (users.isEmpty()) {
                System.out.println("User file is empty. Creating initial MAIN_ADMIN user...");
                User adminUser = new User();
                adminUser.setId(nextId.getAndIncrement());
                adminUser.setUsername(initialAdminUsername);
                adminUser.setEmail(initialAdminEmail);
                adminUser.setPhoneNumber(initialAdminPhone);
                adminUser.setPasswordHash(passwordEncoder.encode(initialAdminPassword));
                adminUser.setFullName(initialAdminFullName);
                adminUser.setRole(User.Role.MAIN_ADMIN);
                adminUser.setStatus(User.AccountStatus.ACTIVE);

                users.add(adminUser);
                saveUsersToFile(users);
                System.out.println("Initial MAIN_ADMIN user created.");
            } else {
                System.out.println("UserRepository initialized. Found existing users. Data file path: " + this.dataFilePath.toAbsolutePath());
            }

        } catch (IOException e) {
            System.err.println("Error initializing UserRepository: " + e.getMessage());
            throw new RuntimeException("Could not initialize UserRepository", e);
        }
    }

    private List<User> loadUsersFromFile() {
        List<User> users = new ArrayList<>();
        long maxId = 0;
        if (Files.notExists(dataFilePath)) {
            System.err.println("Warning: Data file does not exist during load: " + dataFilePath);
            nextId.set(1);
            return users;
        }
        try (BufferedReader reader = Files.newBufferedReader(dataFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = User.fromCsvString(line, DELIMITER);
                if (user != null) {
                    users.add(user);
                    if (user.getId() > maxId) {
                        maxId = user.getId();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading users from file: " + dataFilePath + " - " + e.getMessage());
        }
        nextId.set(maxId + 1);
        return users;
    }

    private void saveUsersToFile(List<User> users) {
        synchronized (this) {
            try (BufferedWriter writer = Files.newBufferedWriter(dataFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (User user : users) {
                    writer.write(user.toCsvString(DELIMITER));
                    writer.newLine();
                }
            } catch (IOException e) {
                System.err.println("Error writing users to file: " + dataFilePath + " - " + e.getMessage());
                throw new RuntimeException("Could not save users to file", e);
            }
        }
    }

    public List<User> findAll() {
        return loadUsersFromFile();
    }

    public Optional<User> findById(long id) {
        return findAll().stream()
                .filter(user -> user.getId() == id)
                .findFirst();
    }

    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();
        return findById(id.longValue());
    }

    public User save(User user) {
        List<User> users = findAll();
        if (user.getId() <= 0) {
            user.setId(nextId.getAndIncrement());
            if (user.getStatus() == null) {
                user.setStatus(user.getRole() == User.Role.CUSTOMER ? User.AccountStatus.ACTIVE : User.AccountStatus.PENDING_APPROVAL);
            }
            users.add(user);
        } else {
            boolean found = false;
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId() == user.getId()) {
                    if (!StringUtils.hasText(user.getPasswordHash())) {
                        user.setPasswordHash(users.get(i).getPasswordHash());
                    }
                    users.set(i, user);
                    found = true;
                    break;
                }
            }
            if (!found) {
                users.add(user);
                if(user.getId() >= nextId.get()){ nextId.set(user.getId() + 1); }
            }
        }
        saveUsersToFile(users);
        return user;
    }


    public void updatePasswordHash(long userId, String newPasswordHash) {
        List<User> users = findAll();
        boolean updated = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == userId) {
                users.get(i).setPasswordHash(newPasswordHash);
                updated = true;
                break;
            }
        }
        if (updated) {
            saveUsersToFile(users);
        } else {
            throw new RuntimeException("User not found for password update: ID " + userId);
        }
    }


    public void deleteById(long id) {
        List<User> users = findAll();
        List<User> updatedUsers = users.stream()
                .filter(user -> user.getId() != id)
                .collect(Collectors.toList());

        if (users.size() != updatedUsers.size()) {
            saveUsersToFile(updatedUsers);
        } else {
            System.out.println("User with ID " + id + " not found for deletion.");
        }
    }

    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) { return Optional.empty(); }
        String trimmedUsername = username.trim();
        return findAll().stream()
                .filter(user -> trimmedUsername.equalsIgnoreCase(user.getUsername()))
                .findFirst();
    }

    public Optional<User> findByEmail(String email) {
        if (!StringUtils.hasText(email)) { return Optional.empty(); }
        String trimmedEmail = email.trim().toLowerCase();
        return findAll().stream()
                .filter(user -> trimmedEmail.equalsIgnoreCase(user.getEmail()))
                .findFirst();
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) { return Optional.empty(); }
        String trimmedPhone = phoneNumber.trim();
        return findAll().stream()
                .filter(user -> user.getPhoneNumber() != null && trimmedPhone.equals(user.getPhoneNumber()))
                .findFirst();
    }

    public List<User> findUsersByRoleAndStatus(User.Role role, User.AccountStatus status) {
        return findAll().stream()
                .filter(user -> user.getRole() == role && user.getStatus() == status)
                .collect(Collectors.toList());
    }
}