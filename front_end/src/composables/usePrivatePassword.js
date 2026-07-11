import { ref } from 'vue'

/** 私密空间密码弹窗 composable — 返回 Promise<string|null> */
export function usePrivatePassword() {
  const visible = ref(false)
  const password = ref('')
  let resolve = null

  function request() {
    return new Promise((res) => {
      password.value = ''
      resolve = res
      visible.value = true
    })
  }

  function confirm() {
    visible.value = false
    if (resolve) { resolve(password.value); resolve = null }
  }

  function cancel() {
    visible.value = false
    if (resolve) { resolve(null); resolve = null }
  }

  return { visible, password, request, confirm, cancel }
}
