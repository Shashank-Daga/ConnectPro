package com.connectpro.emergency.notification;

import org.springframework.stereotype.Component;

@Component
public class UnavailableSmsDeliveryProvider
        implements SmsDeliveryProvider {

    @Override
    public void sendSms(String phoneNumber, String message) {
        throw new SmsProviderNotConfiguredException();
    }
}
