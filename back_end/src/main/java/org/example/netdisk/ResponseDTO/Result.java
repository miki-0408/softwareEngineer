package org.example.netdisk.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // Lombok生成无参构造器
@AllArgsConstructor // Lombok生成全参构造器
@Data // Lombok自动生成getter/setter等
public class Result<T> { // 统一响应结果封装：所有API接口返回此格式
    private Integer code; // 业务状态码：0=成功，1=失败，2=文件名冲突
    private String message; // 提示信息
    private T data; // 响应数据，泛型支持任意类型

    public static <E> Result<E> success(E data) { // 快速构建成功响应（带数据）
        return new Result<>(0, "操作成功", data);
    }

    public static Result success() { // 快速构建成功响应（无数据）
        return new Result(0, "操作成功", null);
    }
    public static Result error(String message) { // 快速构建失败响应
        return new Result(1, message, null);
    }

    public static Result<String> conflict(String conflictName, String message) { // 构建冲突响应（code=2），data存冲突文件名供前端弹窗
        return new Result<>(2, message, conflictName);
    }
}
