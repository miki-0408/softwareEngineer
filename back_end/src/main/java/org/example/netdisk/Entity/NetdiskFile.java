package org.example.netdisk.Entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data // Lombok自动生成getter/setter等
public class NetdiskFile {
    private Long fileId; // 文件唯一ID（自增主键）
    private String fileName; // 文件名
    private String fileType; // 文件类型（扩展名）
    private Long fileSize; // 文件大小（字节）
    private String path; // 文件在服务器上的存储路径
    private LocalDateTime uploadTime; // 上传时间
    private Long userId; // 所属用户ID
    private Long dirId; // 所在目录ID
    private Integer status; // 文件状态：0=正常，1=回收站
    private Integer isEncrypted; // 加密状态：0=未加密，1=已加密（在私密空间中）
    private Integer compressMethod; // 压缩方式编号
}
