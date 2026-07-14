package org.example.netdisk.Config;

import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Support.FileConflictException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        long maxSize = e.getMaxUploadSize();
        String sizeStr = maxSize >= 1048576
            ? (maxSize / 1048576) + "MB"
            : (maxSize / 1024) + "KB";
        return Result.error("上传文件大小不能超过 " + sizeStr);
    }

    @ExceptionHandler(FileConflictException.class)
    public Result<String> handleConflict(FileConflictException e) {
        return Result.conflict(e.getConflictName(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String name = e.getName();
        return Result.error("参数 " + name + " 格式不正确，请输入有效的数值");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<String> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
