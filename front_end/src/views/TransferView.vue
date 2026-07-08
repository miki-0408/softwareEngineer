<template>
  <div class="app-layout">
    <Sidebar active-menu="transfer" />
    <div class="app-main">
      <div class="app-header">
        <el-breadcrumb>
          <el-breadcrumb-item><el-icon><Connection /></el-icon> 传输</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="flex-center gap-8">
          <span v-if="transferStore.running > 0" class="text-muted" style="font-size:13px">
            {{ transferStore.running }} 个任务进行中
          </span>
          <el-button v-if="hasDone" size="small" @click="transferStore.clearDone()">
            清除已完成
          </el-button>
        </div>
      </div>
      <div class="app-content">
        <el-empty v-if="transferStore.tasks.length === 0" description="暂无传输任务" />

        <!-- 进行中 -->
        <template v-if="activeTasks.length > 0">
          <div style="font-size:14px;font-weight:600;margin-bottom:12px;color:#303133">
            进行中（{{ activeTasks.length }}）
          </div>
          <TransferCard v-for="t in activeTasks" :key="t.id" :task="t" @remove="transferStore.removeTask(t.id)" />
        </template>

        <!-- 已完成 -->
        <template v-if="doneTasks.length > 0">
          <div style="font-size:14px;font-weight:600;margin:20px 0 12px;color:#909399">
            已完成（{{ doneTasks.length }}）
          </div>
          <TransferCard v-for="t in doneTasks" :key="t.id" :task="t" @remove="transferStore.removeTask(t.id)" />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useTransferStore } from '../stores/transfer'
import TransferCard from '../components/TransferCard.vue'
import Sidebar from '../components/Sidebar.vue'

const transferStore = useTransferStore()

const activeTasks = computed(() =>
  transferStore.tasks.filter(t => t.status === 'queued' || t.status === 'running')
)
const doneTasks = computed(() =>
  transferStore.tasks.filter(t => t.status === 'done' || t.status === 'error')
)
const hasDone = computed(() => doneTasks.value.length > 0)
</script>
