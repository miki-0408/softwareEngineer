import { ref, computed } from 'vue'  // Vue 3 响应式：ref(变量) / computed(计算属性)

/**
 * 文件浏览器核心逻辑：MainView 和 PrivateSpaceView 共用
 *
 * @param {Object}   apiSet          API 方法集合
 * @param {Function} apiSet.loadDirs (parentId) => 目录数组 的 Promise
 * @param {Function} apiSet.loadFs   (dirId) => 文件数组 的 Promise（不同页面调不同 API）
 * @param {string}   apiSet.rootName 根目录的显示名称（"我的文件" 或 "私密空间"）
 */
export function useFileBrowser({ loadDirs, loadFs, rootName }) {
  // ===== 导航状态 =====

  const currentDirId = ref(null)   // 当前所在目录的数据库 ID（null = 正在加载根目录）
  const breadcrumb = ref([])       // 面包屑栈：[{dirId, dirName}, ...]，首项为根目录，末项为当前目录
  const directories = ref([])      // 当前目录下的子目录列表
  const files = ref([])            // 当前目录下的文件列表
  const loading = ref(false)       // 是否正在加载数据（控制表格的 v-loading 效果）

  /** 当前路径字符串：面包屑各段用 " / " 拼接，如 "我的文件 / 项目 / 图片" */
  const currentPath = computed(() =>
    breadcrumb.value.map(b => b.dirName).join(' / ') || rootName
  )

  // ===== 导航函数 =====

  /** 点击目录进入子目录：入栈 + 刷新数据 */
  async function navigateToDir(dirId, dirName) {
    currentDirId.value = dirId
    breadcrumb.value.push({ dirId: String(dirId), dirName }) // 面包屑加一项（ID 转字符串避免类型比较问题）
    directories.value = []; files.value = []; loading.value = true // 先清空再加载（避免旧数据闪烁）
    await refreshCurrentDir()
  }

  /** 点击面包屑中的某一级：栈截断 + 刷新 */
  async function navigateToBreadcrumb(index) {
    const target = breadcrumb.value[index]
    breadcrumb.value = breadcrumb.value.slice(0, index + 1) // 截断数组：丢掉 index 之后的所有段
    currentDirId.value = Number(target.dirId)               // 恢复到那一级目录的 ID
    directories.value = []; files.value = []; loading.value = true
    await refreshCurrentDir()
  }

  // ===== 数据加载 =====

  async function loadDirectories(parentId) {
    try { directories.value = await loadDirs(parentId) }   // 调用方传入的 API 方法（不同页面调不同 API）
    catch { directories.value = [] }                        // 出错时显示空列表
  }

  async function loadFilesFn(dirId) {
    if (!dirId) { files.value = []; return }                // dirId 为 null 时不加载（还没确定目录）
    loading.value = true
    try { files.value = await loadFs(dirId) }
    catch { files.value = [] }
    finally { loading.value = false }                       // 无论成功失败都停止 loading
  }

  /** 刷新当前目录：重新加载子目录 + 文件列表 */
  async function refreshCurrentDir() {
    await loadDirectories(currentDirId.value)
    if (currentDirId.value) await loadFilesFn(currentDirId.value)
  }

  // ===== 列表合成：把目录和文件合并成统一的表格行 =====

  const allItems = computed(() => {
    const dirs = directories.value.map(d => ({               // 目录行
      id: d.dirId, name: d.dirName, type: '文件夹', isDir: true,
      size: 0, time: d.createTime, order: 0, original: d    // order:0 → 文件夹排在前面
    }))
    const fls = files.value.map(f => ({                      // 文件行
      id: f.fileId, name: f.fileName, type: f.fileType || '未知', isDir: false,
      size: f.fileSize, time: f.uploadTime, order: 1, original: f // order:1 → 文件排在后面
    }))
    return [...dirs, ...fls]                                 // 合并 → 文件夹优先
  })

  /** 点击行：如果是目录则进入，如果是文件则无反应（文件操作在操作列） */
  function onRowClick(row) {
    if (row.isDir) navigateToDir(row.id, row.name)
  }

  // ===== 筛选逻辑 =====

  const filterText = ref('')         // 搜索框文字（按文件名/目录名过滤）
  const filterType = ref('')         // 类型下拉选择（"文件夹" / "pdf" / "jpg" 等）
  const selectedItems = ref([])      // 已被勾选的项（多选批量操作）
  const tableRef = ref(null)         // el-table 组件的引用（用于调用 clearSelection 等方法）

  /** 从当前文件列表中动态提取已有的文件类型（用于类型下拉框的选项） */
  const fileTypeOptions = computed(() => {
    const types = new Set()
    files.value.forEach(f => { if (f.fileType) types.add(f.fileType) }) // 去重
    return [...types].sort()                                             // 转数组 + 字母排序
  })

  /** 按搜索词 + 类型筛选后的最终显示列表 */
  const filteredItems = computed(() => {
    let items = allItems.value
    if (filterText.value) {
      const q = filterText.value.toLowerCase()
      items = items.filter(i => i.name.toLowerCase().includes(q))       // 名称模糊匹配
    }
    if (filterType.value) {
      items = items.filter(i => filterType.value === '文件夹' ? i.isDir : i.type === filterType.value)
    }
    return items
  })

  /** el-table 的选择变化事件：更新 selectedItems */
  function onSelectionChange(selection) { selectedItems.value = selection }

  // ===== 辅助：供外部设置初始导航状态 =====

  function setBreadcrumb(items) { breadcrumb.value = items }    // 直接覆盖面包屑（初始化用）
  function setCurrentDirId(id) { currentDirId.value = id }      // 直接设目录 ID（初始化用）

  // ===== 对话框状态（MainView 和 PrivateSpaceView 共用的各种弹窗开关和数据） =====

  const moveVisible = ref(false)        // 移动弹窗是否显示
  const moveFileTarget = ref(null)      // 单文件移动的目标文件对象
  const moveDirTree = ref([])           // 移动弹窗中的目录树数据
  const moveTargetDirId = ref(null)     // 用户在目录树中选中的目标目录 ID
  const moveLoading = ref(false)        // 移动操作是否正在执行
  const batchMoveItems = ref([])        // 批量移动时选中的文件/目录数组

  const uploadVisible = ref(false)      // 上传弹窗是否显示

  const createDirVisible = ref(false)   // 新建文件夹弹窗
  const createDirName = ref('')         // 新建文件夹的名称
  const createDirLoading = ref(false)   // 新建文件夹是否正在请求

  const renameDirVisible = ref(false)   // 重命名文件夹弹窗
  const renameDirTarget = ref(null)     // 被重命名的目录对象
  const renameDirNewName = ref('')      // 新文件夹名
  const renameDirLoading = ref(false)   // 重命名是否正在请求

  const renameFileVisible = ref(false)  // 重命名文件弹窗
  const renameFileTarget = ref(null)    // 被重命名的文件对象
  const renameFileNewName = ref('')     // 新文件名
  const renameFileLoading = ref(false)  // 重命名是否正在请求

  return {
    currentDirId, breadcrumb, directories, files, loading, currentPath,
    navigateToDir, navigateToBreadcrumb, refreshCurrentDir,
    loadDirectories, loadFilesFn,                               // MainView 的 navigateToRoot 需要直接调 loadDirectories
    setBreadcrumb, setCurrentDirId,
    allItems, onRowClick,
    filterText, filterType, selectedItems, tableRef,
    fileTypeOptions, filteredItems, onSelectionChange,
    moveVisible, moveFileTarget, moveDirTree, moveTargetDirId, moveLoading, batchMoveItems,
    uploadVisible,
    createDirVisible, createDirName, createDirLoading,
    renameDirVisible, renameDirTarget, renameDirNewName, renameDirLoading,
    renameFileVisible, renameFileTarget, renameFileNewName, renameFileLoading
  }
}
