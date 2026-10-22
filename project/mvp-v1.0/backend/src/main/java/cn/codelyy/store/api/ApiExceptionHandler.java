package cn.codelyy.store.api;

import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of("message", exception.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(FieldError::getDefaultMessage).orElse("请求参数无效");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public org.springframework.http.ResponseEntity<java.util.Map<String,String>> duplicate(org.springframework.dao.DataIntegrityViolationException e){
        return org.springframework.http.ResponseEntity.status(409).body(java.util.Map.of("message","记录重复或关联数据正在使用，请刷新后重试"));
    }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,jakarta.validation.ConstraintViolationException.class, org.springframework.web.method.annotation.HandlerMethodValidationException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public org.springframework.http.ResponseEntity<java.util.Map<String,String>> invalid(Exception e){
        return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message","请求参数不合法，请检查页码、数量和编号"));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String,String>> missing(org.springframework.web.servlet.resource.NoResourceFoundException e){
        return ResponseEntity.status(404).body(Map.of("message","接口或资源不存在"));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "服务器暂时无法处理请求"));
    }
}
