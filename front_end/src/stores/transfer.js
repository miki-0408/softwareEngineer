import { defineStore } from 'pinia'
import { ref } from 'vue'

let taskId = 0

export const useTransferStore = defineStore('transfer', () => {
  const tasks = ref([])
  const running = ref(0)
  const MAX_CONCURRENT = 2

  function enqueue(runner) {
    if (running.value < MAX_CONCURRENT) runner()
    else setTimeout(() => enqueue(runner), 300)
  }

  /** 添加上传任务: builder 返回 { xhr, body } */
  function addUpload(name, size, builder) {
    const id = ++taskId
    tasks.value.push({ id, name, size, type: 'upload', status: 'queued', progress: 0, errorMsg: '' })
    enqueue(() => {
      const task = tasks.value.find(t => t.id === id)
      if (!task) return
      task.status = 'running'
      running.value++
      const { xhr, body } = builder()
      xhr.responseType = 'json'
      xhr.upload.onprogress = e => { if (e.lengthComputable) task.progress = Math.round(e.loaded / e.total * 100) }
      xhr.onload = () => {
        task.progress = 100
        const resp = xhr.response
        if (xhr.status !== 200) {
          task.status = 'error'; task.errorMsg = 'HTTP ' + xhr.status
        } else if (!resp) {
          task.status = 'error'; task.errorMsg = '服务器无响应'
        } else if (resp.code !== 0) {
          task.status = 'error'; task.errorMsg = resp.message || '上传失败'
        } else {
          task.status = 'done'
        }
        running.value--
      }
      xhr.onerror = () => { task.status = 'error'; task.errorMsg = '网络错误'; running.value-- }
      xhr.send(body)
    })
    return id
  }

  /** 添加下载任务: 先弹出 saveFilePicker，再入队列。返回 true 表示已加入，false 表示用户取消 */
  async function addDownloadWithPicker(name, size, fileId, password) {
    let fileHandle = null
    if (window.showSaveFilePicker) {
      try {
        fileHandle = await window.showSaveFilePicker({ suggestedName: name })
      } catch { return false } // 用户取消
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
    const base = import.meta.env.PROD ? '' : '/api'
    const body = new URLSearchParams()
    body.append('fileId', fileId)
    if (password) body.append('privatePassword', password)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', base + '/user/file/download')
    if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
    xhr.responseType = 'blob'
    xhr.onprogress = e => { if (e.lengthComputable) task.progress = Math.round(e.loaded / e.total * 100) }
    xhr.onload = async () => {
      task.progress = 100
      if (xhr.status !== 200) {
        task.status = 'error'; task.errorMsg = 'HTTP ' + xhr.status
      } else if (xhr.response.type === 'application/json' || xhr.getResponseHeader('content-type')?.includes('json')) {
        // 后端可能返回 JSON 错误而非 blob
        task.status = 'error'; task.errorMsg = '下载失败'
      } else {
        const blob = new Blob([xhr.response])
        if (fileHandle) {
          try {
            const writable = await fileHandle.createWritable()
            await writable.write(blob); await writable.close()
          } catch { /* ignore */ }
        } else {
          const url = window.URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url; a.download = name
          document.body.appendChild(a); a.click(); document.body.removeChild(a)
          window.URL.revokeObjectURL(url)
        }
        task.status = 'done'
      }
      running.value--
    }
    xhr.onerror = () => { task.status = 'error'; task.errorMsg = '网络错误'; running.value-- }
    xhr.send(body)
  }

  function removeTask(id) {
    const idx = tasks.value.findIndex(t => t.id === id)
    if (idx >= 0) tasks.value.splice(idx, 1)
  }

  function clearDone() {
    tasks.value = tasks.value.filter(t => t.status !== 'done' && t.status !== 'error')
  }

  function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B'
    const k = 1024, i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + ['B', 'KB', 'MB', 'GB'][i]
  }

  return { tasks, running, addUpload, addDownloadWithPicker, removeTask, clearDone, formatSize }
})
