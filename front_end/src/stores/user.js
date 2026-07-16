import { defineStore } from 'pinia'        // Pinia 状态管理：定义全局共享数据仓库
import { ref, computed } from 'vue'         // Vue 3 响应式：ref(变量) / computed(计算属性)
import { formatSize } from '../utils/format' // 共享的文件大小格式化工具

export const useUserStore = defineStore('user', () => {
  // ===== 持久化状态：从 localStorage 恢复（登录时写入，刷新后自动恢复） =====

  const token = ref(localStorage.getItem('token') || '')     // JWT 令牌（登录成功时写入）
  const userId = ref(localStorage.getItem('userId') || '')   // 当前用户数据库 ID
  const role = ref(localStorage.getItem('role') || '')       // 角色：'user' 或 'admin'
  const username = ref(localStorage.getItem('username') || '') // 用户显示名称
  const avatar = ref(localStorage.getItem('avatar') || '')   // 头像 URL（可能是绝对路径或相对路径）
  const gender = ref(localStorage.getItem('gender') || '')   // 性别：'男'/'女'/'未知'

  // ===== 服务端加载状态：从后端 API 获取（不持久化，每次登录后重新拉取） =====

  const totalSpace = ref(0)    // 总存储空间（字节，默认 10GB）
  const usedSpace = ref(0)     // 已用空间（字节）
  const remainSpace = ref(0)   // 剩余空间（字节）

  // ===== 私密空间状态：从 sessionStorage 恢复（sessionStorage 在关闭标签页后自动清除） =====

  const privateSpaceEnabled = ref(sessionStorage.getItem('ps_enabled') === 'true') // 私密空间是否已开启
  const privateSpaceRootDirId = ref(sessionStorage.getItem('ps_root') || '')        // 私密空间根目录 ID
  const privatePassword = ref(sessionStorage.getItem('ps_pwd') || '')               // 已验证过的私密密码（密码正确才会被缓存）

  // ===== 计算属性：根据其他状态派生，自动缓存 =====

  const isAdmin = computed(() => role.value === 'admin') // 是否管理员（用于显示/隐藏管理后台入口）

  // ===== 登录数据写入：后端返回 token + 用户信息后调用 =====

  function setLoginData(data) {
    const { token: t, user } = data    // 解构后端返回值
    token.value = t                    // 写入内存（响应式）
    userId.value = user.userId
    role.value = user.role
    username.value = user.name
    avatar.value = user.avatar || ''
    gender.value = user.sex || ''

    localStorage.setItem('token', t)     // 同步写入 localStorage（持久化，刷新后恢复）
    localStorage.setItem('userId', user.userId)
    localStorage.setItem('role', user.role)
    localStorage.setItem('username', user.name)
    localStorage.setItem('avatar', user.avatar || '')
    localStorage.setItem('gender', user.sex || '')
  }

  // ===== 存储空间更新：后端返回 StorageSpace 对象后调用 =====

  function setStorageInfo(storage) {
    totalSpace.value = storage.totalSpace      // 总空间
    usedSpace.value = storage.usedSpace        // 已用
    remainSpace.value = storage.remainSpace     // 剩余
  }

  // ===== 私密空间状态更新 =====

  function setPrivateSpaceStatus(enabled, rootDirId) {
    privateSpaceEnabled.value = enabled
    sessionStorage.setItem('ps_enabled', enabled ? 'true' : 'false') // sessionStorage 可在跨页面但同标签页场景下持久化
    if (rootDirId) {
      privateSpaceRootDirId.value = rootDirId
      sessionStorage.setItem('ps_root', rootDirId)
    }
  }

  function setPrivatePassword(password) {
    privatePassword.value = password           // 仅在密码通过后端验证后调用
    sessionStorage.setItem('ps_pwd', password)
  }

  function clearPrivateAccess() {
    privatePassword.value = ''                 // 密码错误或注销时清空缓存
    sessionStorage.removeItem('ps_pwd')
  }

  // ===== 个人信息更新：改名/改性别/换头像后调用 =====

  function updateUserInfo(name, sex, ava) {
    username.value = name
    if (sex) gender.value = sex                // 性别可能不传（留空）
    if (ava) avatar.value = ava                // 头像可能不传（留空）
    localStorage.setItem('username', name)
    if (sex) localStorage.setItem('gender', sex)
    if (ava) localStorage.setItem('avatar', ava)
  }

  // ===== 注销：清空全部状态 =====

  function logout() {
    token.value = ''
    userId.value = ''
    role.value = ''
    username.value = ''
    avatar.value = ''
    gender.value = ''
    privatePassword.value = ''
    privateSpaceEnabled.value = false
    privateSpaceRootDirId.value = ''
    sessionStorage.removeItem('ps_pwd')
    sessionStorage.removeItem('ps_enabled')
    sessionStorage.removeItem('ps_root')
    localStorage.clear()                         // 清空 localStorage 中全部数据
  }

  return {
    token, userId, role, username, avatar, gender,
    totalSpace, usedSpace, remainSpace,
    privateSpaceEnabled, privateSpaceRootDirId, privatePassword,
    isAdmin, formatSize,  // formatSize 从 utils 重导出，方便模板中直接 userStore.formatSize(xxx)
    setLoginData, setStorageInfo, setPrivateSpaceStatus,
    setPrivatePassword, clearPrivateAccess,
    updateUserInfo, logout
  }
})
