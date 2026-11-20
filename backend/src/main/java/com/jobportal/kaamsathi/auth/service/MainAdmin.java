package com.jobportal.kaamsathi.auth.service;

import com.jobportal.kaamsathi.auth.entity.User;
import com.jobportal.kaamsathi.common.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Who is the MAIN admin? The ADMIN account whose email is ADMIN_EMAIL in .env (made by AdminSeeder).
 * Always checked on the user row from the DB, never on the token.
 */
@Component
public class MainAdmin {

    private final String email;

    public MainAdmin(@Value("${app.admin.email}") String email) {
        this.email = email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean is(User user) {
        return !email.isEmpty() && user.getRole() == Role.ADMIN && email.equals(user.getEmail());
    }
}
