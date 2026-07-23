package com.aivle13.fin_audit_ai.global.validation;

import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    public void validate(String password, String passwordConfirm) {
        validatePolicy(password);
        validateConfirm(password, passwordConfirm);
    }

    // 비밀번호 확인값 일치 여부 검증
    private void validateConfirm(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new BusinessException(
                    ErrorCode.PASSWORD_CONFIRM_NOT_MATCH
            );
        }
    }

    // 비밀번호 정책 검증
    private void validatePolicy(String password) {
        boolean hasLetter = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        String excludedCharacters = "()<>\"';";

        for (char character : password.toCharArray()) {

            if (Character.isWhitespace(character)
                    || character < 33
                    || character > 126
                    || excludedCharacters.indexOf(character) >= 0) {

                throw new BusinessException(
                        ErrorCode.INVALID_PASSWORD_POLICY
                );
            }

            if ((character >= 'A' && character <= 'Z')
                    || (character >= 'a'
                    && character <= 'z')) {

                hasLetter = true;

            } else if (character >= '0'
                    && character <= '9') {

                hasNumber = true;

            } else {
                hasSpecial = true;
            }
        }

        int typeCount = 0;

        if (hasLetter) {
            typeCount++;
        }

        if (hasNumber) {
            typeCount++;
        }

        if (hasSpecial) {
            typeCount++;
        }

        int length = password.length();

        boolean validTwoTypes =
                typeCount == 2
                        && length >= 10
                        && length <= 16;

        boolean validThreeTypes =
                typeCount == 3
                        && length >= 8
                        && length <= 16;

        if (!validTwoTypes && !validThreeTypes) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY);
        }
    }
}