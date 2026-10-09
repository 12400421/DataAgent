# DataAgent Frontend

DataAgent 的前端 MVP，使用 Vue 3、Vite 和 Axios。

## 本地运行

先启动 Spring Boot 后端并确保它监听 `http://localhost:8081`，然后执行：

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。开发服务器会把 `/api` 请求代理到
`http://localhost:8081`，因此后端无需为本地开发额外配置 CORS。

## 生产构建

```bash
npm run build
```

构建产物位于 `frontend/dist`。
