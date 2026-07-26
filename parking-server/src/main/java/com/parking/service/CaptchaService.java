package com.parking.service;

import java.util.Map;

public interface CaptchaService {
    Map<String, String> generateCaptcha();
    boolean validateCaptcha(String key, String code);
}
