package com.canineai.backend.event;

/**
 * Event published after a new user is successfully persisted to MySQL.
 * Carried to WelcomeEmailListener once the surrounding @Transactional
 * method commits — so the user row is guaranteed to exist in the database
 * before any email is attempted.
 *
 * @param toEmail   the newly registered user's email address
 * @param fullName  the user's full name for personalisation
 */
public record WelcomeEmailEvent(String toEmail, String fullName) {}
