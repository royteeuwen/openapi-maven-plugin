package com.github.kbuntrock.utils;

import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public enum OpenApiDataType {

    STRING("string"),
    STRING_BINARY("string", OpenApiDataFormat.BINARY),
    STRING_DATE("string", OpenApiDataFormat.DATE),
    STRING_DATE_TIME("string", OpenApiDataFormat.DATE_TIME),
    BOOLEAN("boolean"),
    INTEGER_32("integer", OpenApiDataFormat.INT32),
    INTEGER_64("integer", OpenApiDataFormat.INT64),
    NUMBER_FLOAT("number", OpenApiDataFormat.FLOAT),
    NUMBER_DOUBLE("number", OpenApiDataFormat.DOUBLE),
    ARRAY("array", OpenApiDataFormat.UNKNOWN),
    OBJECT("object", OpenApiDataFormat.UNKNOWN);

    private final String value;
    private final OpenApiDataFormat format;

    OpenApiDataType(String openapiName) {
        this(openapiName, OpenApiDataFormat.NONE);
    }

    OpenApiDataType(String value, OpenApiDataFormat format) {
        this.value = value;
        this.format = format;
    }

    public String getValue() {
        return value;
    }

    public OpenApiDataFormat getFormat() {
        return format;
    }

    public static OpenApiDataType fromJavaType(Class<?> type) {
        if (Boolean.class == type || Boolean.TYPE == type) {
            return BOOLEAN;
        } else if (Integer.class == type || Integer.TYPE == type) {
            return INTEGER_32;
        } else if (Long.class == type || Long.TYPE == type) {
            return INTEGER_64;
        } else if (Float.class == type || Float.TYPE == type) {
            return NUMBER_FLOAT;
        } else if (Double.class == type || Double.TYPE == type) {
            return NUMBER_DOUBLE;
        } else if (String.class == type) {
            return STRING;
        } else if (LocalDateTime.class == type || Instant.class == type) {
            return STRING_DATE_TIME;
        } else if (LocalDate.class == type) {
            return STRING_DATE;
        } else if(MultipartFile.class == type) {
            return STRING_BINARY;
        } else if (type.isArray() || List.class == type || Set.class == type) {
            return ARRAY;
        } else if(type.isEnum()) {
            return STRING;
        }
        return OBJECT;
    }

}
