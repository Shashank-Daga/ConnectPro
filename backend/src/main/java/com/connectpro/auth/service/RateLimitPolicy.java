package com.connectpro.auth.service;

import java.time.Duration;

public final class RateLimitPolicy {

    private RateLimitPolicy() {
    }

    public static final int LOGIN_MAX_ATTEMPTS = 5;
    public static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    public static final int PIN_RESET_REQUEST_MAX_ATTEMPTS = 5;
    public static final Duration PIN_RESET_REQUEST_WINDOW = Duration.ofMinutes(15);

    public static final int PIN_RESET_MAX_ATTEMPTS = 5;
    public static final Duration PIN_RESET_WINDOW = Duration.ofMinutes(15);

    public static final int VERIFICATION_MAX_ATTEMPTS = 5;
    public static final Duration VERIFICATION_WINDOW = Duration.ofMinutes(15);

    public static final int REGISTRATION_MAX_ATTEMPTS = 5;
    public static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

    public static final int REFRESH_MAX_ATTEMPTS = 10;
    public static final Duration REFRESH_WINDOW = Duration.ofMinutes(15);

    public static final int LOCATION_UPDATE_MAX_ATTEMPTS = 60;
    public static final Duration LOCATION_UPDATE_WINDOW = Duration.ofMinutes(1);

    public static final int LOGIN_IP_MAX_ATTEMPTS = 20;
    public static final Duration LOGIN_IP_WINDOW = Duration.ofMinutes(15);

    public static final int REGISTRATION_IP_MAX_ATTEMPTS = 10;
    public static final Duration REGISTRATION_IP_WINDOW = Duration.ofHours(1);

    public static final int PIN_RESET_REQUEST_IP_MAX_ATTEMPTS = 10;
    public static final Duration PIN_RESET_REQUEST_IP_WINDOW = Duration.ofHours(1);

    public static final int PIN_RESET_IP_MAX_ATTEMPTS = 20;
    public static final Duration PIN_RESET_IP_WINDOW = Duration.ofMinutes(15);

    public static final int VERIFICATION_IP_MAX_ATTEMPTS = 20;
    public static final Duration VERIFICATION_IP_WINDOW = Duration.ofMinutes(15);

    public static final int REFRESH_IP_MAX_ATTEMPTS = 30;
    public static final Duration REFRESH_IP_WINDOW = Duration.ofMinutes(15);
}
