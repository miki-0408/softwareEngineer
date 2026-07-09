<template>
  <div class="app-layout">
    <!-- ==================== 侧边栏 ==================== -->
    <Sidebar :active-menu="'files'" />

    <!-- ==================== 主区域 ==================== -->
    <div class="app-main">
      <!-- 顶栏 -->
      <div class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="item.dirId">
            <el-icon v-if="idx === 0"><HomeFilled /></el-icon>
            <el-link type="primary" underline="never" @click="navigateToBreadcrumb(idx)">
              {{ item.dirName }}
            </el-link>
          </el-breadcrumb-item>
        </el-breadcrumb>

        <div class="flex-center gap-8">
          <!-- 存储空间条 -->
          <el-popover placement="bottom" :width="260" trigger="hover">
            <template #reference>
              <el-tag size="large" type="info" effect="plain" style="cursor:pointer">
                存储 {{ userStore.formatSize(userStore.usedSpace) }} / {{ userStore.formatSize(userStore.totalSpace) }}
              </el-tag>
            </template>
            <div>
              <div class="flex-between mb-16">
                <span>已用空间</span>
                <span>{{ ((userStore.usedSpace / userStore.totalSpace) * 100).toFixed(1) }}%</span>
              </div>
              <el-progress
                :percentage="Math.round((userStore.usedSpace / userStore.totalSpace) * 100)"
                :stroke-width="12"
                :color="storageProgressColor"
              />
            </div>
          </el-popover>

          <el-button @click="openProfileDialog">
            <el-icon><User /></el-icon> 个人信息
          </el-button>
          <el-button @click="openPasswordDialog">
            <el-icon><Key /></el-icon> 修改密码
          </el-button>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="app-content">
        <!-- 工具栏 -->
        <div class="toolbar">
          <el-button type="primary" @click="openCreateDir">
            <el-icon><FolderAdd /></el-icon> 新建文件夹
          </el-button>
          <el-button type="success" @click="openUpload">
            <el-icon><Upload /></el-icon> 上传文件
          </el-button>
          <el-button @click="refreshCurrentDir">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>

        <!-- 筛选 + 批量操作 -->
        <div class="flex-between mb-16" style="flex-wrap:wrap;gap:8px">
          <div class="flex-center gap-8">
            <el-input v-model="filterText" placeholder="搜索文件或文件夹..." clearable
              style="width:240px" :prefix-icon="Search" size="default" />
            <el-select v-model="filterType" placeholder="类型筛选" clearable style="width:140px" size="default">
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
            <el-button size="default" type="warning" @click="batchEncrypt">
              <el-icon><Lock /></el-icon> 移入私密空间
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
                <el-icon :size="20" :color="row.isDir ? '#e6a23c' : getFileIconColor(row.original)">
                  <Folder v-if="row.isDir" />
                  <Document v-else />
                </el-icon>
                <span class="item-name" @click.stop="row.isDir && navigateToDir(row.id, row.name)">
                  {{ row.name }}
                </span>
                <el-tag v-if="!row.isDir && row.original.isEncrypted === 1" size="small" type="danger" effect="dark">
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
                      <el-dropdown-item command="encrypt" divided>
                        <el-icon><Lock /></el-icon> 移入私密空间
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ==================== 弹窗 ==================== -->

    <!-- 新建文件夹 -->
    <el-dialog v-model="createDirVisible" title="新建文件夹" width="420px" @opened="focusCreateInput">
      <el-form @submit.prevent="doCreateDir">
        <el-form-item label="文件夹名称">
          <el-input ref="createDirInput" v-model="createDirName" placeholder="请输入文件夹名称"
            :prefix-icon="Folder" maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDirVisible = false">取消</el-button>
        <el-button type="primary" :loading="createDirLoading" @click="doCreateDir">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名文件 -->
    <el-dialog v-model="renameFileVisible" title="重命名文件" width="420px">
      <el-form @submit.prevent="doRenameFile">
        <el-form-item label="新文件名">
          <el-input v-model="renameFileNewName" placeholder="请输入新文件名"
            maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameFileVisible = false">取消</el-button>
        <el-button type="primary" :loading="renameFileLoading" @click="doRenameFile">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重命名文件夹 -->
    <el-dialog v-model="renameDirVisible" title="重命名文件夹" width="420px">
      <el-form @submit.prevent="doRenameDir">
        <el-form-item label="新文件夹名">
          <el-input v-model="renameDirNewName" placeholder="请输入新文件夹名"
            maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDirVisible = false">取消</el-button>
        <el-button type="primary" :loading="renameDirLoading" @click="doRenameDir">确定</el-button>
      </template>
    </el-dialog>

    <!-- 上传文件 -->
    <UploadDialog v-model="uploadVisible" :target-path="currentBreadcrumbPath" show-encrypt
      :submit-text="'上传'"
      @confirm="onUploadConfirm" />

    <!-- 选择私密空间目标目录 -->
    <DirPickerDialog v-model="encryptPickerVisible" title="移入私密空间 - 选择目标目录"
      :build-tree="buildPrivateDirTree"
      @confirm="onEncryptTargetSelected" />

    <!-- 移动文件 -->
    <el-dialog v-model="moveVisible" :title="_batchMoveItems.length > 0 ? `批量移动 ${_batchMoveItems.length} 项` : '移动到...'" width="500px">
      <el-tree
        :data="moveDirTree"
        :props="{ label: 'dirName', children: 'children' }"
        node-key="dirId"
        highlight-current
        accordion
        @node-click="handleMoveTargetSelect"
      />
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" :loading="moveLoading" :disabled="!moveTargetDirId"
          @click="doMove">确定移动</el-button>
      </template>
    </el-dialog>

    <!-- 个人信息 -->
    <el-dialog v-model="profileVisible" title="个人信息" width="440px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
        <el-form-item label="用户名" prop="newUsername">
          <el-input v-model="profileForm.newUsername" placeholder="请输入新用户名" maxlength="100" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.newGender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
            <el-radio value="未知">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="头像">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="handleProfileAvatarChange"
            :on-remove="handleProfileAvatarRemove"
          >
            <el-button size="small" type="primary" plain>选择新头像</el-button>
            <template #tip><span class="text-muted" style="margin-left:8px">留空则不修改</span></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="profileLoading" @click="doUpdateProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-position="top">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmNew">
          <el-input v-model="pwdForm.confirmNew" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="doChangePassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 私密空间密码输入 -->
    <el-dialog v-model="privatePwdVisible" title="私密空间验证" width="400px">
      <p class="mb-16 text-muted">此操作需要私密空间密码</p>
      <el-input v-model="privatePassword" type="password" show-password
        placeholder="请输入私密空间密码" @keyup.enter="resolvePrivatePwd" />
      <template #footer>
        <el-button @click="cancelPrivatePwd">取消</el-button>
        <el-button type="primary" @click="resolvePrivatePwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  HomeFilled, FolderAdd, Upload, Refresh, Folder, Document,
  Download, Edit, Delete, MoreFilled, Rank, Lock, Unlock, Key, UploadFilled, Search, User, Close
} from '@element-plus/icons-vue'
import { directoryAPI, fileAPI } from '../api'
import { useUserStore } from '../stores/user'
import { useTransferStore } from '../stores/transfer'
import UploadDialog from '../components/UploadDialog.vue'
import DirPickerDialog from '../components/DirPickerDialog.vue'
import Sidebar from '../components/Sidebar.vue'

const router = useRouter()
const userStore = useUserStore()
const transferStore = useTransferStore()

// ==================== 目录导航（手动面包屑栈） ====================
const currentDirId = ref(null)
const breadcrumb = ref([])       // [{ dirId, dirName }], 首项为根目录 "我的文件"
const directories = ref([])

const currentBreadcrumbPath = computed(() => {
  return breadcrumb.value.map(b => b.dirName).join(' / ') || '我的文件'
})

/** 点击目录列表中的目录 → 进入子目录 */
async function navigateToDir(dirId, dirName) {
  currentDirId.value = dirId
  breadcrumb.value.push({ dirId: String(dirId), dirName })
  directories.value = []; files.value = []; loading.value = true
  await loadDirectories(dirId)
  await loadFiles(dirId)
}

/** 点击面包屑中的某一级 → 跳回该级 */
async function navigateToBreadcrumb(index) {
  const target = breadcrumb.value[index]
  breadcrumb.value = breadcrumb.value.slice(0, index + 1)
  currentDirId.value = Number(target.dirId)
  directories.value = []; files.value = []; loading.value = true
  await loadDirectories(Number(target.dirId))
  await loadFiles(Number(target.dirId))
}

/** 回到根目录 → 自动进入用户的 "我的文件" */
async function navigateToRoot() {
  try {
    const res = await directoryAPI.list(null)
    const dirs = res.data.data || []
    const rootDir = dirs.find(d => d.dirName === '我的文件')
    if (rootDir) {
      currentDirId.value = Number(rootDir.dirId)
      breadcrumb.value = [{ dirId: rootDir.dirId, dirName: rootDir.dirName }]
      await loadDirectories(Number(rootDir.dirId))
      await loadFiles(Number(rootDir.dirId))
    } else {
      currentDirId.value = null
      breadcrumb.value = []
      await loadDirectories(null)
      files.value = []
    }
  } catch {
    currentDirId.value = null
    breadcrumb.value = []
  }
  loading.value = false
}

// ==================== 数据加载 ====================
const files = ref([])
const loading = ref(false)

// 统一列表：文件夹优先（order=0），文件在后（order=1）
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

// 从当前文件列表中提取已有的文件类型
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
    } catch { /* skip failures */ }
  }
  ElMessage.success(`成功删除 ${ok} 项`)
  await refreshCurrentDir()
}

async function batchMove() {
  if (selectedItems.value.length === 0) return
  moveFileTarget.value = null  // batch mode
  _batchMoveItems.value = selectedItems.value
  moveTargetDirId.value = null
  moveDirTree.value = await buildDirTree(null)
  moveVisible.value = true
}

const _batchMoveItems = ref([])

async function doMove() {
  // 兼容单文件移动和批量移动
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
    _batchMoveItems.value = []
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { moveLoading.value = false }
}

async function batchEncrypt() {
  if (selectedItems.value.length === 0) return
  const filesToEncrypt = selectedItems.value.filter(i => !i.isDir)
  if (filesToEncrypt.length === 0) {
    ElMessage.warning('请至少选择一个文件')
    return
  }
  openEncryptPicker(filesToEncrypt)
}

async function loadDirectories(parentId) {
  try {
    const res = await directoryAPI.list(parentId)
    directories.value = res.data.data || []
  } catch { directories.value = [] }
}

async function loadFiles(dirId) {
  if (!dirId) { files.value = []; return }
  loading.value = true
  try {
    const res = await fileAPI.list(dirId)
    files.value = res.data.data || []
  } catch { files.value = [] }
  finally { loading.value = false }
}

async function refreshCurrentDir() {
  await loadDirectories(currentDirId.value)
  if (currentDirId.value) await loadFiles(currentDirId.value)
}

async function init() {
  try {
    const res = await import('../api').then(m => m.userAPI.getUserInfo(userStore.userId))
    const info = res.data.data
    userStore.setStorageInfo(info.storage)
    userStore.setPrivateSpaceStatus(info.privateSpace ? info.privateSpace.enabled : false)
  } catch { /* ignore */ }

  await navigateToRoot()
}

// ==================== 目录操作 ====================
const createDirVisible = ref(false)
const createDirName = ref('')
const createDirLoading = ref(false)
const createDirInput = ref(null)

function openCreateDir() {
  createDirName.value = ''
  createDirVisible.value = true
}

function focusCreateInput() {
  nextTick(() => createDirInput.value?.focus())
}

async function doCreateDir() {
  if (!createDirName.value.trim()) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  createDirLoading.value = true
  try {
    await directoryAPI.create(createDirName.value.trim(),
      currentDirId.value || undefined)
    ElMessage.success('文件夹已创建')
    createDirVisible.value = false
    await refreshCurrentDir()
  } catch { /* 拦截器处理 */ }
  finally { createDirLoading.value = false }
}

const renameDirVisible = ref(false)
const renameDirTarget = ref(null)
const renameDirNewName = ref('')
const renameDirLoading = ref(false)

function openRenameDir(dir) {
  renameDirTarget.value = dir
  renameDirNewName.value = dir.dirName
  renameDirVisible.value = true
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
    await ElMessageBox.confirm(
      `确定要删除文件夹「${dir.dirName}」吗？只有空文件夹才能被删除。`,
      '删除文件夹',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
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
  renameFileTarget.value = file
  renameFileNewName.value = file.fileName
  renameFileVisible.value = true
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

async function onUploadConfirm({ files, packMethod, compressMethod, tarName, encrypt }) {
  if (files.length === 0) return
  if (encrypt && !userStore.privateSpaceEnabled) {
    ElMessage.warning('私密空间未开启，无法加密上传。')
    return
  }
  const pwd = encrypt ? (userStore.privatePassword || await requestPrivatePassword()) : undefined
  if (encrypt && !pwd) return
  if (encrypt && !userStore.privatePassword) userStore.setPrivatePassword(pwd)

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
        if (encrypt) { fd.append('encrypt', 'true'); fd.append('privatePassword', pwd) }
        fd.append('packMethod', 'none')
        fd.append('compressMethod', compressMethod)
        transferStore.addUpload(f.name, f.size, () => {
          const xhr = new XMLHttpRequest()
          xhr.open('POST', base + '/user/file/upload')
          if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token)
          return { xhr, body: fd }
        }, (success) => { if (success) refreshCurrentDir() })
      }
    } else {
      const finalName = tarName || (files[0].name.replace(/\.[^.]+$/, '') + '.tar')
      const fd = new FormData()
      fd.append('dirId', currentDirId.value)
      files.forEach(f => fd.append('files', f.raw))
      files.forEach(f => fd.append('relativePaths', f.relativePath || f.name))
      if (encrypt) { fd.append('encrypt', 'true'); fd.append('privatePassword', pwd) }
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
      }, (success) => { if (success) refreshCurrentDir() })
    }
    uploadVisible.value = false
    ElMessage.success('已加入传输队列')
  } catch { /* ignore */ }
}

// 下载
async function downloadFile(file) {
  let pwd = undefined
  if (file.isEncrypted === 1) {
    pwd = userStore.privatePassword || await requestPrivatePassword()
    if (!pwd) return
  }
  const ok = await transferStore.addDownloadWithPicker(file.fileName, file.fileSize, file.fileId, pwd)
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
  moveDirTree.value = await buildDirTree(null)
  moveVisible.value = true
}

async function buildDirTree(parentId) {
  const res = await directoryAPI.list(parentId)
  const dirs = (res.data.data || []).filter(d => d.dirName !== '私密空间')
  const tree = []
  for (const d of dirs) {
    tree.push({
      dirId: Number(d.dirId),
      dirName: d.dirName,
      children: await buildDirTree(d.dirId)
    })
  }
  return tree
}

function handleMoveTargetSelect(node) {
  moveTargetDirId.value = node.dirId
}

// 删除（移到回收站）
async function deleteFile(file) {
  try {
    await ElMessageBox.confirm(
      `确定要将「${file.fileName}」移入回收站吗？`,
      '删除文件',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  try {
    await fileAPI.remove(file.fileId)
    ElMessage.success('已移入回收站')
    await refreshCurrentDir()
  } catch { /* ignore */ }
}

// 文件更多操作
function handleFileAction(cmd, file) {
  switch (cmd) {
    case 'move': openMove(file); break
    case 'delete': deleteFile(file); break
    case 'encrypt': encryptFileAction(file); break
  }
}

// 加密 / 解密
// 移入私密空间：选择目标目录
const encryptPickerVisible = ref(false)
let _encryptTarget = null  // 待加密的文件（单个或数组）

function openEncryptPicker(target) {
  _encryptTarget = target
  encryptPickerVisible.value = true
}

async function buildPrivateDirTree(parentId) {
  const res = await directoryAPI.list(parentId)
  const dirs = (res.data.data || []).filter(d => d.dirName === '私密空间')
  const tree = []
  for (const d of dirs) {
    const children = await buildPrivateDirTreeRecursive(d.dirId)
    tree.push({ dirId: Number(d.dirId), dirName: d.dirName, children })
  }
  return tree
}

async function buildPrivateDirTreeRecursive(parentId) {
  const res = await directoryAPI.list(parentId)
  const tree = []
  for (const d of (res.data.data || [])) {
    tree.push({
      dirId: Number(d.dirId), dirName: d.dirName,
      children: await buildPrivateDirTreeRecursive(d.dirId)
    })
  }
  return tree
}

async function onEncryptTargetSelected(targetDirId) {
  let pwd = userStore.privatePassword
  if (!pwd) {
    pwd = await requestPrivatePassword()
    if (!pwd) return
    userStore.setPrivatePassword(pwd)
  }
  const targets = Array.isArray(_encryptTarget) ? _encryptTarget
    : (_encryptTarget ? [_encryptTarget] : [])
  ElMessage.info(`正在移入 ${targets.length} 个文件...`)
  let ok = 0
  for (const item of targets) {
    try {
      await fileAPI.encrypt(item.id || item.fileId, pwd, targetDirId)
      ok++
    } catch { /* skip */ }
  }
  ElMessage.success(`成功移入 ${ok} 个文件`)
  _encryptTarget = null
  await refreshCurrentDir()
}

// 加密/解密入口
async function encryptFileAction(file) {
  openEncryptPicker([file])
}

async function decryptFileAction(file) {
  let pwd = userStore.privatePassword
  if (!pwd) {
    pwd = await requestPrivatePassword()
    if (!pwd) return
    userStore.setPrivatePassword(pwd)
  }
  try {
    await fileAPI.decrypt(file.fileId, pwd, userStore.privateSpaceRootDirId)
    ElMessage.success('已移出私密空间')
    await refreshCurrentDir()
  } catch { /* ignore */ }
}

// ==================== 私密密码弹窗 ====================
const privatePwdVisible = ref(false)
const privatePassword = ref('')
let privatePwdResolve = null

function requestPrivatePassword() {
  return new Promise((resolve) => {
    privatePassword.value = ''
    privatePwdResolve = resolve
    privatePwdVisible.value = true
  })
}

function resolvePrivatePwd() {
  const pwd = privatePassword.value
  privatePwdVisible.value = false
  if (privatePwdResolve) {
    privatePwdResolve(pwd)
    privatePwdResolve = null
  }
}

function cancelPrivatePwd() {
  privatePwdVisible.value = false
  if (privatePwdResolve) {
    privatePwdResolve(null)
    privatePwdResolve = null
  }
}

// ==================== 个人信息 ====================
const profileVisible = ref(false)
const profileFormRef = ref(null)
const profileForm = reactive({ newUsername: '', newGender: '未知' })
const profileAvatarFile = ref(null)
const profileLoading = ref(false)
const profileRules = {
  newUsername: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

function openProfileDialog() {
  profileForm.newUsername = userStore.username
  profileForm.newGender = userStore.gender || '未知'
  profileAvatarFile.value = null
  profileVisible.value = true
}

function handleProfileAvatarChange(uploadFile) {
  profileAvatarFile.value = uploadFile.raw
}
function handleProfileAvatarRemove() {
  profileAvatarFile.value = null
}

async function doUpdateProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return
  profileLoading.value = true
  try {
    const { userAPI } = await import('../api')
    await userAPI.updateUserInfo(profileForm.newUsername, profileForm.newGender || null, profileAvatarFile.value)
    // 重新拉取用户信息以获取正确的头像 URL
    const infoRes = await userAPI.getUserInfo(userStore.userId)
    const info = infoRes.data.data
    userStore.updateUserInfo(info.user.name, info.user.sex, info.user.avatar)
    userStore.setStorageInfo(info.storage)
    ElMessage.success('个人信息已更新')
    profileVisible.value = false
  } catch { /* ignore */ }
  finally { profileLoading.value = false }
}

// ==================== 修改密码 ====================
const passwordVisible = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmNew: '' })
const pwdLoading = ref(false)
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmNew: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (r, v, cb) => v !== pwdForm.newPassword ? cb(new Error('两次密码不一致')) : cb(), trigger: 'blur' }
  ]
}

function openPasswordDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmNew = ''
  passwordVisible.value = true
}

async function doChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    const { userAPI } = await import('../api')
    await userAPI.changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码已修改')
    passwordVisible.value = false
  } catch { /* ignore */ }
  finally { pwdLoading.value = false }
}

// ==================== 右键菜单 ====================
// simplified: just use the dropdown on each row

// ==================== 辅助函数 ====================
function getFileIconColor(file) {
  const type = (file.fileType || '').toLowerCase()
  const colors = {
    pdf: '#f56c6c', doc: '#409eff', docx: '#409eff',
    xls: '#67c23a', xlsx: '#67c23a', ppt: '#e6a23c', pptx: '#e6a23c',
    jpg: '#e040fb', jpeg: '#e040fb', png: '#e040fb', gif: '#e040fb',
    mp4: '#ff5722', mp3: '#795548', zip: '#607d8b', rar: '#607d8b',
    txt: '#9e9e9e', md: '#9e9e9e'
  }
  return colors[type] || '#909399'
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  // LocalDateTime format: "2024-01-01T12:00:00"
  return timeStr.replace('T', ' ')
}

const storageProgressColor = computed(() => {
  const pct = userStore.usedSpace / userStore.totalSpace
  if (pct > 0.9) return '#f56c6c'
  if (pct > 0.7) return '#e6a23c'
  return '#67c23a'
})

onMounted(init)
</script>
