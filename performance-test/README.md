# Netdisk 性能测试

## 目录结构

```
performance-test/
├── data/
│   ├── test_users.csv     # 测试用户凭证（10 个用户）
│   ├── sample.txt         # 上传测试用的小文件
│   └── setup.sql          # 测试数据初始化 SQL
├── single-api-test.jmx    # 单接口压测计划
├── mixed-scenario-test.jmx # 混合场景压测计划
├── generate_jmx.py        # JMX 生成脚本（修改后重新生成用）
└── README.md
```

## 前置条件

1. **JMeter 5.5+** 下载: https://jmeter.apache.org/download_jmeter.cgi
   - 解压后 `bin/jmeter.bat` (Windows) 或 `bin/jmeter` (Linux/Mac)
   - 需要 **JDK 17+**（与项目一致）

2. **后端服务已启动**（默认 http://localhost:8080）

3. **初始化测试数据**（见下文）

## 第一步：初始化测试数据

```bash
# 1. 登录 MySQL，创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS netdisk CHARACTER SET utf8mb4;"

# 2. 初始化项目表结构（项目已有的 init.sql）
mysql -u root -p netdisk < back_end/init.sql

# 3. 导入测试用户数据
mysql -u root -p netdisk < performance-test/data/setup.sql
```

setup.sql 会创建 10 个测试用户：
| 用户名 | 密码 | 角色 |
|---|---|---|
| perf_test_1 ~ perf_test_10 | test123456 | user |

> **注意**：setup.sql 中密码的 bcrypt hash 值需要与项目代码一致。如果启动后端后先注册用户也可以，不需要执行 setup.sql。

## 第二步：运行单接口压测

### JMeter GUI 模式（查看测试结构）

```bash
# Windows
jmeter.bat -t performance-test/single-api-test.jmx

# Linux/Mac
jmeter -t performance-test/single-api-test.jmx
```

### JMeter CLI 模式（运行测试）

```bash
# 创建报告目录
mkdir -p performance-test/reports

# 运行单接口压测
jmeter -n -t performance-test/single-api-test.jmx \
  -l performance-test/reports/raw-result.jtl \
  -JBASE_URL=localhost \
  -JdirId=1 \
  -JfileId=1

# 生成 HTML 报告
jmeter -g performance-test/reports/raw-result.jtl \
  -o performance-test/reports/html-report \
  -Jjmeter.reportgenerator.overall_granularity=1000
```

### 单接口测试包含的接口

| 线程组 | 线程数 | 循环 | 总请求 | 测什么 |
|---|---|---|---|---|
| Login | 10 | 50 | 500 | 登录认证性能 |
| File List | 10 | 50 | 500 | 文件列表查询 |
| Upload | 5 | 20 | 100 | 小文件上传（含 LZ77 压缩）|
| Encrypt | 3 | 10 | 30 | 文件加密（压缩+加密流水线）|
| Recycle Bin | 10 | 30 | 300 | 回收站列表 |
| Register | 5 | 10 | 50 | 注册新用户 |

## 第三步：运行混合场景压测

```bash
# 运行混合场景压测
jmeter -n -t performance-test/mixed-scenario-test.jmx \
  -l performance-test/reports/mixed-raw.jtl \
  -JBASE_URL=localhost

# 生成 HTML 报告
jmeter -g performance-test/reports/mixed-raw.jtl \
  -o performance-test/reports/mixed-html-report \
  -Jjmeter.reportgenerator.overall_granularity=1000
```

### 混合场景说明

- **10 个并发用户**，30 秒内逐步启动（ramp-up）
- 每个用户循环 **20 次**
- 每次循环随机执行：浏览文件 / 查看回收站 / 下载文件
- 登录 → Token 提取 → 带 Token 调用 API，完整模拟真实用户操作链

## 第四步：查看报告

生成后打开:
```
performance-test/reports/html-report/index.html
performance-test/reports/mixed-html-report/index.html
```

HTML 报告包含：

| 图表 | 说明 |
|---|---|
| **Statistics** | 平均响应时间、中位数、P90/P99、吞吐量、错误率 |
| **Response Time Percentiles** | 响应时间分布曲线 |
| **Response Time Overview** | 各请求的响应时间概览 |
| **Throughput** | 随时间变化的吞吐量曲线 |
| **Latencies** | 网络延迟分析 |

## 自定义测试参数

### 修改并发量

单接口测试用 GUI 打开 `.jmx` 后直接修改线程组中的 `Number of Threads`。

或命令行传参（仅限于 JMeter 属性 `${__P(threads,10)}` 方式使用——当前 `.jmx` 未做此配置，可以在 GUI 中修改）。

### 修改测试的目录/文件

在单接口测试的 `User Defined Variables` 中修改：
- `dirId` — 文件列表的目录 ID（默认 1）
- `fileId` — 下载/加密的文件 ID（默认 1）
- `targetDirId` — 加密目标目录 ID（默认 2，即私密空间根目录）

### 测试上传大文件

在 `data/` 下创建大文件，然后在 JMeter GUI 中修改 Upload 请求的文件路径：

```bash
# Windows 生成 1MB 测试文件
fsutil file createnew data/1mb.dat 1048576

# Linux/Mac
dd if=/dev/urandom of=data/1mb.dat bs=1024 count=1024
```

## 性能基线参考（单机开发环境）

以下为本地开发环境的典型参考值（实际取决于硬件）：

| 接口 | 平均响应(ms) | 吞吐量(req/s) |
|---|---|---|
| Login | 10-30 | 200-500 |
| File List | 5-20 | 300-600 |
| Upload (小文件) | 50-200 | 10-30 |
| Download | 5-15 | 300-800 |
| Encrypt | 100-500 | 3-15 |
| Recycle List | 5-15 | 300-800 |

## 常见问题

**Q: 上传测试失败，返回 code != 0**
A: 检查后端是否启动；检查 `dirId` 是否对应测试用户的目录；检查文件大小是否超过 10MB。

**Q: 加密测试失败**
A: 私密空间需先启用。已通过 setup.sql 初始化，或先在页面手动启用。加密时需要 `targetDirId` 为私密空间下的目录 ID。

**Q: JMeter GUI 中文乱码**
A: 编辑 `jmeter/bin/jmeter.properties`，取消注释并修改：`sampleresult.default.encoding=UTF-8`

**Q: HTML 报告报 "Error generating the report"**
A: 先确保 `.jtl` 文件非空。指定 `-Jjmeter.reportgenerator.overall_granularity=1000` 避免精度问题。
