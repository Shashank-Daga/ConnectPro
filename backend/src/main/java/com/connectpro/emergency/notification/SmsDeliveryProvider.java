package com.connectpro.emergency.notification;

public interface SmsDeliveryProvider {

    void sendSms(String phoneNumber, String message);
}
