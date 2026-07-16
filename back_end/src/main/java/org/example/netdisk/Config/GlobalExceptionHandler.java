package org.example.netdisk.Config;

import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Support.FileConflictException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice // 全局异常处理器，拦截所有Controller抛出的异常并返回统一JSON格式
public class GlobalExceptionHandler {

    @ExceptionHandler(FileConflictException.class) // 捕获文件重名冲突异常
    public Result<String> handleConflict(FileConflictException e) {
        return Result.conflict(e.getConflictName(), e.getMessage()); // 返回409冲突状态及冲突文件名，让前端提示用户选择覆盖或重命名
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class) // 捕获请求参数类型不匹配异常（如传入字符串而非数字）
    public Result<String> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String name = e.getName(); // 获取出错的参数名，定位是哪个字段格式错误
        return Result.error("参数 " + name + " 格式不正确，请输入有效的数值"); // 返回中文错误提示，帮助用户修正输入
    }

    @ExceptionHandler(MissingServletRequestParameterException.class) // 捕获缺少必填请求参数的异常
    public Result<String> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数: " + e.getParameterName()); // 明确告知前端缺少哪个参数，方便调试和提示用户
    }

    @ExceptionHandler(RuntimeException.class) // 兜底捕获所有未处理的运行时异常，避免直接暴露500错误堆栈给前端
    public Result<String> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage()); // 提取异常消息作为用户可读的错误提示返回
    }
}
