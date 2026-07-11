/** 格式化文件大小（B → KB/MB/GB/TB 自适应） */
export function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(i === 0 ? 0 : 2)) + ' ' + units[i]
}

/** 格式化后端返回的时间字符串（去除 T） */
export function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}
