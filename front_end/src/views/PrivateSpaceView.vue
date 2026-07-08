<template>
  <div class="app-layout">
    <Sidebar active-menu="private" />

    <div class="app-main">
      <!-- 顶栏 -->
      <div class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="item.dirId">
            <el-icon v-if="idx === 0"><Lock /></el-icon>
            <el-link type="primary" underline="never" @click="navigateToBreadcrumb(idx)">
              {{ item.dirName }}
            </el-link>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <div class="flex-center gap-8">
          <el-tag type="danger" effect="dark" size="small">
            <el-icon><Lock /></el-icon> 私密空间
          </el-tag>
        </div>
      </div>

      <div class="app-content">
        <!-- ========== 未启用 ========== -->
        <el-result
          v-if="!userStore.privateSpaceEnabled"
          icon="warning"
          title="私密空间未开启"
          sub-title="开启后将拥有独立的加密文件保险箱"
        >
          <template #extra>
            <el-button type="primary" @click="openEnableDialog">
              <el-icon><Key /></el-icon> 设置私密密码并开启
            </el-button>
          </template>
        </el-result>

        <!-- ========== 未验证密码 ========== -->
        <template v-else-if="!verified && !hasExistingPassword">
          <div style="max-width:400px;margin:80px auto;text-align:center">
            <el-icon :size="48" color="#f56c6c"><Lock /></el-icon>
            <h3 style="margin:16px 0 8px">私密空间已锁定</h3>
            <p class="text-muted" style="margin-bottom:20px">请输入私密空间密码以访问加密文件</p>
            <el-input v-model="pwdInput" type="password" show-password
              placeholder="私密空间密码" size="large" @keyup.enter="doVerify" />
            <el-button type="primary" size="large" :loading="verifyLoading"
              style="margin-top:12px;width:100%" @click="doVerify">
              解锁
            </el-button>
          </div>
        </template>

        <!-- ========== 已解锁：文件管理器 ========== -->
        <template v-else>
          <!-- 工具栏 -->
          <div class="toolbar">
            <el-button type="primary" @click="openCreateDir">
              <el-icon><FolderAdd /></el-icon> 新建文件夹
            </el-button>
            <el-button type="success" @click="openUpload">
              <el-icon><Upload /></el-icon> 上传（自动加密）
            </el-button>
            <el-button @click="refreshCurrentDir">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <div style="flex:1" />
            <el-button type="warning" plain size="small" @click="openDisableDialog">
              关闭私密空间
            </el-button>
          </div>

          <!-- 筛选 + 批量操作 -->
          <div class="flex-between mb-16" style="flex-wrap:wrap;gap:8px">
            <div class="flex-center gap-8">
              <el-input v-model="filterText" placeholder="搜索..." clearable
                style="width:200px" :prefix-icon="Search" size="default" />
              <el-select v-model="filterType" placeholder="类型" clearable style="width:120px" size="default">
                <el-option label="全部" value="" />
                <el-option label="文件夹" value="文件夹" />
                <el-option v-for="t in fileTypeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
            <div v-if="selectedItems.length > 0" class="flex-center gap-8">
              <span class="text-muted">已选 {{ selectedItems.length }} 项</span>
              <el-button size="default" @click="batchMove">
                <el-icon><Rank /></el-icon> 批量移动
              </el-button>
              <el-button size="default" type="warning" @click="batchDecrypt">
                <el-icon><Unlock /></el-icon> 批量移出
              </el-button>
              <el-button size="default" type="danger" @click="batchDelete">
                <el-icon><Delete /></el-icon> 批量删除
              </el-button>
            </div>
          </div>

          <!-- 统一文件/文件夹列表 -->
          <el-table
            :data="filteredItems"
            style="width:100%"
            v-loading="loading"
            empty-text="此目录为空"
            :default-sort="{ prop: 'order', order: 'ascending' }"
            @row-click="onRowClick"
            @selection-change="onSelectionChange"
            ref="tableRef"
          >
            <el-table-column type="selection" width="40" />
            <el-table-column label="名称" min-width="250" prop="name" sortable>
              <template #default="{ row }">
                <div class="flex-center gap-8">
                  <el-icon :size="20" :color="row.isDir ? '#e6a23c' : '#909399'">
                    <Folder v-if="row.isDir" />
                    <Document v-else />
                  </el-icon>
                  <span class="item-name" @click.stop="row.isDir && navigateToDir(row.id, row.name)">
                    {{ row.name }}
                  </span>
                  <el-tag v-if="!row.isDir" size="small" type="danger" effect="dark">
                    <el-icon :size="12"><Lock /></el-icon>
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100" prop="type" sortable>
              <template #default="{ row }">
                <el-tag size="small" :type="row.isDir ? 'warning' : 'info'">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="120" prop="size" sortable>
              <template #default="{ row }">
                {{ row.isDir ? '-' : userStore.formatSize(row.size) }}
              </template>
            </el-table-column>
            <el-table-column label="修改时间" width="180" prop="time" sortable>
              <template #default="{ row }">
                {{ formatTime(row.time) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <template v-if="row.isDir">
                  <el-button size="small" link @click.stop="openRenameDir(row.original)">
                    <el-icon><Edit /></el-icon> 重命名
                  </el-button>
                  <el-button size="small" link type="danger" @click.stop="deleteDirectory(row.original)">
                    <el-icon><Delete /></el-icon> 删除
                  </el-button>
                </template>
                <template v-else>
                  <el-button size="small" type="primary" link @click.stop="downloadFile(row.original)">
                    <el-icon><Download /></el-icon> 下载
                  </el-button>
                  <el-button size="small" link @click.stop="openRenameFile(row.original)">
                    <el-icon><Edit /></el-icon> 重命名
                  </el-button>
                  <el-dropdown trigger="click" @command="(cmd) => handleFileAction(cmd, row.original)">
                    <el-button size="small" link @click.stop>
                      <el-icon><MoreFilled /></el-icon>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="move">
                          <el-icon><Rank /></el-icon> 移动到...
                        </el-dropdown-item>
                        <el-dropdown-item command="delete">
                          <el-icon><Delete /></el-icon> 删除
                        </el-dropdown-item>
                        <el-dropdown-item command="decrypt" divided>
                          <el-icon><Unlock /></el-icon> 移出私密空间
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </div>

    <!-- ==================== 弹窗 ==================== -->
    <!-- 新建文件夹 -->
    <el-dialog v-model="createDirVisible" title="新建文件夹" width="400px">
      <el-input v-model="createDirName" placeholder="文件夹名称" maxlength="50" @keyup.enter="doCreateDir" />
      <template #footer>
        <el-button @click="createDirVisible = false">取消</el-button>
        <el-button type="primary" :loading="createDirLoading" @click="doCreateDir">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名文件 -->
    <el-dialog v-model="renameFileVisible" title="重命名文件" width="400px">
      <el-input v-model="renameFileNewName" placeholder="新文件名" @keyup.enter="doRenameFile" />
      <template #footer>
        <el-button @click="renameFileVisible = false">取消</el-button>
        <el-button type="primary" :loading="renameFileLoading" @click="doRenameFile">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名文件夹 -->
    <el-dialog v-model="renameDirVisible" title="重命名文件夹" width="400px">
      <el-input v-model="renameDirNewName" placeholder="新文件夹名" @keyup.enter="doRenameDir" />
      <template #footer>
        <el-button @click="renameDirVisible = false">取消</el-button>
        <el-button type="primary" :loading="renameDirLoading" @click="doRenameDir">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传 -->
    <UploadDialog v-model="uploadVisible" title="上传文件（自动加密）"
      :target-path="currentPath" :auto-encrypt="true" :show-encrypt="false"
      submit-text="上传（将自动加密）"
      @confirm="onUploadConfirm" />

    <!-- 选择移出目标目录 -->
    <DirPickerDialog v-model="decryptPickerVisible" title="移出私密空间 - 选择目标目录"
      :build-tree="buildNormalDirTree"
      @confirm="onDecryptTargetSelected" />

    <!-- 移动 -->
    <el-dialog v-model="moveVisible" :title="_batchMoveItems.length > 0 ? `批量移动 ${_batchMoveItems.length} 项` : '移动到...'" width="500px">
      <el-tree :data="moveDirTree" :props="{ label: 'dirName', children: 'children' }"
        node-key="dirId" highlight-current accordion @node-click="handleMoveTargetSelect" />
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" :loading="moveLoading" :disabled="!moveTargetDirId" @click="doMove">
          确定移动
        </el-button>
      </template>
    </el-dialog>

    <!-- 开启私密空间 -->
    <el-dialog v-model="enableVisible" title="开启私密空间" width="420px">
      <el-form ref="enableFormRef" :model="enableForm" :rules="enableRules" label-position="top">
        <el-form-item label="私密密码" prop="password">
          <el-input v-model="enableForm.password" type="password" show-password placeholder="请设置私密密码（至少6位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="enableForm.confirm" type="password" show-password placeholder="请再次输入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="enableVisible = false">取消</el-button>
        <el-button type="primary" :loading="enableLoading" @click="doEnable">确定开启</el-button>
      </template>
    </el-dialog>

    <!-- 关闭私密空间 -->
    <el-dialog v-model="disableVisible" title="关闭私密空间" width="420px">
      <el-alert type="error" title="关闭后私密密码将被清除，加密文件将无法访问"
        :closable="false" show-icon style="margin-bottom:16px" />
      <el-input v-model="disablePassword" type="password" show-password placeholder="请输入私密密码确认" />
      <template #footer>
        <el-button @click="disableVisible = false">取消</el-button>
        <el-button type="danger" :loading="disableLoading" @click="doDisable">确认关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Lock, Key, FolderAdd, Upload, Refresh, Folder, Document,
  Download, Edit, Delete, MoreFilled, Rank, Unlock, UploadFilled, Search, Close
} from '@element-plus/icons-vue'
import { privateSpaceAPI, directoryAPI, fileAPI, userAPI } from '../api'
import { useUserStore } from '../stores/user'
import { useTransferStore } from '../stores/transfer'
import UploadDialog from '../components/UploadDialog.vue'
import DirPickerDialog from '../components/DirPickerDialog.vue'
import Sidebar from '../components/Sidebar.vue'

const router = useRouter()
const userStore = useUserStore()
const transferStore = useTransferStore()

// ==================== 密码验证 ====================
const pwdInput = ref('')
const verifyLoading = ref(false)
const verified = ref(!!userStore.privatePassword)  // 会话内已解锁则跳过锁屏

// 如果 store 中已有密码，直接使用
const hasExistingPassword = ref(false)

async function doVerify() {
  if (!pwdInput.value) { ElMessage.warning('请输入私密密码'); return }
  verifyLoading.value = true
  try {
    await privateSpaceAPI.verify(pwdInput.value)
    userStore.setPrivatePassword(pwdInput.value)
    verified.value = true
    pwdInput.value = ''
    await initFileManager()
  } catch { /* ignore */ }
  finally { verifyLoading.value = false }
}

// ==================== 文件管理器 ====================
const currentDirId = ref(null)
const breadcrumb = ref([])
const currentPath = computed(() => breadcrumb.value.map(b => b.dirName).join(' / ') || '私密空间')
const directories = ref([])
const files = ref([])
const loading = ref(false)

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

function onSelectionChange(selection) {
  selectedItems.value = selection
}

// ==================== 批量操作 ====================

async function batchDelete() {
  if (selectedItems.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedItems.value.length} 个文件/文件夹吗？`,
      '批量删除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  let ok = 0
  for (const item of selectedItems.value) {
    try {
      if (item.isDir) {
        await directoryAPI.remove(item.id)
      } else {
        await fileAPI.remove(item.id)
      }
      ok++
    } catch { /* skip */ }
  }
  ElMessage.success(`成功删除 ${ok} 项`)
  await refreshCurrentDir()
}

const _batchMoveItems = ref([])

async function batchMove() {
  if (selectedItems.value.length === 0) return
  moveFileTarget.value = null
  _batchMoveItems.value = selectedItems.value
  moveTargetDirId.value = null
  moveDirTree.value = await buildPrivateDirTree(userStore.privateSpaceRootDirId)
  moveVisible.value = true
}

async function batchDecrypt() {
  if (selectedItems.value.length === 0) return
  const filesToDecrypt = selectedItems.value.filter(i => !i.isDir)
  if (filesToDecrypt.length === 0) {
    ElMessage.warning('请至少选择一个文件')
    return
  }
  openDecryptPicker(filesToDecrypt)
}

async function navigateToDir(dirId, dirName) {
  currentDirId.value = dirId
  breadcrumb.value.push({ dirId: String(dirId), dirName })
  directories.value = []; files.value = []; loading.value = true
  await loadDirectories(dirId)
  await loadFiles(dirId)
}

async function navigateToBreadcrumb(index) {
  const target = breadcrumb.value[index]
  breadcrumb.value = breadcrumb.value.slice(0, index + 1)
  currentDirId.value = Number(target.dirId)
  directories.value = []; files.value = []; loading.value = true
  await loadDirectories(Number(target.dirId))
  await loadFiles(Number(target.dirId))
}

async function loadDirectories(parentId) {
  try {
    const res = await privateSpaceAPI.listDirectories(parentId)
    directories.value = res.data.data || []
  } catch { directories.value = [] }
}

async function loadFiles(dirId) {
  if (!dirId) { files.value = []; return }
  loading.value = true
  try {
    const res = await privateSpaceAPI.listFiles(dirId)
    files.value = res.data.data || []
  } catch { files.value = [] }
  finally { loading.value = false }
}

async function refreshCurrentDir() {
  await loadDirectories(currentDirId.value)
  if (currentDirId.value) await loadFiles(currentDirId.value)
}

async function initFileManager() {
  try {
    const statusRes = await privateSpaceAPI.status()
    const statusData = statusRes.data.data
    // 更新 store 中的 rootDirId
    if (statusData?.rootDirId) {
      userStore.privateSpaceRootDirId = statusData.rootDirId
    }
    const rootDirId = statusData?.rootDirId || userStore.privateSpaceRootDirId
    if (rootDirId) {
      const rootId = Number(rootDirId)
      currentDirId.value = rootId
      breadcrumb.value = [{ dirId: String(rootId), dirName: '私密空间' }]
      await loadDirectories(rootId)
      await loadFiles(rootId)
    } else {
      ElMessage.error('私密空间目录未找到，请尝试关闭后重新开启私密空间')
    }
  } catch {
    ElMessage.error('加载私密空间失败')
  }
}

// ==================== 目录操作 ====================
const createDirVisible = ref(false)
const createDirName = ref('')
const createDirLoading = ref(false)

function openCreateDir() { createDirName.value = ''; createDirVisible.value = true }

async function doCreateDir() {
  if (!createDirName.value.trim()) return
  createDirLoading.value = true
  try {
    await directoryAPI.create(createDirName.value.trim(), currentDirId.value)
    ElMessage.success('已创建')
    createDirVisible.value = false
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { createDirLoading.value = false }
}

const renameDirVisible = ref(false)
const renameDirTarget = ref(null)
const renameDirNewName = ref('')
const renameDirLoading = ref(false)

function openRenameDir(dir) {
  renameDirTarget.value = dir; renameDirNewName.value = dir.dirName; renameDirVisible.value = true
}

async function doRenameDir() {
  if (!renameDirNewName.value.trim()) return
  renameDirLoading.value = true
  try {
    await directoryAPI.rename(renameDirTarget.value.dirId, renameDirNewName.value.trim())
    ElMessage.success('已重命名')
    renameDirVisible.value = false
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { renameDirLoading.value = false }
}

async function deleteDirectory(dir) {
  try {
    await ElMessageBox.confirm(`确定要删除文件夹「${dir.dirName}」吗？`, '删除文件夹',
      { type: 'warning' })
  } catch { return }
  try {
    await directoryAPI.remove(dir.dirId)
    ElMessage.success('已删除')
    await refreshCurrentDir()
  } catch { /* ignore */ }
}

// ==================== 文件操作 ====================
const renameFileVisible = ref(false)
const renameFileTarget = ref(null)
const renameFileNewName = ref('')
const renameFileLoading = ref(false)

function openRenameFile(file) {
  renameFileTarget.value = file; renameFileNewName.value = file.fileName; renameFileVisible.value = true
}

async function doRenameFile() {
  if (!renameFileNewName.value.trim()) return
  renameFileLoading.value = true
  try {
    await fileAPI.rename(renameFileTarget.value.fileId, renameFileNewName.value.trim())
    ElMessage.success('已重命名')
    renameFileVisible.value = false
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { renameFileLoading.value = false }
}

const uploadVisible = ref(false)

function openUpload() {
  uploadVisible.value = true
}

async function onUploadConfirm({ files, packMethod, compressMethod, tarName }) {
  if (files.length === 0) return
  if (!currentDirId.value) {
    ElMessage.error('目录信息未加载，请刷新页面重试')
    return
  }
  try {
    if (packMethod === 'none') {
      let targetDirId = currentDirId.value
      const firstRp = files[0].relativePath
      if (firstRp && firstRp.includes('/')) {
        try {
          const res = await directoryAPI.create(firstRp.split('/')[0], currentDirId.value)
          targetDirId = Number(res.data.data.dirId)
        } catch { /* skip */ }
      }
      const token = localStorage.getItem('token')
      const base = import.meta.env.PROD ? '' : '/api'
      for (const f of files) {
        const fd = new FormData()
        fd.append('dirId', targetDirId)
        fd.append('files', new File([f.raw], f.name, { type: f.raw.type || 'application/octet-stream' }))
        fd.append('relativePaths', f.name)
        fd.append('encrypt', 'true'); fd.append('privatePassword', userStore.privatePassword)
        fd.append('packMethod', 'none')
        fd.append('compressMethod', compressMethod)
        transferStore.addUpload(f.name, f.size, () => {
          const xhr = new XMLHttpRequest()
          xhr.open('POST', base + '/user/file/upload')
          if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
          return { xhr, body: fd }
        })
      }
    } else {
      const finalName = tarName || (files[0].name.replace(/\.[^.]+$/, '') + '.tar')
      const fd = new FormData()
      fd.append('dirId', currentDirId.value)
      files.forEach(f => fd.append('files', f.raw))
      files.forEach(f => fd.append('relativePaths', f.relativePath || f.name))
      fd.append('encrypt', 'true'); fd.append('privatePassword', userStore.privatePassword)
      fd.append('packMethod', 'tar')
      fd.append('compressMethod', compressMethod)
      fd.append('displayName', finalName)
      const token = localStorage.getItem('token')
      const base = import.meta.env.PROD ? '' : '/api'
      transferStore.addUpload(finalName, files.reduce((s, f) => s + f.size, 0), () => {
        const xhr = new XMLHttpRequest()
        xhr.open('POST', base + '/user/file/upload')
        if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
        return { xhr, body: fd }
      })
    }
    uploadVisible.value = false
    ElMessage.success('已加入传输队列')
  } catch { /* ignore */ }
}

// 下载
async function downloadFile(file) {
  const ok = await transferStore.addDownloadWithPicker(file.fileName, file.fileSize, file.fileId, userStore.privatePassword)
  if (ok) ElMessage.success('已加入传输队列')
}

// 移动
const moveVisible = ref(false)
const moveFileTarget = ref(null)
const moveDirTree = ref([])
const moveTargetDirId = ref(null)
const moveLoading = ref(false)

async function openMove(file) {
  moveFileTarget.value = file
  _batchMoveItems.value = []
  moveTargetDirId.value = null
  moveDirTree.value = await buildPrivateDirTree(userStore.privateSpaceRootDirId)
  moveVisible.value = true
}

async function buildPrivateDirTree(parentId) {
  try {
    const res = await privateSpaceAPI.listDirectories(parentId)
    const dirs = res.data.data || []
    const tree = []
    for (const d of dirs) {
      tree.push({ dirId: Number(d.dirId), dirName: d.dirName, children: await buildPrivateDirTree(d.dirId) })
    }
    return tree
  } catch { return [] }
}

function handleMoveTargetSelect(node) { moveTargetDirId.value = node.dirId }

async function doMove() {
  // 批量移动
  if (_batchMoveItems.value.length > 0) {
    if (!moveTargetDirId.value) { ElMessage.warning('请选择目标文件夹'); return }
    moveLoading.value = true
    let ok = 0
    for (const item of _batchMoveItems.value) {
      try {
        if (!item.isDir) await fileAPI.move(item.id, moveTargetDirId.value)
        ok++
      } catch { /* skip */ }
    }
    moveLoading.value = false
    ElMessage.success(`成功移动 ${ok} 项`)
    moveVisible.value = false
    _batchMoveItems.value = []
    await refreshCurrentDir()
    return
  }
  // 单文件移动
  if (!moveTargetDirId.value) { ElMessage.warning('请选择目标文件夹'); return }
  moveLoading.value = true
  try {
    await fileAPI.move(moveFileTarget.value.fileId, moveTargetDirId.value)
    ElMessage.success('已移动')
    moveVisible.value = false
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { moveLoading.value = false }
}

// 移出私密空间：选择目标目录
const decryptPickerVisible = ref(false)
let _decryptTarget = null

function openDecryptPicker(target) {
  _decryptTarget = target
  decryptPickerVisible.value = true
}

async function buildNormalDirTree(parentId) {
  const res = await directoryAPI.list(parentId)
  const dirs = (res.data.data || []).filter(d => d.dirName !== '私密空间')
  const tree = []
  for (const d of dirs) {
    tree.push({
      dirId: Number(d.dirId), dirName: d.dirName,
      children: await buildNormalDirTree(d.dirId)
    })
  }
  return tree
}

async function onDecryptTargetSelected(targetDirId) {
  const targets = Array.isArray(_decryptTarget) ? _decryptTarget
    : (_decryptTarget ? [_decryptTarget] : [])
  let ok = 0
  for (const item of targets) {
    try {
      await fileAPI.decrypt(item.id || item.fileId, userStore.privatePassword, targetDirId)
      ok++
    } catch { /* skip */ }
  }
  ElMessage.success(`成功移出 ${ok} 个文件`)
  _decryptTarget = null
  await refreshCurrentDir()
}

async function moveOutPrivateSpace(file) {
  try {
    await ElMessageBox.confirm(`确定要将「${file.fileName}」移出私密空间吗？`, '移出私密空间', { type: 'warning' })
  } catch { return }
  openDecryptPicker([file])
}

// 删除
async function deleteFile(file) {
  try {
    await ElMessageBox.confirm(`确定要将「${file.fileName}」移入回收站吗？`, '删除', { type: 'warning' })
  } catch { return }
  try {
    await fileAPI.remove(file.fileId)
    ElMessage.success('已移入回收站')
    await refreshCurrentDir()
  } catch { /* ignore */ }
}

function handleFileAction(cmd, file) {
  switch (cmd) {
    case 'move': openMove(file); break
    case 'delete': deleteFile(file); break
    case 'decrypt': moveOutPrivateSpace(file); break
  }
}

// ==================== 开启/关闭 ====================
const enableVisible = ref(false)
const enableFormRef = ref(null)
const enableForm = reactive({ password: '', confirm: '' })
const enableLoading = ref(false)
const enableRules = {
  password: [{ required: true, message: '请设置密码', trigger: 'blur' }, { min: 6, message: '至少6位', trigger: 'blur' }],
  confirm: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (r, v, cb) => v !== enableForm.password ? cb(new Error('两次密码不一致')) : cb(), trigger: 'blur' }
  ]
}

function openEnableDialog() { enableForm.password = ''; enableForm.confirm = ''; enableVisible.value = true }

async function doEnable() {
  const valid = await enableFormRef.value.validate().catch(() => false)
  if (!valid) return
  enableLoading.value = true
  try {
    await privateSpaceAPI.enable(enableForm.password)
    ElMessage.success('私密空间已开启')
    enableVisible.value = false
    userStore.setPrivateSpaceStatus(true)
    userStore.setPrivatePassword(enableForm.password)
    verified.value = true
    await initFileManager()
  } catch { /* ignore */ }
  finally { enableLoading.value = false }
}

const disableVisible = ref(false)
const disablePassword = ref('')
const disableLoading = ref(false)

function openDisableDialog() { disablePassword.value = ''; disableVisible.value = true }

async function doDisable() {
  if (!disablePassword.value) return
  disableLoading.value = true
  try {
    await privateSpaceAPI.disable(disablePassword.value)
    ElMessage.success('私密空间已关闭')
    disableVisible.value = false
    userStore.setPrivateSpaceStatus(false)
    userStore.clearPrivateAccess()
    verified.value = false
  } catch { /* ignore */ }
  finally { disableLoading.value = false }
}

// ==================== 初始化 ====================
function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

onMounted(async () => {
  // 刷新私密空间状态
  try {
    const res = await privateSpaceAPI.status()
    const data = res.data.data
    userStore.setPrivateSpaceStatus(data?.enabled ?? false, data?.rootDirId)
  } catch { /* ignore */ }
  // 刷新存储信息
  try {
    const infoRes = await userAPI.getUserInfo(userStore.userId)
    userStore.setStorageInfo(infoRes.data.data.storage)
  } catch { /* ignore */ }

  // 如果已启用且有缓存密码，直接进入
  if (userStore.privateSpaceEnabled && userStore.privatePassword) {
    hasExistingPassword.value = true
    verified.value = true
    await initFileManager()
  }
})
</script>
