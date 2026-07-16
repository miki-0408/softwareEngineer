import axios from 'axios'                        // HTTP 请求库：替代浏览器原生 fetch
import { ElMessage, ElMessageBox } from 'element-plus' // 消息提示 + 确认弹窗组件

const http = axios.create({                         // 创建 Axios 实例（统一配置）
  baseURL: import.meta.env.PROD ? '' : '/api',      // 生产环境用同域，开发环境 Vite 代理到 localhost:8080
  timeout: 60000                                    // 超时 60 秒（大文件上传可能很久）
})

// ===== 请求拦截器：每次请求前自动附加 JWT Token =====
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')       // 从 localStorage 读取登录时保存的 JWT
  if (token) config.headers['Authorization'] = 'Bearer ' + token  // 标准 JWT Bearer 头
  return config
})

// ===== 响应拦截器：统一处理错误码和冲突 =====
http.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') return response // blob 响应（下载文件）不解析 JSON
    const data = response.data
    if (data && data.code !== undefined && data.code !== 0) {
      if (data.code === 2) {                       // code=2 = 文件名冲突
        const err = new Error(data.message || '文件名冲突')
        err.conflict = true                        // 标记为冲突错误（handleConflict 会检查此标志）
        err.conflictName = data.data               // 冲突的文件名
        return Promise.reject(err)                 // 不弹 toast，由 handleConflict 弹确认弹窗
      }
      ElMessage.error(data.message || '操作失败')   // 其他业务错误（密码错误/权限不足等）→ 红框提示
      return Promise.reject(new Error(data.message || '操作失败'))
    }
    return response
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401 || status === 403) {      // Token 过期或权限不足
        ElMessage.error('登录已过期，请重新登录')
        localStorage.clear()                       // 清除全部登录信息
        window.location.href = '/'                 // 强制跳回登录页
      } else {
        ElMessage.error('网络错误: ' + error.message)
      }
    } else {
      ElMessage.error('无法连接到服务器')            // 后端没启动或网络不通
    }
    return Promise.reject(error)
  }
)

// ===== 通用助手：POST application/x-www-form-urlencoded =====
function post(url, params = {}) {
  const p = new URLSearchParams()                   // Spring Boot 默认接收 k=v&k=v 格式
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined) p.append(String(k), v) // 跳过 null/undefined（可选参数）
  }
  return http.post(url, p)
}

// ===== 通用助手：POST multipart/form-data（用于文件上传） =====
function postForm(url, fields = {}, fileFields = {}) {
  const fd = new FormData()                         // 浏览器原生表单数据格式
  for (const [k, v] of Object.entries(fields)) {
    if (v !== null && v !== undefined) fd.append(k, String(v))  // 普通文本字段
  }
  for (const [k, v] of Object.entries(fileFields)) {
    if (v !== null && v !== undefined) fd.append(k, v)           // 文件字段（File 对象）
  }
  return http.post(url, fd)
}

// ==================== 认证 API ====================

export const authAPI = {
  login: (username, password) => post('/login', { username, password }), // → UserController.login
  register: (username, password, gender, avatarFile) =>
    postForm('/register', { username, password, gender }, { avatar: avatarFile }) // → UserController.register（头像在 fileFields 中）
}

// ==================== 用户 API ====================

export const userAPI = {
  getUserInfo: (userId) => post('/userInfo', { userId }),           // → UserController.getUserInfo（返回用户+存储空间+私密空间状态）
  changePassword: (oldPassword, newPassword) => post('/changePassword', { oldPassword, newPassword }), // → UserController.changePassword
  updateUserInfo: (newUsername, newGender, newAvatar) =>
    postForm('/user/updateUserInfo', { newUsername, newGender }, { newAvatar }) // → UserController.updateUserInfo
}

// ==================== 目录 API ====================

export const directoryAPI = {
  list: (parentDirId) => post('/user/directory/list', { parentDirId }),     // parentDirId 为 null → 查根目录
  create: (dirName, parentDirId) => post('/user/directory/create', { dirName, parentDirId }),
  rename: (dirId, newDirName) => post('/user/directory/rename', { dirId, newDirName }),
  remove: (dirId) => post('/user/directory/delete', { dirId })              // 删除（仅空目录可删）
}

// ==================== 文件 API ====================

export const fileAPI = {
  list: (dirId) => post('/user/file/list', { dirId }),                     // 列出目录下的文件（自动排除已加密文件）
  rename: (fileId, newFileName, force) =>                                  // force 仅在冲突处理时传 true
    post('/user/file/rename', { fileId, newFileName, force: force ? 'true' : undefined }),
  move: (fileId, targetDirId, force) =>
    post('/user/file/move', { fileId, targetDirId, force: force ? 'true' : undefined }),
  remove: (fileId) => post('/user/file/delete', { fileId }),               // 移入回收站
  encrypt: (fileId, privatePassword, targetDirId, force) =>                 // 移入私密空间（加密）
    post('/user/file/encrypt', { fileId, privatePassword, targetDirId, force: force ? 'true' : undefined }),
  decrypt: (fileId, privatePassword, targetDirId, force) =>                 // 移出私密空间（解密）
    post('/user/file/decrypt', { fileId, privatePassword, targetDirId, force: force ? 'true' : undefined })
}

// ==================== 回收站 API ====================

export const recycleAPI = {
  list: () => post('/user/recycle/list'),                                   // 列出回收站文件
  restore: (fileId) => post('/user/recycle/restore', { fileId }),          // 从回收站恢复
  deletePermanent: (fileId) => post('/user/recycle/deletePermanent', { fileId }) // 彻底删除
}

// ==================== 私密空间 API ====================

export const privateSpaceAPI = {
  status: () => post('/user/privateSpace/status'),                          // 获取私密空间启用状态+根目录ID
  enable: (password) => post('/user/privateSpace/enable', { password }),   // 开启私密空间（设置密码）
  disable: (password) => post('/user/privateSpace/disable', { password }), // 关闭私密空间（需验证密码+目录为空）
  verify: (password) => post('/user/privateSpace/verify', { password }),   // 验证密码（返回临时 token）
  listFiles: (dirId) => post('/user/privateSpace/files', { dirId }),       // 列出加密文件
  listDirectories: (parentDirId) => post('/user/privateSpace/directories', { parentDirId }) // 列出加密目录
}

// ==================== 管理员 API ====================

export const adminAPI = {
  getLogs: () => http.get('/systemAdmin/logs'),                             // 获取操作日志（GET 请求，无需参数）
  resetPassword: (userId) => post('/systemAdmin/resetPassword', { userId }), // 重置用户密码为默认值
  updateUserInfo: (userId, newUsername, newGender, newAvatar) =>
    postForm('/systemAdmin/updateUserInfo', { userId, newUsername, newGender }, { newAvatar })
}

// ==================== 冲突处理工具 ====================

/** 文件名冲突弹窗：用户选择"替换"→ 调用 retryWithForce()；选择"跳过"→ 返回 false */
export async function handleConflict(error, retryWithForce) {
  if (error && error.conflict) {                    // 只有真正的冲突错误才弹窗
    try {
      await ElMessageBox.confirm(                   // Element Plus 确认弹窗
        `文件「${error.conflictName}」已存在，是否替换？`,
        '文件名冲突',
        { confirmButtonText: '替换', cancelButtonText: '跳过', type: 'warning' }
      )
    } catch { return false }                        // 用户点取消/关闭 → 跳过
    return await retryWithForce()                   // 用户点替换 → 用 force=true 重试
  }
  throw error                                       // 非冲突错误 → 继续向上抛出（给 withConflictRetry 处理）
}

/**
 * 冲突重试包装器：先尝试普通调用 → 如果是冲突错误则弹窗 → 用户确认则重试
 * 返回 true（成功/替换）或 false（跳过），失败（其他错误）抛异常
 */
export async function withConflictRetry(fn, forceFn) {
  try { await fn(); return true }                   // 首次调用成功 → 直接返回
  catch (e) { return !!(await handleConflict(e, forceFn)) } // 进入冲突处理流程
}

export default http
