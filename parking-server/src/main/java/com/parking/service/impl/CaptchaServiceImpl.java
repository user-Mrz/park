package com.parking.service.impl;

import com.parking.service.CaptchaService;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public Map<String, String> generateCaptcha() {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        captcha.setCharType(Captcha.TYPE_DEFAULT);
        String code = captcha.text().toLowerCase();
        String key = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("captcha:" + key, code, 5, TimeUnit.MINUTES);
        Map<String, String> result = new HashMap<>();
        result.put("key", key);
        result.put("image", captcha.toBase64());
        return result;
    }

    @Override
    public boolean validateCaptcha(String key, String code) {
        if (key == null || code == null) return false;
        String stored = redisTemplate.opsForValue().get("captcha:" + key);
        if (stored != null && stored.equalsIgnoreCase(code)) {
            redisTemplate.delete("captcha:" + key);
            return true;
        }
        return false;
    }
}
