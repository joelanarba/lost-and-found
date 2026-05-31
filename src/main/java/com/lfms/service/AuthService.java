package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.dao.UserDAO;
import com.lfms.model.User;
import com.lfms.util.HashUtil;
import com.lfms.util.ValidationUtil;

/**
 * Authentication and registration business logic.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditDAO = new AuditLogDAO();

    /**
     * Attempts to log a user in. Returns the {@link User} on success, or {@code null} if the
     * email is unknown, the account is deactivated, or the password is wrong.
     */
    public User login(String email, String password) {
        if (!ValidationUtil.isNotEmpty(email) || !ValidationUtil.isNotEmpty(password)) {
            return null;
        }
        User user = userDAO.findByEmail(email.trim());
        if (user == null || !user.isActive()) {
            return null;
        }
        if (!HashUtil.verify(password, user.getPasswordHash())) {
            return null;
        }
        auditDAO.log(user.getUserId(), "LOGIN", "USER", user.getUserId(), "User logged in");
        return user;
    }

    /**
     * Registers a new STUDENT account after validating input and uniqueness.
     */
    public Result register(String name, String indexNo, String email, String password, String phone) {
        if (!ValidationUtil.isNotEmpty(name)) {
            return Result.fail("Full name is required.");
        }
        if (!ValidationUtil.isNotEmpty(indexNo)) {
            return Result.fail("Index / ID number is required.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            return Result.fail("Enter a valid email address.");
        }
        if (!ValidationUtil.isMinLength(password, 8)) {
            return Result.fail("Password must be at least 8 characters.");
        }
        if (userDAO.emailExists(email.trim())) {
            return Result.fail("An account with this email already exists.");
        }
        if (userDAO.indexNoExists(indexNo.trim())) {
            return Result.fail("This index / ID number is already registered.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setIndexNo(indexNo.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(HashUtil.hash(password));
        user.setPhone(ValidationUtil.isNotEmpty(phone) ? phone.trim() : null);
        user.setRole("STUDENT");
        user.setActive(true);

        if (!userDAO.create(user)) {
            return Result.fail("Registration failed. Please try again.");
        }

        User created = userDAO.findByEmail(email.trim());
        if (created != null) {
            auditDAO.log(created.getUserId(), "REGISTER", "USER", created.getUserId(),
                    "New student account registered");
        }
        return Result.ok();
    }

    /**
     * Simple success/error wrapper returned by {@link #register}.
     */
    public static class Result {
        private final boolean success;
        private final String errorMessage;

        private Result(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
