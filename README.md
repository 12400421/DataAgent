# DataAgent 项目介绍

DataAgent 是一个面向 CSV 数据集的数据分析工作台。当前版本提供从文件上传、持久化管理、数据预览、质量检查、统计分析、图表展示到 AI 总结的完整基础流程，适合作为本地数据分析与智能分析功能的 MVP。

当前已支持：

- CSV 数据上传、格式校验与文件存储
- 数据集列表、详情、删除和前 100 行预览
- 缺失值、缺失率、唯一值、字段类型和重复行分析
- 数值字段统计与分类字段 Top 10 高频值分析
- 基于 ECharts 的缺失率、数值统计和高频值柱状图
- 基于数据集信息、质量结果和统计结果的 AI 智能分析

## 技术架构

| 层次 | 技术 | 当前用途 |
| --- | --- | --- |
| 前端 | Vue 3、Vite、Vue Router、Axios | 数据集管理、详情展示和 API 调用 |
| 可视化 | Apache ECharts | 缺失率、数值统计、Top Values 柱状图 |
| 后端 | Java 21、Spring Boot、Spring Web MVC | REST API、文件处理和业务编排 |
| 数据访问 | MyBatis、MySQL Connector/J | 数据集元数据和字段信息持久化 |
| CSV 处理 | Apache Commons CSV | UTF-8 CSV 解析、预览、质量与统计分析 |
| AI | OpenAI-compatible Chat Completions API | 将结构化分析结果整理为中文分析报告 |
| 本地模型 | Ollama + Qwen | 本地运行 Qwen 文本模型并提供兼容接口 |

前端开发服务器运行在 `5173`，通过 Vite 将 `/api` 请求代理到 `http://localhost:8081`。后端将数据集元数据保存到 MySQL，将上传的 CSV 文件保存到本地目录，默认路径为 `data/uploads`。

AI 客户端没有把模型名称写死。当前本地使用方式是通过 Ollama 运行 Qwen，并用环境变量指定实际模型标签。

## 项目结构

以下目录树省略了 `.git`、`target`、`node_modules`、`dist` 等版本控制或构建产物：

```text
dataagent/
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── start-dataagent.bat
├── src/
│   ├── main/
│   │   ├── java/com/jiayi/dataagent/
│   │   │   ├── DataagentApplication.java
│   │   │   ├── config/
│   │   │   │   ├── AiModelConfiguration.java
│   │   │   │   └── AiModelSettings.java
│   │   │   ├── controller/
│   │   │   │   ├── DatasetController.java
│   │   │   │   ├── DatasetPersistenceController.java
│   │   │   │   └── AiAnalysisController.java
│   │   │   ├── exception/
│   │   │   │   ├── DatasetException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── mapper/
│   │   │   │   └── DatasetMapper.java
│   │   │   ├── model/
│   │   │   │   ├── DatasetMetadata.java
│   │   │   │   └── AiAnalysisResponse.java
│   │   │   ├── service/
│   │   │   │   ├── DatasetService.java
│   │   │   │   ├── DatasetPersistenceService.java
│   │   │   │   ├── DatasetFileStorage.java
│   │   │   │   ├── AiAnalysisService.java
│   │   │   │   ├── AiModelClient.java
│   │   │   │   └── OpenAiCompatibleAiModelClient.java
│   │   │   └── utils/
│   │   │       ├── CsvUtils.java
│   │   │       ├── CsvQualityAnalyzer.java
│   │   │       └── CsvProfileAnalyzer.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-no-db.properties
│   │       ├── application-mysql.properties
│   │       └── db/mysql/schema.sql
│   └── test/java/com/jiayi/dataagent/
│       ├── config/
│       ├── controller/
│       ├── service/
│       └── utils/
├── frontend/
│   ├── package.json
│   ├── package-lock.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── styles.css
│       ├── api/datasets.js
│       ├── router/index.js
│       ├── views/
│       │   ├── DatasetListView.vue
│       │   └── DatasetDetailView.vue
│       └── components/
│           ├── EChartCanvas.vue
│           └── DatasetVisualizations.vue
└── data/
    └── uploads/                 # 运行时生成，Git 忽略
```

MySQL 初始化脚本会创建 `datasets` 和 `dataset_columns` 两张表。`dataset_columns.dataset_id` 通过外键关联 `datasets.id`。

## 环境要求

| 组件 | 项目要求或版本 | 当前开发环境验证 |
| --- | --- | --- |
| Java | 21 | Temurin OpenJDK 21.0.12.1 LTS |
| Spring Boot | 4.0.8 | 由 `pom.xml` 固定 |
| Maven | 使用 Maven Wrapper | Wrapper 3.9.16 |
| Node.js | `^20.19.0` 或 `>=22.12.0` | Node.js 24.20.0 |
| npm | 11.19.0 | npm 11.19.0 |
| MySQL | 8.0 | MySQL Community Server 8.0.46 |
| Ollama | 支持 OpenAI-compatible API 的本地版本 | Ollama 0.40.2 |
| Qwen | Ollama 中可用的文本生成模型 | 已验证 `qwen3:1.7b`、`qwen3:4b` |

MySQL 和 Ollama 的补丁版本没有在仓库中锁定。上表右侧是当前开发环境的验证版本；其他兼容版本也可以使用。

## 快速启动

### 启动前准备

1. 启动 MySQL，并创建默认数据库：

   ```sql
   CREATE DATABASE dataagent CHARACTER SET utf8mb4;
   ```

   后端启用 `mysql` Profile 后会通过 `schema.sql` 自动创建数据表，但不会自动创建数据库本身。

2. 首次运行前安装前端依赖：

   ```bash
   cd frontend
   npm install
   cd ..
   ```

3. 在 Windows 用户环境变量中配置：

   | 变量 | 是否必需 | 说明 |
   | --- | --- | --- |
   | `DATAAGENT_DB_USERNAME` | 一键启动必需 | MySQL 用户名 |
   | `DATAAGENT_DB_PASSWORD` | 必需 | MySQL 密码，不要写入仓库 |
   | `DATAAGENT_AI_MODEL` | 建议配置 | Ollama 中已安装的 Qwen 模型名，例如当前环境的 `qwen3:4b` |

   修改用户环境变量后，请新开 CMD、PowerShell 或重新启动 IDEA，使新进程获得这些变量。

4. 确保 Ollama 已安装，并至少存在一个文本生成模型。可以通过以下命令查看：

   ```bash
   ollama list
   ```

### Windows 一键启动

双击项目根目录中的：

```text
start-dataagent.bat
```

脚本会按顺序：

1. 检查 Java、npm、curl、Ollama 和数据库环境变量。
2. 检查 Ollama；未运行时在独立 CMD 窗口执行 `ollama serve`。
3. 在独立 CMD 窗口使用 `mysql` Profile 和 `8081` 端口启动 Spring Boot 后端。
4. 在独立 CMD 窗口执行 `npm run dev`，启动 Vue 前端。
5. 等待服务就绪并打开 `http://localhost:5173`。

脚本会检查 `11434`、`8081` 和 `5173` 端口，避免重复启动。它不会启动 MySQL，也不会自动执行 `npm install`。

如需只检查环境和端口而不启动服务，可以执行：

```bat
start-dataagent.bat --check
```

### 手动启动

#### 1. 启动 Ollama

```bash
ollama serve
```

#### 2. 启动后端

完整工作台需要 `mysql` Profile，前端代理要求后端运行在 `8081`。

Windows：

```bat
set "DATAAGENT_DB_USERNAME=root"
set "DATAAGENT_DB_PASSWORD=<你的 MySQL 密码>"
set "DATAAGENT_AI_MODEL=qwen3:4b"
set "DATAAGENT_AI_BASE_URL=http://127.0.0.1:11434/v1"
set "DATAAGENT_AI_API_KEY=<任意非空的本地占位值>"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql -Dspring-boot.run.arguments=--server.port=8081
```

macOS / Linux：

```bash
export DATAAGENT_DB_USERNAME=root
export DATAAGENT_DB_PASSWORD='<你的 MySQL 密码>'
export DATAAGENT_AI_MODEL='qwen3:4b'
export DATAAGENT_AI_BASE_URL='http://127.0.0.1:11434/v1'
export DATAAGENT_AI_API_KEY='<任意非空的本地占位值>'
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql -Dspring-boot.run.arguments=--server.port=8081
```

项目也支持基础命令：

```bash
mvnw spring-boot:run
```

该命令使用默认 `no-db` Profile，只提供不依赖数据库的 `/api/dataset/upload` 和 `/api/dataset/preview`，不包含完整的数据集管理页面所需接口。

#### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问地址：[http://localhost:5173](http://localhost:5173)

后端 API 地址：`http://localhost:8081`

## 配置说明

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DATAAGENT_DB_URL` | `jdbc:mysql://localhost:3306/dataagent?...` | MySQL JDBC 地址 |
| `DATAAGENT_DB_USERNAME` | 后端默认 `root` | MySQL 用户名；一键脚本要求显式配置 |
| `DATAAGENT_DB_PASSWORD` | 无 | MySQL 密码，`mysql` Profile 必需 |
| `DATAAGENT_STORAGE_ROOT` | `./data/uploads` | 上传 CSV 的存储目录 |
| `DATAAGENT_AI_MODEL` | 无 | OpenAI-compatible 模型名；使用 Ollama 时填写本地模型标签 |
| `DATAAGENT_AI_BASE_URL` | `https://api.openai.com/v1` | 模型服务地址；一键脚本会改为本地 Ollama 地址 |
| `DATAAGENT_AI_API_KEY` | 无 | AI 客户端要求非空；一键脚本仅在进程内生成 Ollama 占位值 |

不要把数据库密码或真实 API Key 写入 `application*.properties`、脚本或提交到 Git。

## API 接口

完整工作台接口使用 `mysql` Profile。所有 CSV 上传请求均使用 `multipart/form-data`，文件字段名为 `file`。

### 基础 CSV 解析接口

这两个接口在默认 `no-db` Profile 下也可使用，不保存上传文件和数据库记录。

| 请求方式 | URL | 功能说明 |
| --- | --- | --- |
| `POST` | `/api/dataset/upload` | 校验并解析 CSV，返回文件名、行列数和字段名 |
| `POST` | `/api/dataset/preview?limit=10` | 直接预览上传的 CSV；`limit` 默认为 10，范围为 1–100 |

### 数据集与分析接口

| 请求方式 | URL | 成功状态 | 功能说明 |
| --- | --- | --- | --- |
| `POST` | `/api/datasets` | `201` | 上传并保存 CSV、数据集元数据和字段信息 |
| `GET` | `/api/datasets` | `200` | 按创建时间倒序查询数据集列表 |
| `GET` | `/api/datasets/{id}` | `200` | 查询数据集详情和字段名 |
| `GET` | `/api/datasets/{id}/preview?limit=10` | `200` | 预览已保存 CSV；`limit` 范围为 1–100 |
| `GET` | `/api/datasets/{id}/quality` | `200` | 查询缺失值、缺失率、唯一值、字段类型和重复行 |
| `GET` | `/api/datasets/{id}/profile` | `200` | 查询数值统计或分类字段 Top 10 高频值 |
| `POST` | `/api/datasets/{id}/ai-analysis` | `200` | 基于详情、质量和统计结果生成 AI 分析报告 |
| `DELETE` | `/api/datasets/{id}` | `204` | 删除数据集、字段记录和对应 CSV 文件 |

不存在的数据集返回 `404`。请求参数或 CSV 校验失败时，统一异常处理会返回包含 `timestamp`、`status`、`error`、`message` 和 `path` 的 JSON。

## 数据分析能力

CSV 文件使用 UTF-8 读取，第一行必须是表头。字段名不能为空或重复，数据行的列数必须与表头一致。

质量分析支持：

- 每列缺失值数量与缺失率
- 每列唯一值数量
- `INTEGER`、`DECIMAL`、`BOOLEAN`、`DATE`、`STRING` 基础类型识别
- 整个数据集的重复行数量

统计分析支持：

- `INTEGER` / `DECIMAL`：`count`、`min`、`max`、`mean`、`median`
- `BOOLEAN` / `DATE` / `STRING`：`uniqueCount` 和 Top 10 高频值

前端使用 ECharts 将上述结果绘制为缺失率图、数值字段统计图和分类字段 Top Values 图，并通过 `ResizeObserver` 响应容器尺寸变化。

## AI 分析功能

AI 功能通过独立的 `AiModelClient` 接口封装，当前实现使用 OpenAI-compatible `/chat/completions` API。使用 Ollama 时，推荐选择本地 Qwen 文本模型，并通过 `DATAAGENT_AI_MODEL` 指定模型标签。

AI 请求不会直接发送整个 CSV 文件，而是整理以下已有结果：

- 数据集基本信息
- 数据质量分析结果
- 数据统计分析结果，包括部分 Top Values

模型报告固定包含：

1. 数据整体概况
2. 数据质量问题
3. 值得关注的字段
4. 主要统计结论
5. 分析建议

如果模型没有配置，接口会返回 `configured: false` 和“AI 模型尚未配置”，不会影响上传、预览、质量分析等其他功能。

当前 AI 功能是单次报告生成，不包含多轮对话、Agent 工具调用、RAG、向量数据库、自动 SQL 或 Python 执行。

## 开发验证

运行后端测试：

```bash
./mvnw test
```

Windows：

```bat
.\mvnw.cmd test
```

构建前端：

```bash
cd frontend
npm run build
```

## 当前版本

- 后端版本：`0.0.1-SNAPSHOT`
- 前端版本：`0.1.0`
- 项目阶段：可本地运行的 MVP

当前已经完成 CSV 解析与预览、MySQL 持久化、数据集管理、数据质量分析、统计分析、ECharts 可视化、AI 智能分析、统一异常处理和 Windows 一键启动脚本。

## 后续计划

- 为数据集列表增加分页、搜索和排序
- 为大文件增加流式处理、上传限制、异步分析和进度反馈
- 增加字段筛选、图表配置和更多可视化类型
- 增加分析报告保存、导出和历史记录
- 增加可配置的模型管理与 Prompt 模板
- 增加用户认证、权限控制和审计日志
- 补充容器化部署、CI、监控和更完整的集成测试

