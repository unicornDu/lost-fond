package com.shiguang.lostfound.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理。
 * 审查 2.24②：500 兜底不再回传 e.getMessage()（防泄露 SQL/类名/堆栈），统一"服务器繁忙"，详情仅 log。
 * 审查 2.28③：新增 DataIntegrityViolation（超长/违约束）与 HttpMessageNotReadable（坏 JSON）→ 友好 400。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof BindException be) {
            FieldError fe = be.getBindingResult().getFieldError();
            if (fe != null) msg = fe.getDefaultMessage();
        } else if (e instanceof MethodArgumentNotValidException me) {
            FieldError fe = me.getBindingResult().getFieldError();
            if (fe != null) msg = fe.getDefaultMessage();
        }
        return Result.fail(400, msg);
    }

    /** 审查 2.28③：字段超长/违反约束 → 友好 400，不暴露 SQL。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleIntegrity(DataIntegrityViolationException e) {
        log.warn("数据约束违反: {}", e.getMessage());
        return Result.fail(400, "提交内容超出字段长度或违反数据约束，请修改后重试");
    }

    /** 审查 2.28③：请求体 JSON 损坏 → 友好 400。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(400, "请求体格式不正确");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public Result<Void> handleParam(Exception e) {
        return Result.fail(400, "请求参数不正确");
    }

    /** 请求 Content-Type 与端点不匹配（如向 multipart 端点发 urlencoded）→ 400 而非 500。 */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaType(org.springframework.web.HttpMediaTypeNotSupportedException e) {
        return Result.fail(400, "请求内容类型不正确（该接口需要 multipart/form-data）");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(413, "图片过大，单张请控制在 5MB 以内");
    }

    /** 审查 2.24②：兜底不泄露内部信息。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(500, "服务器繁忙，请稍后重试");
    }
}
