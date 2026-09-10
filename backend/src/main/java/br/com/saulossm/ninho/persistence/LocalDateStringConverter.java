package br.com.saulossm.ninho.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

@Converter
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate value) {
        return value == null ? null : value.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}
