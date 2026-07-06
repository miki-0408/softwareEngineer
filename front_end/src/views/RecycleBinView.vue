<template>
  <div class="app-layout">
    <Sidebar active-menu="recycle" />

    <div class="app-main">
      <div class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item><el-icon><Delete /></el-icon> 回收站</el-breadcrumb-item>
        </el-breadcrumb>
        <span class="text-muted">已删除的文件会保留在此处，可还原或彻底删除</span>
      </div>

      <div class="app-content">
        <div style="margin-bottom:16px">
          <el-button @click="refreshList">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
          <el-button type="danger" plain :disabled="selectedIds.length === 0"
            @click="batchDeletePermanent">
            彻底删除选中项
          </el-button>
        </div>

        <el-table
          :data="recycleFiles"
          v-loading="loading"
          empty-text="回收站为空"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column label="文件名" min-width="260">
            <template #default="{ row }">
              <div class="flex-center gap-8">
                <el-icon :size="20"><Document /></el-icon>
                <span>{{ row.fileName }}</span>
                <el-tag v-if="row.isEncrypted === 1" size="small" type="danger" effect="dark">
                  已加密
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
          <el-table-column label="删除时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.uploadTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="restoreFile(row)">
                <el-icon><RefreshLeft /></el-icon> 还原
              </el-button>
              <el-button size="small" type="danger" link @click="permanentDeleteFile(row)">
                <el-icon><DeleteFilled /></el-icon> 彻底删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, DeleteFilled, RefreshLeft, Document } from '@element-plus/icons-vue'
import { recycleAPI } from '../api'
import { useUserStore } from '../stores/user'
import Sidebar from '../components/Sidebar.vue'

const userStore = useUserStore()

const recycleFiles = ref([])
const selectedIds = ref([])
const loading = ref(false)

async function refreshList() {
  loading.value = true
  try {
    const res = await recycleAPI.list()
    recycleFiles.value = res.data.data || []
  } catch { recycleFiles.value = [] }
  finally { loading.value = false }
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(f => f.fileId)
}

async function restoreFile(file) {
  try {
    await recycleAPI.restore(file.fileId)
    ElMessage.success('文件已还原')
    await refreshList()
  } catch { /* ignore */ }
}

async function permanentDeleteFile(file) {
  try {
    await ElMessageBox.confirm(
      `文件「${file.fileName}」将被彻底删除，无法恢复！确定继续？`,
      '彻底删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error' }
    )
  } catch { return }
  try {
    await recycleAPI.deletePermanent(file.fileId)
    ElMessage.success('已彻底删除')
    await refreshList()
  } catch { /* ignore */ }
}

async function batchDeletePermanent() {
  try {
    await ElMessageBox.confirm(
      `将彻底删除选中的 ${selectedIds.value.length} 个文件，无法恢复！确定继续？`,
      '批量删除',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'error' }
    )
  } catch { return }
  for (const id of selectedIds.value) {
    try { await recycleAPI.deletePermanent(id) } catch { /* continue */ }
  }
  ElMessage.success('批量删除完成')
  await refreshList()
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ')
}

onMounted(refreshList)
</script>
