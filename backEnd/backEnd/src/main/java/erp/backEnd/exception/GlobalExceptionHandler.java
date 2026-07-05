package erp.backEnd.exception;

import erp.backEnd.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;

import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.nio.file.AccessDeniedException;
import java.sql.SQLIntegrityConstraintViolationException;

import static erp.backEnd.exception.ErrorCode.*;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Value("${spring.servlet.multipart.max-file-size:25MB}")
    private String maxFileSize;

    //ResponseEntityExceptionHandler Override
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpHeaders headers,
                                                                         HttpStatusCode status, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        String[] supportedMethods = e.getSupportedMethods();
        StringBuilder sb = new StringBuilder();
        sb.append("해당 URI에서 사용가능한 메서드 목록 : ");
        for (String supportedMethod : supportedMethods) {
            sb.append(supportedMethod);
            sb.append(" ");
        }
        final ErrorResponseDto response = ErrorResponseDto.of(METHOD_NOT_ALLOWED, sb.toString(), "현재 요청 HTTP메서드 : " + e.getMethod());
        return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(NOT_SUPPORTED_MEDIA_TYPE);
        return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }


    // @Valid, @Validated 에서 binding error 발생 시 (@RequestBody)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("exception: {}", e.getClass().getSimpleName());
        // 첫 번째 에러 메시지만 추출
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("유효성 검사에 실패하였습니다.");
        log.error("message: {}", message);
        final ErrorResponseDto response = ErrorResponseDto.of(INVALID_INPUT_VALUE, e.getBindingResult());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(NOT_FOUND, e.getRequestURL(), "존재하지 않는 URL입니다.");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 매핑되지 않은 정적 리소스 요청(예: prod 에서 비활성화된 /v3/api-docs, /swagger-ui/**).
     * base 클래스(ResponseEntityExceptionHandler)가 이미 이 예외를 다루므로 별도 @ExceptionHandler 를
     * 추가하면 'Ambiguous' 로 기동 실패한다. → 반드시 base 메서드를 override 해야 한다.
     * 기본 구현은 handleExceptionInternal(여기서 500 강제)을 타므로, 직접 404 를 반환한다.
     */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(NOT_FOUND, e.getResourcePath(), "존재하지 않는 리소스입니다.");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response;
        if (e.getCause() instanceof MismatchedInputException mismatchedInputException) {
            response = ErrorResponseDto.of(INVALID_INPUT_VALUE,
                    mismatchedInputException.getPath().get(0).getPropertyName() + " 필드의값이 잘못되었습니다.", "" , "입력 포맷을 확인해 보십시오");
        } else response = ErrorResponseDto.of(INVALID_INPUT_VALUE, "", "입력 포맷을 확인해 보십시오");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());

        final ErrorResponseDto response = ErrorResponseDto.of(MISSING_PARAMETER, "결여된 파라미터 명 : " + e.getParameterName(), "" );
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    //Spring security 오류

//    @ExceptionHandler
//    protected ResponseEntity<ErrorResponseDto> handleBadCredentialException(BadCredentialsException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(UNAUTHORIZED);
//        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//    }
//
//    @ExceptionHandler
//    protected ResponseEntity<ErrorResponseDto> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        ErrorResponseDto response;
//        if (e.getMessage().contains("AttributeConverter")) {
//            response =  ErrorResponseDto.of(CONVERTED_FAIL);
//            log.error("message: " + CONVERTED_FAIL.getMessage());
//        } else {
//            response = ErrorResponseDto.of(INTERNAL_AUTHENTICATION);
//            log.error("message: " + e.getMessage());
//        }
//        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//    @ExceptionHandler
//    protected ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(e);
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler
//    protected ResponseEntity<ErrorResponseDto> handleSignatureException(SignatureException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(INVALID_TOKEN);
//        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//    }
//
//    @ExceptionHandler
//    public ResponseEntity<ErrorResponseDto> handleConstraintViolationException(ConstraintViolationException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(INVALID_INPUT_VALUE, e.getConstraintViolations());
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler
//    public ResponseEntity<ErrorResponseDto> handleMalformedJwtException(MalformedJwtException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(INVALID_TOKEN);
//        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//    }
//
//    @ExceptionHandler
//    public ResponseEntity<ErrorResponseDto> handleExpiredJwtException(ExpiredJwtException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(EXPIRED_TOKEN);
//        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//    }
//    @ExceptionHandler
//    public ResponseEntity<ErrorResponseDto> handleUnsupportedJwtException(UnsupportedJwtException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//        final ErrorResponseDto response = ErrorResponseDto.of(UNSUPPORTED_TOKEN);
//        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//    }

// 토큰 검증 오류 끝
    //    404예외 처리 핸들러

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentExceptionJwtException(IllegalArgumentException e) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(ILLEGAL_ARGUMENT);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("X-Error-Detail", e.getMessage()) // ✅ 상세 메시지
                .body(response);
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler(AuthenticationException.class)
//    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
//        log.error("🔐 AuthenticationException 발생: {}", e.getMessage());
//        ErrorResponseDto response = ErrorResponseDto.of(ErrorCode.UNAUTHORIZED);
//        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
//    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentExceptionJwtException(AccessDeniedException e) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(INVALID_AUTHENTICATION);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler //무결성 제약조건 위반 오류
    public ResponseEntity<ErrorResponseDto> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException e) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(DATA_INTEGRITY_CONSTRAINT_VIOLATION);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

//    @ExceptionHandler // yaml 파일에 정의한 파일 size보다 큰 경우 예외처리
//    public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
//        log.error("exception: " + e.getClass().getSimpleName());
//        log.error("message: " + e.getMessage());
//
//        final ErrorResponseDto response = ErrorResponseDto.of(ErrorCode.MAX_UPLOAD_SIZE_EXCEED, e.getClass().getSimpleName(), maxFileSize, "최대 파일 사이즈 " + maxFileSize);
//        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
//    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());

        final ErrorResponseDto response = ErrorResponseDto.of(ErrorCode.MAX_UPLOAD_SIZE_EXCEED, e.getClass().getSimpleName(), maxFileSize, "최대 파일 사이즈 " + maxFileSize);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    // 그 밖에 발생하는 모든 예외처리가 이곳으로 모인다.
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(INTERNAL_SERVER_ERROR, e.getClass().getSimpleName(), "알수 없는 서버 에러발생");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 엑셀 대량 입고 업로드 파싱 실패: 사유 메시지를 본문(message)에 담아 400 반환
    @ExceptionHandler
    protected ResponseEntity<ErrorResponseDto> handleExcelParseException(ExcelParseException e) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorResponseDto response = ErrorResponseDto.of(INVALID_INPUT_VALUE, e);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 비즈니스 요구사항에 따른 Exception
    @ExceptionHandler
    protected ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException e) {
        log.error("exception: " + e.getClass().getSimpleName());
        log.error("message: " + e.getMessage());
        final ErrorCode errorCode = e.getErrorCode();
        final ErrorResponseDto response = ErrorResponseDto.of(errorCode, e.getErrors());
        return new ResponseEntity<>(response, HttpStatus.valueOf(errorCode.getStatus()));
    }

    /**
     * 보다 범용적으로 스프링 데이터 접근 계열 예외를 잡고 싶으면 DataAccessException을 사용
     */
    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<ErrorResponseDto> handleDataAccessException(DataAccessException e) {
        log.error("exception: {}", e.getClass().getSimpleName());
        log.error("message: {}", e.getMessage());

        final ErrorResponseDto response = ErrorResponseDto.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
