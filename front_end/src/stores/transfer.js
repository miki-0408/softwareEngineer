import { defineStore } from 'pinia'        // Pinia 状态管理：定义全局共享数据仓库
import { ref, watch } from 'vue'            // Vue 3 响应式：ref(变量) / watch(监听变化)
import { formatSize } from '../utils/format' // 共享的文件大小格式化工具

const STORAGE_KEY = 'transfer_tasks'        // localStorage 键名（存储已完成和中断的任务）
const SEQ_KEY = 'transfer_seq'             // localStorage 键名（存储任务 ID 计数器的最大值）

function loadFromStorage(key, fallback) {
  try {
    const raw = localStorage.getItem(key)   // 从 localStorage 读取 JSON 字符串
    return raw ? JSON.parse(raw) : fallback // 解析成功返回数据，失败返回 fallback
  } catch { return fallback }               // 解析异常也返回 fallback（容错）
}

let taskId = loadFromStorage(SEQ_KEY, 0)    // 任务计数器：恢复最后一次的最大 ID，避免 ID 重复

export const useTransferStore = defineStore('transfer', () => {
  // 从 localStorage 恢复上次未完成的任务列表（刷新后回来还能看到）
  const restored = loadFromStorage(STORAGE_KEY, [])
  if (restored.length > 0) {
    taskId = Math.max(taskId, ...restored.map(t => t.id)) // 确保 ID 不会冲突：取上次最大 ID
  }

  // 任务数组：恢复时把"未完成"标记为"中断"（XHR 请求随页面刷新而销毁，无法恢复）
  const tasks = ref(restored.map(t => {
    if (t.status === 'queued' || t.status === 'running') {
      return { ...t, status: 'interrupted', progress: 0, errorMsg: '页面刷新，任务中断' }
    }
    return t                         // 已完成/已失败的任务保持原样
  }))
  const running = ref(0)              // 当前正在运行的并发任务数
  const MAX_CONCURRENT = 2            // 最大并发数：同时最多 2 个传输任务

  // 每次 tasks 数组或 taskId 变化时，自动持久化到 localStorage
  watch([tasks, () => taskId], () => {
    try {
      localStorage.setItem(SEQ_KEY, JSON.stringify(taskId)) // 持久化 ID 计数器
      const data = tasks.value.map(t => ({                   // 只持久化可序列化的字段
        id: t.id, name: t.name, size: t.size, type: t.type,
        status: t.status, progress: t.progress, errorMsg: t.errorMsg
      }))
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data)) // 持久化任务列表
    } catch { /* ignore：localStorage 满了等情况静默失败 */ }
  }, { deep: true })                  // deep:true → 监听数组内部元素的属性变化

  // ===== 并发队列调度 =====

  function enqueue(runner) {
    if (running.value < MAX_CONCURRENT) runner()         // 有空位 → 立即执行
    else setTimeout(() => enqueue(runner), 300)           // 无空位 → 300ms 后重试
  }

  // ===== 添加上传任务：使用 XMLHttpRequest 直接发送（支持 onprogress 事件追踪进度） =====

  function addUpload(name, size, builder, onComplete) {
    const id = ++taskId                                   // 生成唯一任务 ID
    tasks.value.push({ id, name, size, type: 'upload', status: 'queued', progress: 0, errorMsg: '' })
    enqueue(() => {                                       // 入并发队列
      const task = tasks.value.find(t => t.id === id)     // 找到当前任务
      if (!task) return                                   // 可能在排队期间被删除
      task.status = 'running'                             // 标记为运行中
      running.value++                                     // 并发计数 +1
      const { xhr, body } = builder()                      // builder 由调用方提供：新建 XHR + 构建 FormData
      xhr.responseType = 'json'                           // 期望后端返回 JSON
      xhr.upload.onprogress = e => {                      // 上传进度事件（浏览器原生支持）
        if (e.lengthComputable) {
          task.progress = Math.min(95, Math.round(e.loaded / e.total * 95)) // 网络上传只到 95%，留 5% 给服务端处理
        }
      }
      xhr.onload = () => {                                // 上传完成（服务器返回 HTTP 响应）
        task.progress = 100                                // 服务端处理完毕 → 跳 100%
        const resp = xhr.response                         // 后端 JSON 响应
        if (xhr.status !== 200) {
          task.status = 'error'; task.errorMsg = 'HTTP ' + xhr.status
        } else if (!resp) {
          task.status = 'error'; task.errorMsg = '服务器无响应'
        } else if (resp.code !== 0) {
          task.status = 'error'; task.errorMsg = resp.message || '上传失败' // 后端业务错误（压缩失败/空间不足等）
        } else {
          task.status = 'done'                             // 上传成功
        }
        running.value--                                    // 并发计数 -1（释放一个槽位）
        if (onComplete) onComplete(task.status === 'done') // 回调通知调用方（用于刷新目录列表）
      }
      xhr.onerror = () => {                                // 网络错误（连接失败/超时）
        task.status = 'error'; task.errorMsg = '网络错误'
        running.value--
        if (onComplete) onComplete(false)
      }
      xhr.send(body)                                       // 发送请求体（FormData，含文件数据和参数）
    })
    return id
  }

  // ===== 添加下载任务：支持 File System Access API 选择保存位置 =====

  async function addDownloadWithPicker(name, size, fileId, password) {
    let fileHandle = null
    if (window.showSaveFilePicker) {
      try {
        fileHandle = await window.showSaveFilePicker({ suggestedName: name }) // 弹出系统"另存为"对话框
      } catch { return false }                           // 用户取消对话框
    }
    const id = ++taskId
    tasks.value.push({ id, name, size, type: 'download', status: 'queued', progress: 0, errorMsg: '' })
    enqueue(() => startDownload(id, fileId, password, name, fileHandle))
    return true
  }

  function startDownload(id, fileId, password, name, fileHandle) {
    const task = tasks.value.find(t => t.id === id)
    if (!task) return
    task.status = 'running'
    running.value++
    const token = localStorage.getItem('token')
    const base = import.meta.env.PROD ? '' : '/api'         // 生产环境用同域，开发环境用 /api 代理
    const body = new URLSearchParams()
    body.append('fileId', fileId)
    if (password) body.append('privatePassword', password)  // 加密文件需要私密密码

    const xhr = new XMLHttpRequest()
    xhr.open('POST', base + '/user/file/download')         // 下载接口（POST 方式，参数在 body 中）
    if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token) // 携带 JWT
    xhr.responseType = 'blob'                               // 二进制响应 → 直接写入磁盘
    xhr.onprogress = e => {                                 // 下载进度事件
      if (e.lengthComputable) task.progress = Math.round(e.loaded / e.total * 100)
    }
    xhr.onload = async () => {
      task.progress = 100
      if (xhr.status !== 200) {
        task.status = 'error'; task.errorMsg = 'HTTP ' + xhr.status
      } else if (xhr.response.type === 'application/json' || xhr.getResponseHeader('content-type')?.includes('json')) {
        task.status = 'error'; task.errorMsg = '下载失败'   // 后端返回 JSON 错误而非文件内容
      } else {
        const blob = new Blob([xhr.response])               // 把二进制数据封装为 Blob 对象
        if (fileHandle) {
          try {
            const writable = await fileHandle.createWritable() // 通过 File System Access API 写入磁盘
            await writable.write(blob)
            await writable.close()
          } catch { /* ignore */ }
        } else {
          const url = window.URL.createObjectURL(blob)       // 不支持 File System Access API → 传统下载
          const a = document.createElement('a')              // 创建隐藏的 <a> 标签
          a.href = url
          a.download = name                                  // 设置下载文件名
          document.body.appendChild(a)
          a.click()                                          // 触发下载
          document.body.removeChild(a)                       // 清理 DOM
          window.URL.revokeObjectURL(url)                     // 释放内存
        }
        task.status = 'done'
      }
      running.value--
    }
    xhr.onerror = () => { task.status = 'error'; task.errorMsg = '网络错误'; running.value-- }
    xhr.send(body)
  }

  // ===== 任务管理 =====

  function removeTask(id) {
    const idx = tasks.value.findIndex(t => t.id === id)
    if (idx >= 0) tasks.value.splice(idx, 1)               // 用 splice 删除指定索引的元素
  }

  function clearDone() {
    tasks.value = tasks.value.filter(t =>                   // 只保留正在进行的任务
      t.status !== 'done' && t.status !== 'error' && t.status !== 'interrupted'
    )
  }

  return { tasks, running, addUpload, addDownloadWithPicker, removeTask, clearDone, formatSize }
})
