package com.webchat.auth;

import com.webchat.common.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 12;

    public void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BadRequestException("Password must be at least " + MIN_LENGTH + " characters");
        }
        boolean lower = false, upper = false, digit = false, special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) lower = true;
            else if (Character.isUpperCase(c)) upper = true;
            else if (Character.isDigit(c)) digit = true;
            else if (!Character.isWhitespace(c)) special = true;
        }
        if (!lower) throw new BadRequestException("Password must contain a lowercase letter");
        if (!upper) throw new BadRequestException("Password must contain an uppercase letter");
        if (!digit) throw new BadRequestException("Password must contain a digit");
        if (!special) throw new BadRequestException("Password must contain a special character");
    }
}
