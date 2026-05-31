package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.dao.UserDAO;
import com.lfms.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User-management business logic for the admin screens. Keeps controllers off the DAO
 * layer (strict Controller -&gt; Service -&gt; DAO separation).
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditDAO = new AuditLogDAO();

    public List<User> findAll() {
        return userDAO.findAll();
    }

    public User findById(int userId) {
        return userDAO.findById(userId);
    }

    public int countAll() {
        return userDAO.findAll().size();
    }

    public int countByRole(String role) {
        return userDAO.countByRole(role);
    }

    /** Activates or deactivates a user and records the action in the audit log. */
    public boolean setActive(int userId, boolean active, int adminId) {
        boolean ok = userDAO.updateActive(userId, active);
        if (ok) {
            auditDAO.log(adminId, active ? "ACTIVATE_USER" : "DEACTIVATE_USER", "USER", userId,
                    active ? "User account activated" : "User account deactivated");
        }
        return ok;
    }

    /**
     * Deletes a user. Returns false (rather than throwing) if the delete is rejected,
     * which typically means the user still has reports or claims referencing them — in
     * that case the caller should suggest deactivating instead.
     */
    public boolean delete(int userId, int adminId) {
        try {
            boolean ok = userDAO.delete(userId);
            if (ok) {
                auditDAO.log(adminId, "DELETE_USER", "USER", userId, "User account deleted");
            }
            return ok;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Convenience lookup of userId -&gt; name for resolving "Reported By" columns efficiently. */
    public Map<Integer, String> nameMap() {
        Map<Integer, String> map = new HashMap<>();
        for (User user : userDAO.findAll()) {
            map.put(user.getUserId(), user.getName());
        }
        return map;
    }
}
