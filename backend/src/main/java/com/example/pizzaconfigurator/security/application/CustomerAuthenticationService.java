package com.example.pizzaconfigurator.security.application;

import com.example.pizzaconfigurator.security.api.AuthResult;
import com.example.pizzaconfigurator.security.api.CustomerAuthentication;
import com.example.pizzaconfigurator.security.api.CustomerQuery;
import com.example.pizzaconfigurator.security.api.CustomerView;
import com.example.pizzaconfigurator.security.api.EmailAlreadyRegisteredException;
import com.example.pizzaconfigurator.security.api.InvalidCredentialsException;
import com.example.pizzaconfigurator.security.domain.Customer;
import com.example.pizzaconfigurator.security.infrastructure.persistence.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CustomerAuthenticationService implements CustomerAuthentication, CustomerQuery {

    private final CustomerRepository customers;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    CustomerAuthenticationService(CustomerRepository customers, JwtTokenProvider tokenProvider) {
        this.customers = customers;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResult register(String name, String email, String phoneNumber, String rawPassword) {
        if (customers.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        Customer customer = customers.save(new Customer(name, email, phoneNumber, passwordEncoder.encode(rawPassword)));
        return authResultFor(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        Customer customer = customers.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (customer.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return authResultFor(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> resolveCustomerId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenProvider.parseSubject(token);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerView> findCustomer(UUID customerId) {
        return customers.findById(customerId).map(this::toView);
    }

    private AuthResult authResultFor(Customer customer) {
        String token = tokenProvider.issueToken(customer.getCustomerId());
        return new AuthResult(toView(customer), token);
    }

    private CustomerView toView(Customer customer) {
        return new CustomerView(customer.getCustomerId(), customer.getName(), customer.getEmail());
    }
}
