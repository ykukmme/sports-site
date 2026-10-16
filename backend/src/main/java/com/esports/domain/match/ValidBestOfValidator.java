package com.esports.domain.match;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// @ValidBestOf 검증기 — null 은 통과 (선택 입력)
public class ValidBestOfValidator implements ConstraintValidator<ValidBestOf, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value == null || BestOf.isAllowed(value);
    }
}
