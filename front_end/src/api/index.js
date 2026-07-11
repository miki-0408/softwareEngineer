import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.PROD ? '' : '/api',
  timeout: 60000
})

// 请求拦截器：自动附带 token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers['Authorization'] = 'Bearer ' + token
  return config
})

// 响应拦截器：统一处理错误
http.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') return response
    const data = response.data
    if (data && data.code !== undefined && data.code !== 0) {
      if (data.code === 2) {
        const err = new Error(data.message || '文件名冲突')
        err.conflict = true
        err.conflictName = data.data
        return Promise.reject(err)
      }
      ElMessage.error(data.message || '操作失败')
      return Promise.reject(new Error(data.message || '操作失败'))
    }
    return response
  },
  error => {
    if (error.response) {
      const status = error.response.status
      if (status === 401 || status === 403) {
        ElMessage.error('登录已过期，请重新登录')
        localStorage.clear()
        window.location.href = '/'
      } else {
        ElMessage.error('网络错误: ' + error.message)
      }
    } else {
      ElMessage.error('无法连接到服务器')
    }
    return Promise.reject(error)
  }
)

// ===================== 通用助手 =====================

/** POST application/x-www-form-urlencoded — 自动跳过 null/undefined */
function post(url, params = {}) {
  const p = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined) p.append(String(k), v)
  }
  return http.post(url, p)
}

/** POST multipart/form-data */
function postForm(url, fields = {}, fileFields = {}) {
  const fd = new FormData()
  for (const [k, v] of Object.entries(fields)) {
    if (v !== null && v !== undefined) fd.append(k, String(v))
  }
  for (const [k, v] of Object.entries(fileFields)) {
    if (v !== null && v !== undefined) fd.append(k, v)
  }
  return http.post(url, fd)
}

// ===================== 认证 =====================

export const authAPI = {
  login: (username, password) => post('/login', { username, password }),
  register: (username, password, gender, avatarFile) =>
    postForm('/register', { username, password, gender }, { avatar: avatarFile })
}

// ===================== 用户 =====================

export const userAPI = {
  getUserInfo: (userId) => post('/userInfo', { userId }),
  changePassword: (oldPassword, newPassword) => post('/changePassword', { oldPassword, newPassword }),
  updateUserInfo: (newUsername, newGender, newAvatar) =>
    postForm('/user/updateUserInfo', { newUsername, newGender }, { newAvatar })
}

// ===================== 目录 =====================

export const directoryAPI = {
  list: (parentDirId) => post('/user/directory/list', { parentDirId }),
  create: (dirName, parentDirId) => post('/user/directory/create', { dirName, parentDirId }),
  rename: (dirId, newDirName) => post('/user/directory/rename', { dirId, newDirName }),
  remove: (dirId) => post('/user/directory/delete', { dirId })
}

// ===================== 文件 =====================

export const fileAPI = {
  list: (dirId) => post('/user/file/list', { dirId }),
  rename: (fileId, newFileName, force) =>
    post('/user/file/rename', { fileId, newFileName, force: force ? 'true' : undefined }),
  move: (fileId, targetDirId, force) =>
    post('/user/file/move', { fileId, targetDirId, force: force ? 'true' : undefined }),
  remove: (fileId) => post('/user/file/delete', { fileId }),
  encrypt: (fileId, privatePassword, targetDirId, force) =>
    post('/user/file/encrypt', { fileId, privatePassword, targetDirId, force: force ? 'true' : undefined }),
  decrypt: (fileId, privatePassword, targetDirId, force) =>
    post('/user/file/decrypt', { fileId, privatePassword, targetDirId, force: force ? 'true' : undefined })
}

// ===================== 回收站 =====================

export const recycleAPI = {
  list: () => post('/user/recycle/list'),
  restore: (fileId) => post('/user/recycle/restore', { fileId }),
  deletePermanent: (fileId) => post('/user/recycle/deletePermanent', { fileId })
}

// ===================== 私密空间 =====================

export const privateSpaceAPI = {
  status: () => post('/user/privateSpace/status'),
  enable: (password) => post('/user/privateSpace/enable', { password }),
  disable: (password) => post('/user/privateSpace/disable', { password }),
  verify: (password) => post('/user/privateSpace/verify', { password }),
  listFiles: (dirId) => post('/user/privateSpace/files', { dirId }),
  listDirectories: (parentDirId) => post('/user/privateSpace/directories', { parentDirId })
}

// ===================== 管理员 =====================

export const adminAPI = {
  getLogs: () => http.get('/systemAdmin/logs'),
  resetPassword: (userId) => post('/systemAdmin/resetPassword', { userId }),
  updateUserInfo: (userId, newUsername, newGender, newAvatar) =>
    postForm('/systemAdmin/updateUserInfo', { userId, newUsername, newGender }, { newAvatar })
}

// ===================== 冲突处理 =====================

/** 遇到文件冲突时弹窗询问，确认则以 force=true 重试 */
export async function handleConflict(error, retryWithForce) {
  if (error && error.conflict) {
    try {
      await ElMessageBox.confirm(
        `文件「${error.conflictName}」已存在，是否替换？`,
        '文件名冲突',
        { confirmButtonText: '替换', cancelButtonText: '跳过', type: 'warning' }
      )
    } catch { return false }
    return await retryWithForce()
  }
  throw error
}

/** 冲突重试包装器 — 返回 true（成功）或 false（跳过），失败则抛异常 */
export async function withConflictRetry(fn, forceFn) {
  try { await fn(); return true }
  catch (e) { return !!(await handleConflict(e, forceFn)) }
}

export default http
