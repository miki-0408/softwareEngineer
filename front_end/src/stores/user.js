import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const gender = ref(localStorage.getItem('gender') || '')

  // 存储空间
  const totalSpace = ref(0)
  const usedSpace = ref(0)
  const remainSpace = ref(0)

  // 私密空间状态
  const privateSpaceEnabled = ref(sessionStorage.getItem('ps_enabled') === 'true')
  const privateSpaceRootDirId = ref(sessionStorage.getItem('ps_root') || '')
  const privatePassword = ref(sessionStorage.getItem('ps_pwd') || '')

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
    gender.value = user.sex || ''

    localStorage.setItem('token', t)
    localStorage.setItem('userId', user.userId)
    localStorage.setItem('role', user.role)
    localStorage.setItem('username', user.name)
    localStorage.setItem('avatar', user.avatar || '')
    localStorage.setItem('gender', user.sex || '')
  }

  function setStorageInfo(storage) {
    totalSpace.value = storage.totalSpace
    usedSpace.value = storage.usedSpace
    remainSpace.value = storage.remainSpace
  }

  function setPrivateSpaceStatus(enabled, rootDirId) {
    privateSpaceEnabled.value = enabled
    sessionStorage.setItem('ps_enabled', enabled ? 'true' : 'false')
    if (rootDirId) {
      privateSpaceRootDirId.value = rootDirId
      sessionStorage.setItem('ps_root', rootDirId)
    }
  }

  function setPrivatePassword(password) {
    privatePassword.value = password
    sessionStorage.setItem('ps_pwd', password)
  }

  function clearPrivateAccess() {
    privatePassword.value = ''
    sessionStorage.removeItem('ps_pwd')
  }

  function updateUserInfo(name, sex, ava) {
    username.value = name
    if (sex) gender.value = sex
    if (ava) avatar.value = ava
    localStorage.setItem('username', name)
    if (sex) localStorage.setItem('gender', sex)
    if (ava) localStorage.setItem('avatar', ava)
  }

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
    token, userId, role, username, avatar, gender,
    totalSpace, usedSpace, remainSpace,
    privateSpaceEnabled, privateSpaceRootDirId, privatePassword,
    isLoggedIn, isAdmin, hasPrivateAccess,
    setLoginData, setStorageInfo, setPrivateSpaceStatus,
    setPrivatePassword, clearPrivateAccess,
    updateUserInfo, logout, formatSize
  }
})
