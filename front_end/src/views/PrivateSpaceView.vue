<template>
  <div class="app-layout">
    <Sidebar active-menu="private" />

    <div class="app-main">
      <!-- 顶栏 -->
      <div class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="(item, idx) in breadcrumb" :key="item.dirId">
            <el-icon v-if="idx === 0"><Lock /></el-icon>
            <el-link type="primary" :underline="false" @click="navigateToBreadcrumb(idx)">
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

          <!-- 目录列表 -->
          <div v-if="directories.length > 0" style="margin-bottom:16px">
            <el-text type="info" size="small">文件夹</el-text>
            <div class="directory-tree">
              <div
                v-for="dir in directories"
                :key="dir.dirId"
                class="dir-item"
                @click="navigateToDir(dir.dirId, dir.dirName)"
              >
                <el-icon color="#e6a23c"><Folder /></el-icon>
                <span>{{ dir.dirName }}</span>
                <div style="margin-left:auto;display:flex;gap:4px" @click.stop>
                  <el-button size="small" text @click="openRenameDir(dir)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button size="small" text type="danger" @click="deleteDirectory(dir)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 文件列表 -->
          <el-table
            :data="files"
            style="width:100%"
            v-loading="loading"
            empty-text="此目录为空"
          >
            <el-table-column label="文件名" min-width="260">
              <template #default="{ row }">
                <div class="flex-center gap-8">
                  <el-icon :size="20"><Document /></el-icon>
                  <span>{{ row.fileName }}</span>
                  <el-tag size="small" type="danger" effect="dark">
                    <el-icon :size="12"><Lock /></el-icon>
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="info">{{ row.fileType || '未知' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="120" sortable prop="fileSize">
              <template #default="{ row }">
                {{ userStore.formatSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="180" sortable prop="uploadTime">
              <template #default="{ row }">
                {{ formatTime(row.uploadTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="downloadFile(row)">
                  <el-icon><Download /></el-icon> 下载
                </el-button>
                <el-button size="small" link @click="openRenameFile(row)">
                  <el-icon><Edit /></el-icon> 重命名
                </el-button>
                <el-dropdown trigger="click" @command="(cmd) => handleFileAction(cmd, row)">
                  <el-button size="small" link>
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
    <el-dialog v-model="uploadVisible" title="上传文件（自动加密）" width="440px">
      <el-upload ref="uploadRef" :auto-upload="false" :limit="1"
        :on-change="handleUploadChange" :on-remove="handleUploadRemove" drag>
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处或<em>点击选择</em></div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" :disabled="!uploadFile" @click="doUpload">
          上传（将自动加密）
        </el-button>
      </template>
    </el-dialog>

    <!-- 移动 -->
    <el-dialog v-model="moveVisible" title="移动到..." width="500px">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Lock, Key, FolderAdd, Upload, Refresh, Folder, Document,
  Download, Edit, Delete, MoreFilled, Rank, Unlock, UploadFilled
} from '@element-plus/icons-vue'
import { privateSpaceAPI, directoryAPI, fileAPI, userAPI } from '../api'
import { useUserStore } from '../stores/user'
import Sidebar from '../components/Sidebar.vue'

const userStore = useUserStore()

// ==================== 密码验证 ====================
const pwdInput = ref('')
const verifyLoading = ref(false)
const verified = ref(false)

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
  ElMessage.success('已刷新')
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
const uploadFile = ref(null)
const uploadLoading = ref(false)

function openUpload() { uploadFile.value = null; uploadVisible.value = true }
function handleUploadChange(item) { uploadFile.value = item.raw }
function handleUploadRemove() { uploadFile.value = null }

async function doUpload() {
  if (!uploadFile.value) return
  if (!currentDirId.value) {
    ElMessage.error('目录信息未加载，请刷新页面重试')
    return
  }
  uploadLoading.value = true
  try {
    await fileAPI.upload(currentDirId.value, uploadFile.value, true, userStore.privatePassword)
    ElMessage.success('已加密上传')
    uploadVisible.value = false
    await refreshCurrentDir()
  } catch { /* ignore */ }
  finally { uploadLoading.value = false }
}

// 下载
async function downloadFile(file) {
  try {
    const res = await fileAPI.download(file.fileId, userStore.privatePassword)
    const url = window.URL.createObjectURL(new Blob([res.data]))
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
  moveFileTarget.value = file; moveTargetDirId.value = null
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
