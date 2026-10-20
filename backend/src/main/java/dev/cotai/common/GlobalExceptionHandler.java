package dev.cotai.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Maps exceptions to RFC 9457 Problem Details ({@code application/problem+json}) with an extra
 * {@code code} field.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://cotai.dev/problems/";

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail problem = problem(ex.code(), ex.getMessage(), request);
        ex.properties().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
                .toList();
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, "Request has invalid fields.", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleInvalidParams(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> Map.of(
                                "field", String.valueOf(r.getMethodParameter().getParameterName()),
                                "message", String.valueOf(e.getDefaultMessage()))))
                .toList();
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, "Request has invalid parameters.", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class
    })
    ProblemDetail handleMalformed(Exception ex, HttpServletRequest request) {
        return problem(ErrorCode.VALIDATION_FAILED, ex.getMessage(), request);
    }

    /** Upload above spring.servlet.multipart limits, rejected before reaching the controller. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return problem(ErrorCode.PHOTO_TOO_LARGE, "Photo is larger than 5 MB.", request);
    }

    /** Unknown route or static file (e.g. "/" when the SPA is not bundled): 404, not 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return problem(ErrorCode.NOT_FOUND, "Not found", request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static ProblemDetail problem(ErrorCode code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setType(URI.create(TYPE_BASE + code.slug()));
        problem.setTitle(code.title());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        return problem;
    }
}
