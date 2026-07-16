package org.example.netdisk.ResponseDTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok自动生成getter/setter等
public class R_File { // 文件响应DTO：返回给前端的文件信息
    private String fileId; // 文件ID（字符串格式）
    private String fileName; // 文件名
    private String fileType; // 文件类型（扩展名）
    private Long fileSize; // 文件大小（字节）
    private LocalDateTime uploadTime; // 上传时间
    private String dirId; // 所在目录ID
    private Integer status; // 文件状态：0=正常，1=回收站
    private Integer isEncrypted; // 加密状态：0=未加密，1=已加密
    private Integer compressMethod; // 压缩方式编号
}
