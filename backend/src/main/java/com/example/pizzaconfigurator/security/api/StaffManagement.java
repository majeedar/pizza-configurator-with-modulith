package com.example.pizzaconfigurator.security.api;

import com.example.pizzaconfigurator.security.domain.EmployeeRole;
import java.util.List;
import java.util.UUID;

/** Agent.md §7.10/§8.3/§9.3 "Staff Users" admin management. */
public interface StaffManagement {

    StaffView createStaff(String username, String displayName, String email, String rawPassword, EmployeeRole role);

    List<StaffView> listStaff();

    StaffView setEnabled(UUID employeeId, boolean enabled);
}
