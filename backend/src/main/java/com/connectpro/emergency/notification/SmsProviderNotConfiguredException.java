package com.connectpro.emergency.notification;

public class SmsProviderNotConfiguredException
        extends RuntimeException {

    public SmsProviderNotConfiguredException() {
        super("SMS delivery provider is not configured");
    }
}
