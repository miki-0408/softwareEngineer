import { ref } from 'vue'  // Vue 3 响应式：ref(变量)

/** 私密空间密码弹窗 composable：把异步的"弹窗等用户输入"封装成 Promise */
export function usePrivatePassword() {
  const visible = ref(false)   // 弹窗是否显示
  const password = ref('')     // 用户输入的密码
  let resolve = null           // Promise 的 resolve 引用（弹窗关闭时调用它）

  /** 弹出密码输入框，返回 Promise<string|null>：用户确认 → 密码；取消 → null */
  function request() {
    return new Promise((res) => {
      password.value = ''      // 每次弹出都清空上次的输入
      resolve = res            // 保存 resolve，等用户确认/取消时调用
      visible.value = true     // 显示弹窗
    })
  }

  /** 用户点击"确定"：关闭弹窗 → 把密码传给 Promise */
  function confirm() {
    visible.value = false
    if (resolve) { resolve(password.value); resolve = null }
  }

  /** 用户点击"取消"：关闭弹窗 → 传 null 给 Promise */
  function cancel() {
    visible.value = false
    if (resolve) { resolve(null); resolve = null }
  }

  return { visible, password, request, confirm, cancel }
}
