package com.lfms.service;

import com.lfms.dao.AuditLogDAO;
import com.lfms.model.AuditLog;

import java.util.List;

/**
 * Read access to the audit trail for the admin screens.
 */
public class AuditService {

    private final AuditLogDAO auditDAO = new AuditLogDAO();

    public List<AuditLog> findRecent(int limit) {
        return auditDAO.findRecent(limit);
    }

    public List<AuditLog> findAll() {
        return auditDAO.findAll();
    }
}
