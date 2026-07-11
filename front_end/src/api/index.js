import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.PROD ? '' : '/api',
  timeout: 60000
})

// 请求拦截器：自动附带 token
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

// 响应拦截器：统一处理错误
http.interceptors.response.use(
  response => {
    // 下载文件返回的是 blob，不处理
    if (response.config.responseType === 'blob') {
      return response
    }
    const data = response.data
    if (data && data.code !== undefined && data.code !== 0) {
      if (data.code === 2) {
        ElMessage.warning(data.message || '文件名冲突')
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

// ===================== 认证 =====================

export const authAPI = {
  login(username, password) {
    const params = new URLSearchParams()
    params.append('username', username)
    params.append('password', password)
    return http.post('/login', params)
  },

  register(username, password, gender, avatarFile) {
    const fd = new FormData()
    fd.append('username', username)
    fd.append('password', password)
    if (gender) fd.append('gender', gender)
    if (avatarFile) fd.append('avatar', avatarFile)
    return http.post('/register', fd)
  }
}

// ===================== 用户 =====================

export const userAPI = {
  getUserInfo(userId) {
    const params = new URLSearchParams()
    params.append('userId', userId)
    return http.post('/userInfo', params)
  },

  changePassword(oldPassword, newPassword) {
    const params = new URLSearchParams()
    params.append('oldPassword', oldPassword)
    params.append('newPassword', newPassword)
    return http.post('/changePassword', params)
  },

  updateUserInfo(newUsername, newGender, newAvatar) {
    const fd = new FormData()
    fd.append('newUsername', newUsername)
    if (newGender) fd.append('newGender', newGender)
    if (newAvatar) fd.append('newAvatar', newAvatar)
    return http.post('/user/updateUserInfo', fd)
  }
}

// ===================== 目录 =====================

export const directoryAPI = {
  list(parentDirId) {
    const params = new URLSearchParams()
    if (parentDirId !== null && parentDirId !== undefined) {
      params.append('parentDirId', parentDirId)
    }
    return http.post('/user/directory/list', params)
  },

  create(dirName, parentDirId) {
    const params = new URLSearchParams()
    params.append('dirName', dirName)
    if (parentDirId !== null && parentDirId !== undefined) {
      params.append('parentDirId', parentDirId)
    }
    return http.post('/user/directory/create', params)
  },

  rename(dirId, newDirName) {
    const params = new URLSearchParams()
    params.append('dirId', dirId)
    params.append('newDirName', newDirName)
    return http.post('/user/directory/rename', params)
  },

  remove(dirId) {
    const params = new URLSearchParams()
    params.append('dirId', dirId)
    return http.post('/user/directory/delete', params)
  }
}

// ===================== 文件 =====================

export const fileAPI = {
  list(dirId) {
    const params = new URLSearchParams()
    params.append('dirId', dirId)
    return http.post('/user/file/list', params)
  },

  rename(fileId, newFileName, force) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    params.append('newFileName', newFileName)
    if (force) params.append('force', 'true')
    return http.post('/user/file/rename', params)
  },

  move(fileId, targetDirId, force) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    params.append('targetDirId', targetDirId)
    if (force) params.append('force', 'true')
    return http.post('/user/file/move', params)
  },

  remove(fileId) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    return http.post('/user/file/delete', params)
  },

  encrypt(fileId, privatePassword, targetDirId, force) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    params.append('privatePassword', privatePassword)
    params.append('targetDirId', targetDirId)
    if (force) params.append('force', 'true')
    return http.post('/user/file/encrypt', params)
  },

  decrypt(fileId, privatePassword, targetDirId, force) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    params.append('privatePassword', privatePassword)
    params.append('targetDirId', targetDirId)
    if (force) params.append('force', 'true')
    return http.post('/user/file/decrypt', params)
  }
}

// ===================== 回收站 =====================

export const recycleAPI = {
  list() {
    return http.post('/user/recycle/list', new URLSearchParams())
  },

  restore(fileId) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    return http.post('/user/recycle/restore', params)
  },

  deletePermanent(fileId) {
    const params = new URLSearchParams()
    params.append('fileId', fileId)
    return http.post('/user/recycle/deletePermanent', params)
  }
}

// ===================== 私密空间 =====================

export const privateSpaceAPI = {
  status() {
    return http.post('/user/privateSpace/status', new URLSearchParams())
  },

  enable(password) {
    const params = new URLSearchParams()
    params.append('password', password)
    return http.post('/user/privateSpace/enable', params)
  },

  disable(password) {
    const params = new URLSearchParams()
    params.append('password', password)
    return http.post('/user/privateSpace/disable', params)
  },

  verify(password) {
    const params = new URLSearchParams()
    params.append('password', password)
    return http.post('/user/privateSpace/verify', params)
  },

  // 私密空间文件列表
  listFiles(dirId) {
    const params = new URLSearchParams()
    params.append('dirId', dirId)
    return http.post('/user/privateSpace/files', params)
  },

  // 私密空间目录列表
  listDirectories(parentDirId) {
    const params = new URLSearchParams()
    if (parentDirId !== null && parentDirId !== undefined) {
      params.append('parentDirId', parentDirId)
    }
    return http.post('/user/privateSpace/directories', params)
  }
}

// ===================== 管理员 =====================

export const adminAPI = {
  getLogs() {
    return http.get('/systemAdmin/logs')
  },

  resetPassword(userId) {
    const params = new URLSearchParams()
    params.append('userId', userId)
    return http.post('/systemAdmin/resetPassword', params)
  },

  updateUserInfo(userId, newUsername, newGender, newAvatar) {
    const fd = new FormData()
    fd.append('userId', userId)
    fd.append('newUsername', newUsername)
    if (newGender) fd.append('newGender', newGender)
    if (newAvatar) fd.append('newAvatar', newAvatar)
    return http.post('/systemAdmin/updateUserInfo', fd)
  }
}

// ===================== 冲突重试 =====================

/** 遇到文件冲突时弹窗询问，确认则以 force=true 重试 */
export async function handleConflict(error, retryWithForce) {
  if (error && error.conflict) {
    try {
      await ElMessageBox.confirm(
        `文件「${error.conflictName}」已存在，是否替换？`,
        '文件名冲突',
        { confirmButtonText: '替换', cancelButtonText: '跳过', type: 'warning' }
      )
    } catch { return false /* 用户取消 */ }
    return await retryWithForce()
  }
  throw error
}

export default http
