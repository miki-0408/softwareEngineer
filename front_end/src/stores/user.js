import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  // 存储空间
  const totalSpace = ref(0)
  const usedSpace = ref(0)
  const remainSpace = ref(0)

  // 私密空间状态
  const privateSpaceEnabled = ref(false)
  const privateSpaceRootDirId = ref('')    // 私密空间根目录 ID
  const privatePassword = ref('')          // 会话内缓存的私密密码

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'admin')
  const hasPrivateAccess = computed(() => !!privatePassword.value)

  function setLoginData(data) {
    const { token: t, user } = data
    token.value = t
    userId.value = user.userId
    role.value = user.role
    username.value = user.name
    avatar.value = user.avatar || ''

    localStorage.setItem('token', t)
    localStorage.setItem('userId', user.userId)
    localStorage.setItem('role', user.role)
    localStorage.setItem('username', user.name)
    localStorage.setItem('avatar', user.avatar || '')
  }

  function setStorageInfo(storage) {
    totalSpace.value = storage.totalSpace
    usedSpace.value = storage.usedSpace
    remainSpace.value = storage.remainSpace
  }

  function setPrivateSpaceStatus(enabled, rootDirId) {
    privateSpaceEnabled.value = enabled
    if (rootDirId) privateSpaceRootDirId.value = rootDirId
  }

  function setPrivatePassword(password) {
    privatePassword.value = password
  }

  function clearPrivateAccess() {
    privatePassword.value = ''
  }

  function updateUserInfo(name, sex, ava) {
    username.value = name
    avatar.value = ava || avatar.value
    localStorage.setItem('username', name)
    if (ava) localStorage.setItem('avatar', ava)
  }

  function logout() {
    token.value = ''
    userId.value = ''
    role.value = ''
    username.value = ''
    avatar.value = ''
    localStorage.clear()
  }

  function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    const k = 1024
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i]
  }

  return {
    token, userId, role, username, avatar,
    totalSpace, usedSpace, remainSpace,
    privateSpaceEnabled, privateSpaceRootDirId, privatePassword,
    isLoggedIn, isAdmin, hasPrivateAccess,
    setLoginData, setStorageInfo, setPrivateSpaceStatus,
    setPrivatePassword, clearPrivateAccess,
    updateUserInfo, logout, formatSize
  }
})
