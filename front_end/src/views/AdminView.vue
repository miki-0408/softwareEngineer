<template>
  <div class="app-layout">
    <Sidebar active-menu="admin" />

    <div class="app-main">
      <div class="app-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item><el-icon><Setting /></el-icon> 管理后台</el-breadcrumb-item>
        </el-breadcrumb>
        <span class="text-muted">欢迎，管理员 {{ userStore.username }}</span>
      </div>

      <div class="app-content">
        <!-- 功能卡片 -->
        <el-row :gutter="20" style="margin-bottom:20px">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="flex-between">
                  <span><el-icon><UserFilled /></el-icon> 重置用户密码</span>
                </div>
              </template>
              <div>
                <div style="font-size:14px;color:#606266;margin-bottom:6px">用户 ID</div>
                <el-input v-model="resetUserId" placeholder="请输入用户ID" style="margin-bottom:12px" />
                <el-alert
                  title="提示"
                  type="info"
                  description="重置后密码将恢复为默认密码 password123"
                  show-icon
                  :closable="false"
                  style="margin-bottom:12px"
                />
                <el-button type="warning" :loading="resetLoading" @click="doResetPassword">
                  重置密码
                </el-button>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="flex-between">
                  <span><el-icon><Edit /></el-icon> 修改用户信息</span>
                </div>
              </template>
              <div>
                <div style="font-size:14px;color:#606266;margin-bottom:6px">用户 ID</div>
                <el-input v-model="updateUserId" placeholder="请输入用户ID" style="margin-bottom:14px" />
                <div style="font-size:14px;color:#606266;margin-bottom:6px">新用户名</div>
                <el-input v-model="updateUsername" placeholder="请输入新用户名" style="margin-bottom:14px" />
                <div style="font-size:14px;color:#606266;margin-bottom:6px">性别</div>
                <el-radio-group v-model="updateGender" style="margin-bottom:14px;display:block">
                  <el-radio value="男">男</el-radio>
                  <el-radio value="女">女</el-radio>
                </el-radio-group>
                <el-button type="primary" :loading="updateLoading" @click="doUpdateUser">
                  更新信息
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 操作日志 -->
        <el-card shadow="hover">
          <template #header>
            <div class="flex-between">
              <span><el-icon><Document /></el-icon> 系统操作日志</span>
              <el-button size="small" @click="refreshLogs">
                <el-icon><Refresh /></el-icon> 刷新
              </el-button>
            </div>
          </template>

          <el-table :data="paginatedLogs" v-loading="logsLoading" empty-text="暂无日志记录" max-height="400">
            <el-table-column label="日志ID" prop="logId" width="80" />
            <el-table-column label="操作者ID" prop="operatorId" width="100" />
            <el-table-column label="操作描述" prop="description" min-width="300" show-overflow-tooltip />
            <el-table-column label="时间" prop="time" width="200">
              <template #default="{ row }">
                {{ formatTime(row.time) }}
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px;display:flex;justify-content:flex-end">
            <el-pagination
              v-model:current-page="logPage"
              :page-size="logPageSize"
              :total="logs.length"
              layout="total, prev, pager, next"
              small
            />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, UserFilled, Edit, Document, Refresh } from '@element-plus/icons-vue'
import { adminAPI } from '../api'
import { formatTime } from '../utils/format'
import { useUserStore } from '../stores/user'
import Sidebar from '../components/Sidebar.vue'

const userStore = useUserStore()

// ==================== 重置密码 ====================
const resetUserId = ref('')
const resetLoading = ref(false)

async function doResetPassword() {
  if (!resetUserId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }
  resetLoading.value = true
  try {
    await adminAPI.resetPassword(resetUserId.value)
    ElMessage.success('密码已重置为 password123')
    resetUserId.value = ''
  } catch { /* ignore */ }
  finally { resetLoading.value = false }
}

// ==================== 更新用户信息 ====================
const updateUserId = ref('')
const updateUsername = ref('')
const updateGender = ref('')
const updateLoading = ref(false)

async function doUpdateUser() {
  if (!updateUserId.value || !updateUsername.value) {
    ElMessage.warning('请填写用户ID和新用户名')
    return
  }
  updateLoading.value = true
  try {
    await adminAPI.updateUserInfo(updateUserId.value, updateUsername.value, updateGender.value || null, null)
    ElMessage.success('用户信息已更新')
    updateUserId.value = ''
    updateUsername.value = ''
    updateGender.value = ''
  } catch { /* ignore */ }
  finally { updateLoading.value = false }
}

// ==================== 日志 ====================
const logs = ref([])
const logsLoading = ref(false)
const logPage = ref(1)
const logPageSize = 20

const paginatedLogs = computed(() => {
  const start = (logPage.value - 1) * logPageSize
  return logs.value.slice(start, start + logPageSize)
})

async function refreshLogs() {
  logsLoading.value = true
  try {
    const res = await adminAPI.getLogs()
    logs.value = res.data.data || []
  } catch { logs.value = [] }
  finally { logsLoading.value = false }
}

onMounted(refreshLogs)
</script>
