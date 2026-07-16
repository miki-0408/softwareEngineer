/** 格式化文件大小：自动选择 B/KB/MB/GB/TB 单位，保留合适的小数位数 */
export function formatSize(bytes) {
  if (!bytes || bytes === 0) return '0 B'   // 0 或空值直接返回
  const units = ['B', 'KB', 'MB', 'GB', 'TB'] // 单位数组，索引越大单位越大
  const k = 1024                            // 二进制换算系数
  const i = Math.floor(Math.log(bytes) / Math.log(k)) // 用对数算出属于哪个单位区间
  return parseFloat((bytes / Math.pow(k, i)).toFixed(i === 0 ? 0 : 2)) + ' ' + units[i] // 换算 + 保留小数
}

/** 格式化时间：把 Java LocalDateTime 的 ISO 格式中的 'T' 替换为空格，更易读 */
export function formatTime(timeStr) {
  if (!timeStr) return ''                    // 空值保护
  return timeStr.replace('T', ' ')           // "2024-01-01T12:00:00" → "2024-01-01 12:00:00"
}
