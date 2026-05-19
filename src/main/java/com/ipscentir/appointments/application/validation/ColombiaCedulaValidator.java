package com.ipscentir.appointments.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ColombiaCedulaValidator implements ConstraintValidator<ValidColombiaCedula, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        
        // Cédula colombiana: 8-10 dígitos (solo números)
        // Puede contener puntos como separador (ej: 1.234.567 o 1234567)
        String cleanCedula = value.replaceAll("\\.", "").trim();
        
        // Validar que sea solo números
        if (!cleanCedula.matches("\\d{8,10}")) {
            return false;
        }
        
        // Validar que no sea todos ceros o secuencia
        return !cleanCedula.matches("0+") && !isSequential(cleanCedula);
    }

    private boolean isSequential(String cedula) {
        // Validar que no sea una secuencia (111111, 123456, etc)
        char first = cedula.charAt(0);
        boolean allSame = cedula.chars().allMatch(c -> c == first);
        
        if (allSame) {
            return true;
        }
        
        // Verificar secuencia ascendente o descendente (1234567890, 9876543210)
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < cedula.length(); i++) {
            if (cedula.charAt(i) != cedula.charAt(i - 1) + 1) {
                ascending = false;
            }
            if (cedula.charAt(i) != cedula.charAt(i - 1) - 1) {
                descending = false;
            }
        }
        
        return ascending || descending;
    }
}
