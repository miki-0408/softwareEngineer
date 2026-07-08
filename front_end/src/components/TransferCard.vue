<template>
  <div class="transfer-card" :class="{ 'is-done': task.status === 'done', 'is-error': task.status === 'error' }">
    <div class="transfer-card-left">
      <!-- 图标 -->
      <div class="transfer-icon" :class="'icon-' + task.status">
        <el-icon :size="18">
          <UploadFilled v-if="task.type === 'upload' && task.status === 'running'" />
          <Download v-if="task.type === 'download' && task.status === 'running'" />
          <CircleCheckFilled v-if="task.status === 'done'" />
          <CircleCloseFilled v-if="task.status === 'error'" />
          <Clock v-if="task.status === 'queued'" />
        </el-icon>
      </div>

      <!-- 信息 -->
      <div class="transfer-info">
        <div class="transfer-name">
          {{ task.name }}
          <el-tag size="small" :type="task.type === 'upload' ? 'primary' : 'success'" effect="plain" style="margin-left:6px">
            {{ task.type === 'upload' ? '上传' : '下载' }}
          </el-tag>
        </div>
        <div class="transfer-meta">
          <span>{{ transferStore.formatSize(task.size) }}</span>
          <template v-if="task.status === 'running'">
            <span class="transfer-progress-text">{{ task.progress }}%</span>
          </template>
          <template v-else-if="task.status === 'queued'">
            <span class="text-muted">排队中...</span>
          </template>
          <template v-else-if="task.status === 'done'">
            <span style="color:#67c23a">完成</span>
          </template>
          <template v-else-if="task.status === 'error'">
            <span class="text-danger">{{ task.errorMsg || '失败' }}</span>
          </template>
        </div>
        <!-- 进度条 -->
        <el-progress
          v-if="task.status === 'running'"
          :percentage="task.progress"
          :stroke-width="4"
          :show-text="false"
          style="margin-top:6px"
        />
        <el-progress
          v-if="task.status === 'done'"
          :percentage="100"
          :stroke-width="4"
          :show-text="false"
          status="success"
          style="margin-top:6px"
        />
      </div>
    </div>

    <!-- 右侧操作 -->
    <el-button
      v-if="task.status === 'done' || task.status === 'error'"
      size="small"
      text
      @click="$emit('remove')"
    >
      <el-icon><Close /></el-icon>
    </el-button>
  </div>
</template>

<script setup>
import {
  UploadFilled, Download, CircleCheckFilled, CircleCloseFilled, Clock, Close
} from '@element-plus/icons-vue'
import { useTransferStore } from '../stores/transfer'

defineProps({ task: Object })
defineEmits(['remove'])
const transferStore = useTransferStore()
</script>

<style scoped>
.transfer-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin-bottom: 6px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  transition: opacity .3s;
}
.transfer-card.is-done { opacity: .7; }
.transfer-card.is-error { border-color: #fde2e2; background: #fef0f0; }
.transfer-card-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.transfer-icon {
  width: 36px; height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.icon-queued { background: #f0f2f5; color: #909399; }
.icon-running { background: #ecf5ff; color: #409eff; }
.icon-done { background: #f0f9eb; color: #67c23a; }
.icon-error { background: #fef0f0; color: #f56c6c; }
.transfer-info { flex: 1; min-width: 0; }
.transfer-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.transfer-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  display: flex;
  gap: 12px;
}
.transfer-progress-text {
  color: #409eff;
  font-weight: 600;
}
</style>
