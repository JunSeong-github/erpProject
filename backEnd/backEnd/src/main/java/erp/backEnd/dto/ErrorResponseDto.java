package erp.backEnd.dto;

import erp.backEnd.exception.ErrorCode;
import jakarta.validation.ConstraintViolation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponseDto {
    private String code;
    private String message;
    private List<FieldError> errors;

    private ErrorResponseDto(final ErrorCode code, final List<FieldError> errors) {
        this.code = code.getCode();
        this.message = code.getMessage();
        this.errors = errors;
    }

    private ErrorResponseDto(final ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
        this.errors = new ArrayList<>();
    }

    private ErrorResponseDto(final ErrorCode code, final Exception exception) {
        this.code = code.getCode();
        this.message = exception.getMessage();
        this.errors = new ArrayList<>();
    }

    public static ErrorResponseDto of(final ErrorCode code, final BindingResult bindingResult) {
        return new ErrorResponseDto(code, FieldError.of(bindingResult));
    }

    public static ErrorResponseDto of(final ErrorCode code, final Exception exception) {
        return new ErrorResponseDto(code, exception);
    }

    //데이터 유효성 검사시 발생하는 실패정보를 담고있는 ConstraintViolation을 가짐
    public static ErrorResponseDto of(final ErrorCode code, final Set<ConstraintViolation<?>> constraintViolations) {
        return new ErrorResponseDto(code, FieldError.of(constraintViolations));
    }

    public static ErrorResponseDto of(final ErrorCode code, final String missingParameterName, final String reason) {
        return new ErrorResponseDto(code, FieldError.of(missingParameterName, "", reason));
    }

    public static ErrorResponseDto of(final ErrorCode code, final String missingParameterName, final String value, final String reason) {
        return new ErrorResponseDto(code, FieldError.of(missingParameterName, value, reason));
    }

    public static ErrorResponseDto of(final ErrorCode code) {
        return new ErrorResponseDto(code);
    }

    public static ErrorResponseDto of(final ErrorCode code, final List<FieldError> errors) {
        return new ErrorResponseDto(code, errors);
    }

    public static ErrorResponseDto of(MethodArgumentTypeMismatchException e) {
        final String value = e.getValue() == null ? "" : e.getValue().toString();
        final List<FieldError> errors = FieldError.of(e.getName(), value, e.getErrorCode());
        return new ErrorResponseDto(ErrorCode.INVALID_TYPE_VALUE, errors);
    }


    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FieldError {
        private String field;
        private String value;
        private String reason;

        public FieldError(String field, String value, String reason) {
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

        public static List<FieldError> of(final String field, final String value, final String reason) {
            List<FieldError> fieldErrors = new ArrayList<>();
            fieldErrors.add(new FieldError(field, value, reason));
            return fieldErrors;
        }

        public static List<FieldError> of(final BindingResult bindingResult) {
            final List<org.springframework.validation.FieldError> fieldErrors = bindingResult.getFieldErrors();
            return fieldErrors.stream()
                              .map(error -> new FieldError(
                                      error.getField(),
                                      error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                                      error.getDefaultMessage()
                              ))
                              .collect(Collectors.toList());
        }

        public static List<FieldError> of(final Set<ConstraintViolation<?>> constraintViolations) {
            List<ConstraintViolation<?>> lists = new ArrayList<>(constraintViolations);
            return lists.stream()
                        .map(error -> new FieldError(
                                error.getPropertyPath().toString(),
                                "",
                                error.getMessageTemplate()
                        ))
                        .collect(Collectors.toList());
        }

        public static List<HttpMethod> of(final Set<HttpMethod> supportedMethods, String method) {
            List<HttpMethod> lists = new ArrayList<>(supportedMethods);
            return lists;
        }
    }
}
