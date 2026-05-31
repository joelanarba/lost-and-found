package com.lfms.util;

import com.lfms.model.User;

/**
 * Thread-safe singleton holding the currently logged-in user for the session.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private volatile User currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void clearSession() {
        this.currentUser = null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
