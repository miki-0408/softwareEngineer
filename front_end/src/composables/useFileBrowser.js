import { ref, computed } from 'vue'

/**
 * 文件浏览器核心逻辑 — MainView 和 PrivateSpaceView 共用
 *
 * @param {Object}   apiSet
 * @param {Function} apiSet.loadDirs   (parentId) => Promise<Array<{dirId,dirName,createTime,...}>>
 * @param {Function} apiSet.loadFs     (dirId) => Promise<Array<{fileId,fileName,fileType,fileSize,uploadTime,isEncrypted,...}>>
 * @param {string}   apiSet.rootName   根目录显示名称
 */
export function useFileBrowser({ loadDirs, loadFs, rootName }) {
  // ==================== 导航 ====================
  const currentDirId = ref(null)
  const breadcrumb = ref([])
  const directories = ref([])
  const files = ref([])
  const loading = ref(false)

  const currentPath = computed(() =>
    breadcrumb.value.map(b => b.dirName).join(' / ') || rootName
  )

  async function navigateToDir(dirId, dirName) {
    currentDirId.value = dirId
    breadcrumb.value.push({ dirId: String(dirId), dirName })
    directories.value = []; files.value = []; loading.value = true
    await refreshCurrentDir()
  }

  async function navigateToBreadcrumb(index) {
    const target = breadcrumb.value[index]
    breadcrumb.value = breadcrumb.value.slice(0, index + 1)
    currentDirId.value = Number(target.dirId)
    directories.value = []; files.value = []; loading.value = true
    await refreshCurrentDir()
  }

  // ==================== 数据加载 ====================
  async function loadDirectories(parentId) {
    try { directories.value = await loadDirs(parentId) }
    catch { directories.value = [] }
  }

  async function loadFilesFn(dirId) {
    if (!dirId) { files.value = []; return }
    loading.value = true
    try { files.value = await loadFs(dirId) }
    catch { files.value = [] }
    finally { loading.value = false }
  }

  async function refreshCurrentDir() {
    await loadDirectories(currentDirId.value)
    if (currentDirId.value) await loadFilesFn(currentDirId.value)
  }

  // ==================== 列表合成 ====================
  const allItems = computed(() => {
    const dirs = directories.value.map(d => ({
      id: d.dirId, name: d.dirName, type: '文件夹', isDir: true,
      size: 0, time: d.createTime, order: 0, original: d
    }))
    const fls = files.value.map(f => ({
      id: f.fileId, name: f.fileName, type: f.fileType || '未知', isDir: false,
      size: f.fileSize, time: f.uploadTime, order: 1, original: f
    }))
    return [...dirs, ...fls]
  })

  function onRowClick(row) {
    if (row.isDir) navigateToDir(row.id, row.name)
  }

  // ==================== 筛选 ====================
  const filterText = ref('')
  const filterType = ref('')
  const selectedItems = ref([])
  const tableRef = ref(null)

  const fileTypeOptions = computed(() => {
    const types = new Set()
    files.value.forEach(f => { if (f.fileType) types.add(f.fileType) })
    return [...types].sort()
  })

  const filteredItems = computed(() => {
    let items = allItems.value
    if (filterText.value) {
      const q = filterText.value.toLowerCase()
      items = items.filter(i => i.name.toLowerCase().includes(q))
    }
    if (filterType.value) {
      items = items.filter(i => filterType.value === '文件夹' ? i.isDir : i.type === filterType.value)
    }
    return items
  })

  function onSelectionChange(selection) { selectedItems.value = selection }

  /** 直接设置面包屑（初始化用） */
  function setBreadcrumb(items) { breadcrumb.value = items }
  function setCurrentDirId(id) { currentDirId.value = id }

  // ==================== 移动对话框（共用状态） ====================
  const moveVisible = ref(false)
  const moveFileTarget = ref(null)
  const moveDirTree = ref([])
  const moveTargetDirId = ref(null)
  const moveLoading = ref(false)
  const batchMoveItems = ref([])

  // ==================== 上传对话框 ====================
  const uploadVisible = ref(false)

  // ==================== 目录 CRUD 对话框 ====================
  const createDirVisible = ref(false)
  const createDirName = ref('')
  const createDirLoading = ref(false)

  const renameDirVisible = ref(false)
  const renameDirTarget = ref(null)
  const renameDirNewName = ref('')
  const renameDirLoading = ref(false)

  const renameFileVisible = ref(false)
  const renameFileTarget = ref(null)
  const renameFileNewName = ref('')
  const renameFileLoading = ref(false)

  return {
    // 导航
    currentDirId, breadcrumb, directories, files, loading, currentPath,
    navigateToDir, navigateToBreadcrumb, refreshCurrentDir,
    loadDirectories, loadFilesFn,
    setBreadcrumb, setCurrentDirId,
    // 列表
    allItems, onRowClick,
    // 筛选
    filterText, filterType, selectedItems, tableRef,
    fileTypeOptions, filteredItems, onSelectionChange,
    // 移动
    moveVisible, moveFileTarget, moveDirTree, moveTargetDirId, moveLoading, batchMoveItems,
    // 上传
    uploadVisible,
    // 对话框
    createDirVisible, createDirName, createDirLoading,
    renameDirVisible, renameDirTarget, renameDirNewName, renameDirLoading,
    renameFileVisible, renameFileTarget, renameFileNewName, renameFileLoading
  }
}
