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
    <el-dialog v-model="uploadVisible" title="上传文件（自动加密）" width="520px" @closed="clearUploadFiles">
      <el-form label-position="top">
        <el-form-item label="选择文件或文件夹">
          <div class="flex-center gap-8" style="margin-bottom:8px">
            <input ref="fileInputRef" type="file" multiple style="display:none"
              @change="onFileInputChange" />
            <input ref="folderInputRef" type="file" webkitdirectory style="display:none"
              @change="onFolderInputChange" />
            <el-button @click="fileInputRef.click()">
              <el-icon><Document /></el-icon> 选择文件
            </el-button>
            <el-button @click="folderInputRef.click()">
              <el-icon><Folder /></el-icon> 选择文件夹
            </el-button>
          </div>
          <div v-if="uploadFiles.length > 0"
            style="max-height:200px;overflow-y:auto;border:1px solid #e4e7ed;border-radius:6px;padding:8px">
            <div v-for="(f, i) in uploadFiles" :key="i"
              class="flex-between" style="padding:4px 0;border-bottom:1px solid #f0f0f0">
              <span style="font-size:13px">{{ f.relativePath || f.name }}</span>
              <span class="text-muted" style="font-size:12px">{{ formatUploadSize(f.size) }}</span>
              <el-button size="small" text type="danger" @click="removeUploadFile(i)">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>
          <div v-else style="padding:24px;text-align:center;border:1px dashed #d9d9d9;border-radius:6px;color:#c0c4cc">
            请选择文件或文件夹
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="打包方式">
              <el-select v-model="uploadPackMethod" style="width:100%" @change="onPackMethodChange">
                <el-option label="不打包" value="none" />
                <el-option label="tar 归档" value="tar" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="压缩算法">
              <el-select v-model="uploadCompressMethod" style="width:100%">
                <el-option label="LZ77" value="lz77" />
                <el-option label="Huffman" value="huffman" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="uploadPackMethod === 'tar'" label="归档文件名">
          <el-input v-model="uploadTarName" placeholder="默认：第一个文件名.tar" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" :disabled="uploadFiles.length === 0" @click="doUpload">
          上传（{{ uploadFiles.length }} 个，自动加密）
        </el-button>
      </template>
    </el-dialog>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Lock, Key, FolderAdd, Upload, Refresh, Folder, Document,
  Download, Edit, Delete, MoreFilled, Rank, Unlock, UploadFilled, Search, Close
} from '@element-plus/icons-vue'
import { privateSpaceAPI, directoryAPI, fileAPI, userAPI } from '../api'
import { useUserStore } from '../stores/user'
import Sidebar from '../components/Sidebar.vue'

const userStore = useUserStore()

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
  try {
    await ElMessageBox.confirm(
      `确定将 ${filesToDecrypt.length} 个文件移出私密空间吗？`,
      '批量移出私密空间', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  let ok = 0
  for (const item of filesToDecrypt) {
    try {
      await fileAPI.decrypt(item.id, userStore.privatePassword)
      ok++
    } catch { /* skip */ }
  }
  ElMessage.success(`成功移出 ${ok} 个文件`)
  await refreshCurrentDir()
}

async function navigateToDir(dirId, dirName) {
  currentDirId.value = dirId
  breadcrumb.value.push({ dirId: String(dirId), dirName })
  await loadDirectories(dirId)
  await loadFiles(dirId)
}

async function navigateToBreadcrumb(index) {
  const target = breadcrumb.value[index]
  breadcrumb.value = breadcrumb.value.slice(0, index + 1)
  currentDirId.value = Number(target.dirId)
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

// 上传（自动加密）
const uploadVisible = ref(false)
const uploadFiles = ref([])
const fileInputRef = ref(null)
const folderInputRef = ref(null)
const uploadPackMethod = ref('none')
const uploadTarName = ref('')
const uploadCompressMethod = ref('lz77')
const uploadLoading = ref(false)

function openUpload() {
  uploadFiles.value = []
  uploadPackMethod.value = 'none'
  uploadCompressMethod.value = 'lz77'
  uploadTarName.value = ''
  uploadVisible.value = true
}

function onPackMethodChange(val) {
  if (val === 'tar' && uploadFiles.value.length > 0) {
    uploadTarName.value = uploadFiles.value[0].name.replace(/\.[^.]+$/, '') + '.tar'
  }
}

function ensureTarSuffix(name) {
  return name.endsWith('.tar') ? name : name + '.tar'
}

function clearUploadFiles() {
  uploadFiles.value = []
  if (fileInputRef.value) fileInputRef.value.value = ''
  if (folderInputRef.value) folderInputRef.value.value = ''
}

function onFileInputChange(e) {
  for (const f of e.target.files) {
    uploadFiles.value.push({ name: f.name, size: f.size, relativePath: f.webkitRelativePath || f.name, raw: f })
  }
  e.target.value = ''
}

function onFolderInputChange(e) {
  for (const f of e.target.files) {
    uploadFiles.value.push({ name: f.name, size: f.size, relativePath: f.webkitRelativePath, raw: f })
  }
  e.target.value = ''
}

function removeUploadFile(index) {
  uploadFiles.value.splice(index, 1)
}

function formatUploadSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

async function doUpload() {
  if (uploadFiles.value.length === 0) return
  if (!currentDirId.value) {
    ElMessage.error('目录信息未加载，请刷新页面重试')
    return
  }
  uploadLoading.value = true
  try {
    if (uploadPackMethod.value === 'none') {
      let targetDirId = currentDirId.value
      const firstRp = uploadFiles.value[0].relativePath
      if (firstRp && firstRp.includes('/')) {
        const folderName = firstRp.split('/')[0]
        try {
          const res = await directoryAPI.create(folderName, currentDirId.value)
          targetDirId = Number(res.data.data.dirId)
        } catch { /* skip */ }
      }
      let ok = 0
      for (const f of uploadFiles.value) {
        try {
          const cleanFile = new File([f.raw], f.name, { type: f.raw.type || 'application/octet-stream' })
          await fileAPI.upload(targetDirId, cleanFile, true, userStore.privatePassword, 'none', uploadCompressMethod.value)
          ok++
        } catch { /* skip */ }
      }
      ElMessage.success(`成功上传 ${ok} 个文件`)
    } else {
      await fileAPI.multiUpload(currentDirId.value, uploadFiles.value, true, userStore.privatePassword,
        'tar', uploadCompressMethod.value,
        uploadTarName.value ? ensureTarSuffix(uploadTarName.value) : undefined)
      ElMessage.success('已加密上传')
    }
    uploadVisible.value = false
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { uploadLoading.value = false }
}

// 下载
async function downloadFile(file) {
  try {
    const res = await fileAPI.download(file.fileId, userStore.privatePassword)
    const blob = new Blob([res.data])

    if (window.showSaveFilePicker) {
      try {
        const handle = await window.showSaveFilePicker({
          suggestedName: file.fileName,
          types: [{ description: '文件', accept: { 'application/octet-stream': ['.' + (file.fileType || 'dat')] } }]
        })
        const writable = await handle.createWritable()
        await writable.write(blob)
        await writable.close()
        ElMessage.success('文件已保存')
        return
      } catch (e) {
        if (e.name === 'AbortError') return  // 用户取消
      }
    }

    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = file.fileName
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch { /* ignore */ }
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

// 移出私密空间
async function moveOutPrivateSpace(file) {
  try {
    await ElMessageBox.confirm(`确定要将「${file.fileName}」移出私密空间吗？`, '移出私密空间', { type: 'warning' })
  } catch { return }
  try {
    await fileAPI.decrypt(file.fileId, userStore.privatePassword)
    ElMessage.success('已移出私密空间，文件已恢复到原始目录')
    await refreshCurrentDir()
  } catch { /* ignore */ }
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
