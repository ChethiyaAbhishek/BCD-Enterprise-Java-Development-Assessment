package com.techmart.ejb.session.stateless;

import com.techmart.common.entity.Customer;
import com.techmart.ejb.dao.CustomerDAO;
import jakarta.ejb.*;
import jakarta.inject.Inject;
import lombok.extern.java.Log;
import org.springframework.lang.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
@Log
public class CustomerServiceBean {

    @Inject
    private CustomerDAO customerDAO;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Customer registerCustomer(String firstName, String lastName, String email,
                                     String phone, String password, String address) {

        if (customerDAO.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        String passwordHash = hashPassword(password);

        Customer customer = new Customer(firstName, lastName, email, passwordHash, phone, address);

        customerDAO.create(customer);
        log.info("Customer registered: " + email);
        return customer;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Customer authenticateCustomer(String email, String password) {
        Customer customer = customerDAO.findByEmail(email);
        if (customer == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String hashedInput = hashPassword(password);

        if (!customer.getPasswordHash().equals(hashedInput)) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        customerDAO.updateLastLogin(customer.getId());
        log.info("Customer logged in: " + email);
        return customer;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Customer getCustomerProfile(Long customerId) {
        Customer customer = customerDAO.findById(customerId);
        if (customer == null) throw new IllegalArgumentException("Customer not found: " + customerId);
        return customer;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Customer updateCustomerProfile(Long customerId, String address, String phone, String city) {
        Customer customer = customerDAO.findById(customerId);
        if (customer == null) throw new IllegalArgumentException("Customer not found: " + customerId);

        customer.setAddress(address);
        customer.setPhone(phone);
        customer.setCity(city);

        return customerDAO.update(customer);
    }

    private String hashPassword(@NonNull String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing algorithm not available", e);
        }
    }
}
