package org.example.netdisk.Config;

import org.example.netdisk.ResponseDTO.Result;
import org.example.netdisk.Service.Support.FileConflictException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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
}
