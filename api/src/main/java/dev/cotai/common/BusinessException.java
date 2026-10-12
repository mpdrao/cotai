package dev.cotai.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business rule violation. Translated to a Problem Detail by {@link GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> properties;

    public BusinessException(ErrorCode code, String detail) {
        this(code, detail, Map.of());
    }

    public BusinessException(ErrorCode code, String detail, Map<String, Object> properties) {
        super(detail);
        this.code = code;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(ErrorCode.NOT_FOUND, what + " not found");
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, Object> properties() {
        return properties;
    }
}
