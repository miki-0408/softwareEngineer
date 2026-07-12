# Netdisk 云盘系统 — PPT 制作素材文档

> 使用说明：本文档按照 **17 页幻灯片** 的结构编写。每页包含「标题」「要点」「图示描述」三个部分。任何能读取 Markdown 并生成 PPT 的 AI Agent，可按页号顺序、提取每页的内容制作幻灯片。图示描述采用纯文字形式，方便 Agent 转化为 SmartArt / 流程图 / 架构图。

---

## 第 1 页：封面

**标题**：Netdisk 云盘系统 — 全栈设计与实现

**副标题**：
- 基于 Spring Boot + Vue 3 的企业级云存储解决方案
- 涵盖手动压缩/加密算法、私密空间、传输队列、性能测试

**制作者 / 日期**：（填写）

---

## 第 2 页：项目概述

**标题**：项目概述

**要点**：
- **项目类型**：全栈 Web 应用（B/S 架构）
- **项目定位**：企业级云存储系统，支持文件管理、加密存储、回收站、管理员后台
- **核心数据**：
  - 后端：60 个 Java 源文件，6 张数据库表，约 4000 行业务代码
  - 前端：19 个 Vue/JS 源文件，6 个页面视图，4 个公共组件
  - 迭代：从 v8.0 到 v14.1，共计 24+ 次版本迭代
- **核心特色**：
  - 手动实现 LZ77 / Huffman / XOR 流密码（不调库）
  - 独立私密空间（加密保险箱）
  - XHR 进度追踪传输队列
  - 交互式文件名冲突处理
  - JMeter 性能测试（2210+1000 次请求，0 错误）

**图示描述**：
- 一个六边形雷达图，六个顶点分别标注：文件管理 / 加密安全 / 压缩算法 / 私密空间 / 传输队列 / 性能优化

---

## 第 3 页：技术栈

**标题**：技术栈概览

**要点**：

| 层级 | 技术 | 版本 / 说明 |
|---|---|---|
| 后端框架 | Spring Boot | 4.1.0 |
| Java | JDK | 17 |
| ORM | MyBatis | 注解式（无 XML） |
| 数据库 | MySQL | 8.0.37 |
| 认证 | JWT (jjwt) | HMAC-SHA256 签名 |
| 密码哈希 | Bcrypt | jBCrypt 库 |
| 前端框架 | Vue 3 | Composition API (`<script setup>`) |
| 构建工具 | Vite | 5.4 |
| UI 库 | Element Plus | 中文国际化 |
| 状态管理 | Pinia | Composition Store |
| 路由 | Vue Router | 4.x (Hash 模式) |
| HTTP 客户端 | Axios | 拦截器统一处理 Token/错误 |
| 图标 | Element Plus Icons | 全局注册 |
| 测试工具 | JMeter | 5.6.3 |

**图示描述**：
```
┌──────────────┐          ┌──────────────┐
│   Vue 3      │  HTTP    │ Spring Boot  │
│ + Element Plus│<───────>│ + MyBatis    │
│ + Pinia      │  JSON    │ + JWT        │
│  (Vite 构建)  │          │  (Maven 构建) │
└──────────────┘          └──────┬───────┘
                                 │ JDBC
                          ┌──────┴───────┐
                          │    MySQL      │
                          │  8.0.37      │
                          └──────────────┘
```

---

## 第 4 页：系统架构

**标题**：系统架构设计

**要点**：

**后端分层架构**：
- Controller 层 — 8 个控制器，处理 HTTP 请求 / 参数校验 / 响应封装
- Service 层 — 8 个接口 + 8 个实现，承载业务逻辑
- Mapper 层 — 6 个 MyBatis 接口，注解式 SQL
- Entity 层 — 6 个实体类，对应数据库表
- Utils 层 — 7 个工具类（JWT / 加密 / 压缩 / 归档 / 通用）
- Config 层 — WebConfig（拦截器注册）+ GlobalExceptionHandler

**前端分层架构**：
- Views — 6 个页面视图，懒加载路由
- Components — 4 个公共组件（Sidebar / 上传对话框 / 目录选择器 / 传输卡片）
- Stores — 2 个 Pinia Store（用户状态 / 传输队列）
- Composables — 2 个共享逻辑（文件浏览器 / 私密密码弹窗）
- API — 1 个 Axios 封装（30+ 接口，拦截器，冲突处理）
- Utils — 统一格式化工具

**关键设计模式**：
- JWT Token 拦截器 — 三层授权（公开 / 用户 / 管理员）
- `@RestControllerAdvice` 全局异常处理
- `Result<T>` 统一响应封装（code=0 成功 / code=2 冲突 / 其他错误）
- `FileConflictException` + 前端 `withConflictRetry()` 交互式冲突处理

**图示描述**：
```
Frontend                    Backend                   Database
───────                     ───────                   ────────
Views ─→ API ─→ Axios ═══ HTTP ═══ Interceptors ─→ Controllers
  │                                                     │
Components                                          Services (Impl)
  │                                                     │
Stores ─→ Composables                               Mappers (MyBatis)
                                                         │
                                                     MySQL (6 Tables)
```

---

## 第 5 页：数据库设计

**标题**：数据库设计（6 表 ER）

**要点**：

| 表名 | 主要字段 | 说明 |
|---|---|---|
| `user` | userId, name, password(hash), sex, avatar, role | 用户表，支持 admin/user 角色 |
| `directory` | dirId, dirName, parentDirId, userId, createTime | 目录表，树形结构（自引用外键） |
| `file` | fileId, fileName, fileSize, path, dirId, userId, isEncrypted, status, compressMethod | 文件表，压缩方法存 DB 列 |
| `storage_space` | userId(PK), totalSpace(10GB), usedSpace, remainSpace | 存储配额表 |
| `private_space` | userId(PK), password(hash), isEncrypted | 私密空间表 |
| `log` | logId, operatorId, description, time | 操作日志表 |

**关键设计**：
- `file.dirId` → `directory.dirId`（外键，目录删除时 SET NULL）
- `file.status`：0=正常，1=已删除（软删除到回收站）
- `file.isEncrypted`：0=普通，1=已加密（私密空间内）
- `file.compressMethod`：1=LZ77，2=Huffman（替代早期 NDKK header）
- 每用户默认 10GB 空间

**图示描述**：
```
 user ──1:N── directory ──1:N── file
  │                                     file.status: 0=正常/1=回收站
  1:1                                   file.isEncrypted: 0/1
  ├── storage_space                    file.compressMethod: 1=LZ77/2=Huffman
  ├── private_space
  └── log (N:1 by operatorId)
```

---

## 第 6 页：后端核心 — 文件处理流水线

**标题**：文件处理流水线 (Processing Pipeline)

**要点**：

**上传流水线**：`打包 → 压缩 → 加密`
```
原始文件字节
  │
  ├─ [可选] TarUtil.createTar()     ← 文件夹上传时打包为 POSIX ustar
  │
  ├─ LZ77Compression.compress()     ← 默认算法(滑动窗口 4KB / 前瞻 16B)
  │  或 HuffmanCompression.compress() ← 备选算法(频率统计+最小堆建树)
  │
  └─ [可选] EncryptionUtil.encrypt() ← XOR 流密码(随机盐 + SHA-256 密钥派生)
     └→ 存入磁盘: uploads/{userId}/files/{fileId}.dat
```

**下载流水线**：`解密 → 解压`
```
磁盘文件字节
  │
  ├─ [可选] EncryptionUtil.decrypt() ← 根据 isEncrypted 标志自动判断
  │
  └─ LZ77/Huffman decompress()      ← 根据 compressMethod 列自动选择
     └→ 返回原始文件字节流
```

**加密/解密流水线**（移入/移出私密空间）：
```
读磁盘 → processDownload(解密+解压) → processUpload(压缩+加密) → 写磁盘
```
- 通过 `recompressFile()` 公用方法实现，`encryptFile` 和 `decryptFile` 各为 1 行委托

**设计要点**：
- 压缩方法记录在 `compressMethod` 列（DB 列而非 header），压缩后存入磁盘
- 最外层加密，确保私密空间文件存储时已加密且已压缩

**图示描述**：
```
      上传路径                         下载路径
  ┌──────────────┐              ┌──────────────┐
  │  原始数据     │              │  存储数据     │
  └──────┬───────┘              └──────┬───────┘
         ↓                             ↓
  ┌──────────────┐              ┌──────────────┐
  │  TAR 打包     │              │  解密(XOR)   │
  └──────┬───────┘              └──────┬───────┘
         ↓                             ↓
  ┌──────────────┐              ┌──────────────┐
  │ LZ77/Huffman │              │ LZ77/Huffman │
  │   压缩        │              │   解压        │
  └──────┬───────┘              └──────┬───────┘
         ↓                             ↓
  ┌──────────────┐              ┌──────────────┐
  │  加密(XOR)    │              │  原始数据     │
  └──────┬───────┘              └──────────────┘
         ↓
  ┌──────────────┐
  │  存储磁盘     │
  └──────────────┘
```

---

## 第 7 页：手动算法实现 — LZ77 压缩

**标题**：LZ77 滑动窗口压缩算法

**要点**：
- **算法原理**：在滑动窗口中找到与当前字节串的最长匹配，用 `(distance, length)` 引用替代重复数据
- **参数配置**：窗口 4096 字节 / 前瞻 16 字节 / 最小匹配 3 字节
- **Token 格式**：
  - `0x00 + 1B data` — 字面量模式（无匹配）
  - `0x01 + 2B distance(LE) + 1B length` — 匹配模式
  - `0xFF` — 结束标记
- **解压优化**：使用动态扩容 `byte[]` 缓冲区替代 `ByteArrayOutputStream.toByteArray()`，避免每次 match 都复制整个数组
- **代码量**：约 210 行（含 findLongestMatch / compress / decompress / grow）

**压缩率示例**：
| 数据类型 | 原始大小 | 压缩后 | 压缩率 |
|---|---|---|---|
| 重复英文文本 | 290 B | 110 B | 38% |
| 重复中英混合 | 185 B | 80 B | 43% |

**图示描述**：
```
滑动窗口 (4096B)         前瞻缓冲区 (16B)
┌──────────────────────┬──────────────┐
│ 已编码区域(搜索字典)  │ 待编码区域    │
│ ← ← ← 扫描方向       │ ↑ 当前位置   │
└──────────────────────┴──────────────┘

匹配成功 → 输出 (0x01, distance, length)
无匹配   → 输出 (0x00, literal_byte)
```

---

## 第 8 页：手动算法实现 — Huffman + XOR + TAR

**标题**：Huffman 编码 / XOR 流密码 / TAR 归档

**要点**：

**Huffman 压缩**：
- 原理：字节频率统计 → 最小堆构建最优二叉树 → 变长编码（高频短码）
- 输出格式：`[4B 原始长度] [256×4B 频率表] [4B 编码位长度] [变长比特流]`
- 优点：无损压缩，对文本文件效果好

**XOR 流密码加密**：
- 原理：SHA-256(密码 + 随机8字节盐 + 计数器) → 派生密钥流 → 逐字节 XOR
- 输出格式：`[8B 随机盐] [密文]`
- 安全特性：同一密码加密不同文件 → 不同盐值 → 不同密钥流（防重放攻击）
- 加解密共用同一算法（XOR 对称性）

**TAR 归档**：
- 实现：POSIX ustar 标准，512 字节固定头部 + 数据块 + 填充
- 用途：文件夹上传时打包（web-kit-directory 多文件）
- 支持：文件名、模式、大小、时间戳、校验和

**图示描述**：
```
Huffman:  字节频率表 → 哈夫曼树 → 编码表 → 压缩比特流
XOR:      密码 → SHA-256(密码+salt+counter) → 密钥流 ⊕ 明文 = 密文
TAR:      多文件 → [512B header][data][padding] × N → 单一归档文件
```

---

## 第 9 页：认证与安全

**标题**：认证授权体系

**要点**：

**三层拦截器架构**：
```
请求 → JwtInterceptor(公开路由) → JwtUserInterceptor(用户路由) → JwtSysAdminInterceptor(管理员路由) → Controller
```

| 拦截器 | 拦截路径 | 权限 |
|---|---|---|
| JwtInterceptor | `/user/**`, `/systemAdmin/**` | 仅验证 Token 合法性 |
| JwtUserInterceptor | `/user/**` | Token 有效 + role 为 user |
| JwtSysAdminInterceptor | `/systemAdmin/**` | Token 有效 + role 为 admin |

**JWT 设计**：
- 签名算法：HMAC-SHA256
- Payload：`{ userId: "12", role: "user" }`
- 过期时间：硬编码过期策略
- 前端存储：`localStorage`，每次请求通过 Axios 拦截器自动附加 `Authorization: Bearer <token>`

**密码安全**：
- 存储：Bcrypt 哈希（`$2a$10$...`），不存储明文
- 验证：`BcryptUtil.matches(明文, 哈希)`
- 私密密码同样使用 Bcrypt 哈希存储

**前端路由守卫**：
- `router.beforeEach`：检查 `localStorage.token`、`requiresAdmin` 角色检查

**图示描述**：
```
 浏览器请求
     │  Authorization: Bearer <jwt_token>
     ↓
┌────────────┐    ┌────────────┐    ┌────────────────┐
│JwtIntercept│───→│JwtUserInter│───→│JwtSysAdminInter│
│(验证Token) │    │(用户权限)   │    │(管理员权限)     │
└────────────┘    └────────────┘    └────────────────┘
     ↓                  ↓                  ↓
  公开API           用户API            管理员API
 (login/register)  (file/dir/...)    (logs/user mgmt)
```

---

## 第 10 页：私密空间设计

**标题**：私密空间 — 加密保险箱

**要点**：

**设计理念**：
- 私密空间是独立的加密文件容器，拥有独立的目录树
- 通过数据库 `private_space` 表管理启用/关闭状态
- 私密密码使用 Bcrypt 哈希，同一用户只保存一份

**核心流程**：

| 操作 | 流程 |
|---|---|
| **启用** | 设置密码 → Bcrypt 哈希 → 创建 `private_space` 记录 + 私密根目录 |
| **验证** | 输入密码 → Bcrypt 匹配 → 返回临时 JWT（含新权限） |
| **移入** | 选文件 → 验证密码 → 解压 → 加密(XOR) → 压缩 → 移动到私密目录 |
| **移出** | 选文件 → 验证密码 → 解压 → 解密(XOR) → 压缩 → 移动到普通目录 |
| **关闭** | 验证密码 → 检查目录为空 → 标记已关闭 |

**安全机制**：
- 密码仅在 `sessionStorage` 中短时缓存，页面关闭即过期
- 移入/移出必须先验证私密密码
- 加密流水线：compress(encrypt(data))，磁盘上存储已压缩已加密的数据

**图示描述**：
```
 普通空间              私密空间(加密保险箱)
 ┌──────────┐         ┌──────────────┐
 │ 文件A    │ 移入→   │ 文件A(加密)  │
 │ 文件B    │ decrypt │ 文件B(加密)  │
 │ 文件C    │ ←移出   │ 文件C(加密)  │
 └──────────┘         └──────────────┘
                            ↑
                     密码验证(Bcrypt)
                     密码缓存(sessionStorage)
```

---

## 第 11 页：传输队列设计

**标题**：传输队列 — XHR 进度追踪

**要点**：

**技术方案**：
- 放弃 Axios 请求（不支持上传进度），改用原生 `XMLHttpRequest`
- `xhr.upload.onprogress` 追踪上传进度
- `xhr.onprogress` 追踪下载进度
- 最大并发数：2（`MAX_CONCURRENT`）

**任务队列架构**：
```
TransferStore (Pinia)
  ├── tasks[]            ← 任务列表（响应式数组）
  ├── enqueue(runner)    ← 并发控制队列
  ├── addUpload()        ← 上传任务入队
  ├── addDownloadWithPicker()  ← 下载任务入队（含 File System Access API）
  └── TransferView.vue   ← 可视化任务卡片（TransferCard 组件）
```

**上传流程**：
```
UploadDialog → emit('confirm') → onUploadConfirm() → 
  构建 FormData → transferStore.addUpload(name, size, builder, onComplete) →
  enqueue → xhr.send(FormData) → onprogress(更新进度) → onload(标记完成)
```

**下载特色**：
- 优先使用 `window.showSaveFilePicker()`（File System Access API），用户选择保存位置
- 降级方案：`Blob URL + <a> download` 传统下载

**图示描述**：
```
 Transfer Page
 ┌──────────────────────────────────┐
 │  Task 1: file.zip  [████████░░] 80% │  ← TransferCard 组件
 │  Task 2: doc.pdf   [██████████] 100% │  ← 进度条实时更新
 │  Task 3: image.png [██░░░░░░░░] 20% │
 └──────────────────────────────────┘
         ↑                    ↑
    xhr.upload.onprogress   xhr.onprogress
    (XHR 原生, 非 Axios)
```

---

## 第 12 页：文件名冲突处理

**标题**：交互式文件名冲突处理

**要点**：

**设计演进**：
- v12：自动加后缀 `(1)(2)` — 用户不满意
- v13+：交互式弹窗 — 用户确认替换或跳过

**技术实现**：

```
后端 resolveConflict(userId, dirId, fileName, force):
  countFilesByName > 0?
    ├─ force=false → throw FileConflictException(conflictName)
    └─ force=true → 删除旧文件（含磁盘+存储配额释放）→ 继续

前端 withConflictRetry(fn, forceFn):
  fn() → 成功 → return true
  fn() → FileConflictException → handleConflict() → 弹窗
    ├─ 用户点替换 → forceFn() → return true
    └─ 用户点跳过 → return false
```

**覆盖范围**：
| 操作 | 冲突处理 |
|---|---|
| 重命名文件 | ✅ |
| 移动文件（单个+批量） | ✅ |
| 移入私密空间（加密） | ✅ |
| 移出私密空间（解密） | ✅ |
| 上传文件 | 自动加后缀（uniqueFileName） |

**GlobalExceptionHandler**：
- `FileConflictException` → code=2，返回冲突文件名
- `RuntimeException` → code≠0，统一 `Result.error()`
- `MaxUploadSizeExceededException` → 文件过大提示

**图示描述**：
```
用户操作(非force) → 后端检查 → 同名存在 → throws FileConflictException
                                                      ↓
                                   前端 Axios 拦截器: code=2 → 构造 conflictError
                                                      ↓
                                   withConflictRetry: catch → ElMessageBox.confirm
                                                      ↓
                                   用户选择                   
                                  ┌────┴────┐                  
                               替换        跳过                  
                          force=true  return false
                             ↓
                     后端删除旧文件
                     → 操作成功
```

---

## 第 13 页：前端架构设计

**标题**：前端架构详细设计

**要点**：

**页面视图(6)**：

| 页面 | 路由 | 对应组件 | 核心功能 |
|---|---|---|---|
| LoginView | `/` | — | 登录/注册双 Tab |
| MainView | `/main` | UploadDialog, DirPickerDialog | 浏览器+操作 |
| PrivateSpaceView | `/private` | UploadDialog, DirPickerDialog | 加密空间浏览器 |
| RecycleBinView | `/recycle` | — | 回收站管理 |
| TransferView | `/transfer` | TransferCard | 传输任务列表 |
| AdminView | `/admin` | — | 密码重置+用户管理+日志 |

**共享组件(4)**：Sidebar、UploadDialog、DirPickerDialog、TransferCard

**Composable(2)**：
- `useFileBrowser`：导航 / 加载 / 筛选 / 列表合成 / 对话框状态（MainView 和 PrivateSpaceView 共用）
- `usePrivatePassword`：密码弹窗 Promise 模式

**Store(2)**：
- `userStore`：用户信息 / 存储空间 / 私密空间状态 / `formatSize`
- `transferStore`：传输任务队列（XHR 最大并发 2 控制）

**API 封装**：
- Axios 实例 + 请求拦截器（JWT Bearer） + 响应拦截器（code≠0 错误 + 冲突检测）
- `post()` / `postForm()` 通用助手 → 30 个 API 方法全部一行声明
- `handleConflict()` / `withConflictRetry()` 冲突处理

**图示描述**：
```
Router (/ → /main → /private → /recycle → /transfer → /admin)
   │
   ├── MainView ───── 共用 ──→ useFileBrowser composable
   ├── PrivateSpaceView ──→ usePrivatePassword composable
   │                              │
   ├── LoginView              formatSize/formatTime (utils/format)
   ├── RecycleBinView              │
   ├── TransferView           transferStore / userStore (Pinia)
   └── AdminView
         │
    Components: Sidebar / UploadDialog / DirPickerDialog / TransferCard
         │
    API (Axios): authAPI / userAPI / directoryAPI / fileAPI / recycleAPI / privateSpaceAPI / adminAPI
```

---

## 第 14 页：前端关键交互

**标题**：前端关键交互设计

**要点**：

**1. 文件夹上传**：
- `<input webkitdirectory>` → 浏览器级文件夹选择
- 前端构建目录树（`buildUploadDirTree`） + 后端 `uploadFiles`（TAR 打包）
- 筛选面板：路径包含 / 类型包含 / 文件名包含 / 大小范围

**2. 目录导航**：
- 手动面包屑栈 `breadcrumb[]`（而非从后端获取完整路径）
- 点击目录进入子目录 → push；点击面包屑级 → slice 还原

**3. 批量操作**：
- `el-table` 的 `@selection-change` → `selectedItems`
- 批量删除：逐个调用 API，容错跳过失败项
- 批量移动：打开 DirPickerDialog → 选择目标 → 逐个移动 + 冲突处理
- 批量加密/解密：筛选文件 → 选择目标目录 + 冲突处理

**4. 文件筛选与排序**：
- `filteredItems` computed：`filterText`（名称包含）+ `filterType`（类型/文件夹）
- `fileTypeOptions` computed：从当前目录文件中动态提取已有类型
- `el-table` 原生 sort：名称、类型、大小、时间列可排序

**5. 下拉菜单操作**：
- `<el-dropdown>` + `MoreFilled` 图标 → 移动/删除/移入私密空间
- `@command` 分发到 `handleFileAction()`

**6. Lazy Loading**：
- 所有路由组件使用 `() => import(...)` 动态导入

**图示描述**：
```
MainView / PrivateSpaceView 核心交互流程:

  Sidebar ──→ 切换页面
     │
  Header: breadcrumb 导航 / 存储空间条
     │
  Toolbar: 新建文件夹 / 上传 / 刷新
     │
  筛选栏: 搜索 + 类型筛选 + 批量按钮(选中时显示)
     │
  el-table: 目录+文件混合列表 (排序/筛选)
     │
  操作列: 下载 / 重命名 / 更多(移动/删除/加密)
     │
  Dialog: 新建/重命名/上传/移动/加密密码/个人信息/修改密码
```

---

## 第 15 页：Git 版本迭代历程

**标题**：开发迭代历程 (v8.0 → v14.1)

**要点**：

| 版本 | 里程碑 |
|---|---|
| v8.0 ~ v8.2 | 后端核心搭建：认证 / CRUD / 回收站 / 私密空间 API |
| v9.0 ~ v9.1 | 文件加密解密逻辑修复 |
| v10.0 | 项目结构重构 |
| v11.0 ~ v11.1 | 压缩/加密算法 Pipeline 优化 |
| v12.0 ~ v12.3 | 前端从零搭建 (Vue 3) / 数据库初始化脚本更新 |
| v13.0 | 私密空间重构为加密保险箱 / 筛选批量操作 / 传输队列 / 静态代码分析 |
| v14.0 ~ v14.1 | 交互式冲突处理 / 代码复用优化 / 性能测试 / 代码清理 |

**开发过程关键决策**：
1. 手动实现压缩/加密算法 → 不使用第三方库（教学目的）
2. 压缩方法从文件头 → 数据库列（更可靠）
3. 加密顺序统一：最外层加密，与上传保持一致
4. 自动加后缀(1)(2) → 交互式替换弹窗（用户体验改进）
5. 提取 Composables → 消除 MainView / PrivateSpaceView 500+ 行重复代码

**图示描述**：
```
v8.0 ──→ v10.0 ──→ v12.0 ──→ v13.0 ──→ v14.1
 后端      重构     前端搭建   功能完善   优化收尾
 骨架     项目结构  Vue3+EP   加密保险箱  代码复用
                   从零起步   传输队列   性能测试
                             批量操作   冲突处理
```

---

## 第 16 页：性能测试

**标题**：JMeter 性能测试

**要点**：

**测试环境**：本地回环 / JDK 21 / Spring Boot 4.1.0 / MySQL 8.0.37 / JMeter 5.6.3

**测试方案**：
- 10 个测试用户，CSV 参数化
- 单接口测试：5 个线程组，2210 次请求
- 混合场景测试：10 并发 × 20 循环，1000 次请求

**测试结果 — 单接口**（2210 req, 0 err）：

| 接口 | 请求 | 平均(ms) | P50(ms) | TPS |
|---|---|---|---|---|
| Login | 1330 | 947 | 1043 | 24.6 |
| File List | 500 | 2 | 1 | 9.3 |
| Upload | 30 | 49 | 56 | 0.6 |
| Recycle List | 300 | 2 | 1 | 5.6 |
| Register | 50 | 54 | 2 | 0.9 |

**测试结果 — 混合场景**（1000 req, 0 err）：

| 操作 | 请求 | 平均(ms) | P50(ms) | 最大(ms) |
|---|---|---|---|---|
| Login | 200 | 278 | 305 | 329 |
| Browse | 200 | 2 | 2 | 3 |
| Upload | 200 | 17 | 16 | 28 |
| Download | 200 | 1 | 1 | 2 |
| Recycle | 200 | 1 | 1 | 2 |

**结论**：
- **零错误**：系统在高并发下运行稳定
- **瓶颈在登录**：JWT 签名 + bcrypt 验证 ~280ms
- **读操作 < 2ms**：数据库性能极佳
- **上传含压缩 ~17ms**：LZ77 + XOR 开销小

**图示描述**：
```
响应时间对比(混合场景):
Login     ████████████████████████████████ 278ms
Upload    ██ 17ms
Browse    █ 2ms
Download  █ 1ms
Recycle   █ 1ms
```

---

## 第 17 页：总结与展望

**标题**：项目总结与展望

**要点**：

**项目成就**：
- 从零搭建了完整的企业级云存储系统（全栈 79 个源文件）
- 手动实现了 LZ77 / Huffman / XOR 流密码 / TAR 归档四种算法
- 构建了独立的加密保险箱（私密空间）
- 实现了 XHR 进度追踪传输队列
- 设计了交互式文件名冲突处理机制
- 通过了 3210 次 JMeter 压力测试（0 错误）
- 完成了代码复用优化（消除 500+ 行重复代码）
- 完成了解耦和清理（24 处 controller try/catch 统一为全局 handler）

**技术亮点**：
- **不依赖第三方压缩/加密库**：自研算法
- **Composition API + Composables**：代码复用
- **JWT 三层拦截器**：精细化权限控制
- **File System Access API**：现代化下载体验
- **全局异常处理**：统一错误响应格式

**可扩展方向**：
- 分片上传 / 断点续传 / 秒传（文件指纹）
- 文件分享（外链 + 过期时间 + 提取码）
- WebSocket 实时通知
- Redis 缓存 Token + 会话管理
- 分布式存储（MinIO / OSS）
- 全文搜索（Elasticsearch）

**图示描述**：
```
          Netdisk 云盘系统
         ┌──────┼──────┐
    文件管理   加密安全   性能优化
         │       │        │
    CRUD操作  LZ77算法   JMeter测试
    批量操作  Huffman    0错误/3210req
    筛选排序  XOR流密码  代码复用
    回收站    私密空间
         │       │        │
         └──────┼──────┘
           未来扩展:
    分享 / 秒传 / Redis / 分布式
```
