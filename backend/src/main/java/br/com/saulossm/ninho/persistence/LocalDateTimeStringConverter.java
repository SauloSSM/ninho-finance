package br.com.saulossm.ninho.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;

@Converter
public class LocalDateTimeStringConverter implements AttributeConverter<LocalDateTime, String> {

    @Override
    public String convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String value) {
        return value == null ? null : LocalDateTime.parse(value);
    }
}
