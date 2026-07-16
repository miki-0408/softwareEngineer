package org.example.netdisk.Service.Impl;

import lombok.extern.slf4j.Slf4j;
import org.example.netdisk.Entity.*;
import org.example.netdisk.Mapper.*;
import org.example.netdisk.ResponseDTO.R_PrivateSpace;
import org.example.netdisk.ResponseDTO.R_VerifyPrivateSpaceDTO;
import org.example.netdisk.Service.Inter.PrivateSpaceService;
import org.example.netdisk.Service.Support.TransformService;
import org.example.netdisk.Utils.BcryptUtil;
import org.example.netdisk.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.example.netdisk.Service.Support.Enum.*;

@Slf4j
@Service
public class PrivateSpaceServiceImpl implements PrivateSpaceService {

    @Autowired
    private PrivateSpaceMapper privateSpaceMapper; // 私密空间表数据库操作接口
    @Autowired
    private UserMapper userMapper; // 用户表数据库操作接口，用于获取用户角色信息生成token
    @Autowired
    private DirectoryMapper directoryMapper; // 目录表数据库操作接口，管理私密空间根目录
    @Autowired
    private FileMapper fileMapper; // 文件表数据库操作接口，检查私密空间是否为空
    @Autowired
    private TransformService transformService; // 实体与响应DTO之间的转换服务

    @Override
    public boolean enablePrivateSpace(Long userId, String password) { // 启用私密空间：验证密码→检查现有状态→插入或更新记录→创建根目录
        if (password == null || password.isBlank()) { // 密码为空或仅含空白字符
            throw new RuntimeException("私密空间密码不能为空"); // 拒绝启用，密码为必填项
        }
        PrivateSpace existing = privateSpaceMapper.selectByUserId(userId); // 查询用户是否已有私密空间记录
        if (existing != null && existing.getIsEncrypted() == privateSpaceEnabled) { // 已存在且已启用
            throw new RuntimeException("私密空间已启用"); // 拒绝重复启用
        }
        PrivateSpace privateSpace = new PrivateSpace(); // 创建私密空间实体对象
        privateSpace.setUserId(userId); // 绑定归属用户
        privateSpace.setPassword(BcryptUtil.hash(password)); // 使用BCrypt加密存储密码，不存明文
        privateSpace.setIsEncrypted(privateSpaceEnabled); // 设置状态为已启用
        if (existing == null) { // 首次启用，无现有记录
            privateSpaceMapper.insertPrivateSpace(privateSpace); // 插入新的私密空间记录
        } else { // 之前已禁用，仅需更新
            privateSpaceMapper.updatePrivateSpace(privateSpace); // 更新现有记录的状态和密码
        }
        Directory existingRoot = findPrivateSpaceRoot(userId); // 查找是否已有私密空间根目录
        if (existingRoot == null) { // 根目录不存在，需要创建
            Directory privateRoot = new Directory(); // 创建私密空间根目录实体
            privateRoot.setDirName(privateSpaceRootDirName); // 设置根目录名称（取值自枚举常量）
            privateRoot.setParentDirId(null); // 根目录的父目录ID为null
            privateRoot.setUserId(userId); // 绑定归属用户
            directoryMapper.insertDirectory(privateRoot); // 插入根目录记录到数据库
        }
        return true; // 启用成功
    }

    @Override
    public boolean disablePrivateSpace(Long userId, String password) { // 关闭私密空间：验证密码→检查目录为空→删除根目录→更新状态
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId); // 查询用户私密空间记录
        if (privateSpace == null || privateSpace.getIsEncrypted() != privateSpaceEnabled) { // 记录不存在或已处于禁用状态
            return false; // 无需操作，返回失败
        }
        if (!BcryptUtil.matches(password, privateSpace.getPassword())) { // 用BCrypt验证用户输入的密码
            return false; // 密码不匹配，拒绝关闭
        }
        Directory privateRoot = findPrivateSpaceRoot(userId); // 查找用户的私密空间根目录
        if (privateRoot != null) { // 根目录存在才需要检查并删除
            int dirCount = directoryMapper.selectDirectoriesByParentId(userId, privateRoot.getDirId()).size(); // 统计根目录下的子目录数量
            int fileCount = fileMapper.countPrivateFilesInDirectory(userId, privateRoot.getDirId()); // 统计根目录下的私密文件数量
            if (dirCount > 0 || fileCount > 0) { // 目录非空（有子目录或文件）
                throw new RuntimeException("私密空间中仍有文件，请先将所有文件移出后再关闭"); // 拒绝关闭，防止文件丢失
            }
            directoryMapper.deleteDirectory(privateRoot.getDirId(), userId); // 根目录为空，安全删除
        }
        privateSpace.setIsEncrypted(privateSpaceDisabled); // 更新状态为已禁用
        privateSpaceMapper.updatePrivateSpace(privateSpace); // 将状态变更写入数据库
        return true; // 关闭成功
    }

    @Override
    public R_VerifyPrivateSpaceDTO verifyPrivateSpace(Long userId, String password) { // 验证私密空间密码：查记录→比对密码→生成临时token
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId); // 查询用户私密空间记录
        if (privateSpace == null || privateSpace.getIsEncrypted() != privateSpaceEnabled) { // 记录不存在或未启用
            return null; // 无需验证
        }
        if (!BcryptUtil.matches(password, privateSpace.getPassword())) { // BCrypt密码不匹配
            R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO(); // 创建验证结果DTO
            dto.setVerified(false); // 设置验证失败标志
            return dto; // 返回验证失败结果
        }
        User user = userMapper.selectUserById(userId); // 密码正确，查询用户信息用于生成token
        String token = JwtUtil.genToken( // 生成JWT临时令牌
            new Claims(String.valueOf(userId), user.getRole()).toMap() // 将userId和角色封装为Claims并转为Map
        );
        R_VerifyPrivateSpaceDTO dto = new R_VerifyPrivateSpaceDTO(); // 创建验证结果DTO
        dto.setVerified(true); // 设置验证成功标志
        dto.setTempToken(token); // 将生成的临时令牌放入DTO
        return dto; // 返回验证成功结果（含临时令牌）
    }

    @Override
    public R_PrivateSpace getPrivateSpaceStatus(Long userId) { // 获取私密空间状态：查记录→转换→补全根目录→设置根目录ID
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId); // 查询用户私密空间记录
        R_PrivateSpace result = transformService.transformPrivateSpaceToRPrivateSpace(privateSpace); // 将实体转为响应DTO
        Directory privateRoot = findPrivateSpaceRoot(userId); // 查找用户的私密空间根目录
        if (privateRoot == null && privateSpace != null && privateSpace.getIsEncrypted() == privateSpaceEnabled) { // 私密空间已启用但根目录丢失（数据修复场景）
            privateRoot = new Directory(); // 创建新的根目录实体
            privateRoot.setDirName(privateSpaceRootDirName); // 设置根目录名称
            privateRoot.setParentDirId(null); // 根目录父ID为null
            privateRoot.setUserId(userId); // 绑定归属用户
            directoryMapper.insertDirectory(privateRoot); // 补插入根目录记录到数据库
        }
        if (privateRoot != null) { // 根目录存在
            result.setRootDirId(String.valueOf(privateRoot.getDirId())); // 将根目录ID放入响应DTO供前端使用
        }
        return result; // 返回私密空间状态
    }

    public void validatePrivatePassword(Long userId, String privatePassword) { // 校验用户私密空间密码的公共方法，供其他服务调用
        PrivateSpace privateSpace = privateSpaceMapper.selectByUserId(userId); // 查询用户私密空间记录
        if (privateSpace == null || privateSpace.getIsEncrypted() != privateSpaceEnabled) { // 记录不存在或未启用
            throw new RuntimeException("请先启用私密空间"); // 抛出异常提示用户启用
        }
        if (privatePassword == null || !BcryptUtil.matches(privatePassword, privateSpace.getPassword())) { // 密码为空或BCrypt不匹配
            throw new RuntimeException("私密空间密码错误"); // 抛出异常提示密码错误
        }
    }

    public Directory findPrivateSpaceRoot(Long userId) { // 在用户的根目录列表中查找名称匹配的私密空间根目录
        java.util.List<Directory> roots = directoryMapper.selectRootDirectories(userId); // 查询用户所有的根级目录
        return roots.stream()
            .filter(d -> privateSpaceRootDirName.equals(d.getDirName())) // 过滤出目录名等于私密空间根目录名的目录
            .findFirst().orElse(null); // 返回第一个匹配的目录，无匹配则返回null
    }
}
