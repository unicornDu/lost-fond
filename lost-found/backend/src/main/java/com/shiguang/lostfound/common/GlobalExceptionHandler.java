package com.shiguang.lostfound.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理。
 * 审查 2.24②：500 兜底不再回传 e.getMessage()（防泄露 SQL/类名/堆栈），统一"服务器繁忙"，详情仅 log。
 * 审查 2.28③：新增 DataIntegrityViolation（超长/违约束）与 HttpMessageNotReadable（坏 JSON）→ 友好 400。
 * 修复 B-06：① 补 NoResourceFound（未知路径）→404 与 HttpRequestMethodNotSupported（方法不匹配）→405 专项 handler，
 *   不再落进 500 兜底；② 所有错误响应的 HTTP 状态码与 body.code 一致（400→400、401→401、403→403、404→404、413→413、500→500），
 *   前端 axios 错误分支读 err.response.data.msg 且 401 用双条件判断，已确认兼容；test_api 只读 body.code，不受影响。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** HTTP 状态与业务 code 对齐；非法区间回落 500。 */
    private ResponseEntity<Result<Void>> resp(int code, String msg) {
        int http = (code >= 400 && code <= 599) ? code : 500;
        return ResponseEntity.status(http).body(Result.fail(code, msg));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return resp(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof BindException be) {
            FieldError fe = be.getBindingResult().getFieldError();
            if (fe != null) msg = fe.getDefaultMessage();
        } else if (e instanceof MethodArgumentNotValidException me) {
            FieldError fe = me.getBindingResult().getFieldError();
            if (fe != null) msg = fe.getDefaultMessage();
        }
        return resp(400, msg);
    }

    /** 审查 2.28③：字段超长/违反约束 → 友好 400，不暴露 SQL。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("数据约束违反: {}", e.getMessage());
        return resp(400, "提交内容超出字段长度或违反数据约束，请修改后重试");
    }

    /** 审查 2.28③：请求体 JSON 损坏 → 友好 400。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return resp(400, "请求体格式不正确");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> handleParam(Exception e) {
        return resp(400, "请求参数不正确");
    }

    /** 请求 Content-Type 与端点不匹配（如向 multipart 端点发 urlencoded）→ 400 而非 500。 */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaType(org.springframework.web.HttpMediaTypeNotSupportedException e) {
        return resp(400, "请求内容类型不正确（该接口需要 multipart/form-data）");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadSize(MaxUploadSizeExceededException e) {
        return resp(413, "图片过大，单张请控制在 5MB 以内");
    }

    /** 修复 B-06：未知路径（Spring Boot 3.2+ 默认抛 NoResourceFoundException）→ 404 而非 500。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return resp(404, "接口不存在");
    }

    /** 修复 B-06：HTTP 方法不匹配（如 POST 只读端点）→ 405 而非 500。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return resp(405, "请求方法不被支持：" + e.getMethod());
    }

    /** 审查 2.24②：兜底不泄露内部信息。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return resp(500, "服务器繁忙，请稍后重试");
    }
}
