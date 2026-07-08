<template>
  <div class="app-sidebar">
    <!-- Logo 区域 -->
    <div class="sidebar-header">
      <el-icon :size="22" color="#409eff"><Cloudy /></el-icon>
      <h3>Netdisk</h3>
    </div>

    <!-- 导航菜单 -->
    <div class="sidebar-nav">
      <el-menu
        :default-active="activeMenu"
        router
        @select="handleSelect"
      >
        <!-- 普通用户菜单 -->
        <template v-if="!userStore.isAdmin">
          <el-menu-item index="files" :route="{ path: '/main' }">
            <el-icon><FolderOpened /></el-icon>
            <span>我的文件</span>
          </el-menu-item>

          <el-menu-item index="recycle" :route="{ path: '/recycle' }">
            <el-icon><Delete /></el-icon>
            <span>回收站</span>
          </el-menu-item>

          <el-menu-item index="transfer" :route="{ path: '/transfer' }">
            <el-icon><Connection /></el-icon>
            <span>传输</span>
            <span v-if="transferStore.running > 0" class="transfer-badge">{{ transferStore.running }}</span>
          </el-menu-item>

          <el-menu-item index="private" :route="{ path: '/private' }"
            :class="{ 'ps-active': userStore.privateSpaceEnabled }">
            <el-icon><Lock /></el-icon>
            <span>私密空间</span>
            <el-icon v-if="userStore.privateSpaceEnabled" size="12" color="#67c23a"
              style="margin-left:auto;margin-right:8px"><CircleCheckFilled /></el-icon>
          </el-menu-item>
        </template>

        <!-- 管理员菜单 -->
        <template v-if="userStore.isAdmin">
          <el-menu-item index="admin" :route="{ path: '/admin' }">
            <el-icon><Setting /></el-icon>
            <span>管理后台</span>
          </el-menu-item>
        </template>
      </el-menu>
    </div>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer">
      <div class="sidebar-user">
        <el-avatar :size="32" :src="avatarUrl" />
        <div class="sidebar-user-info">
          <div class="sidebar-user-name">{{ userStore.username }}</div>
          <div class="sidebar-user-role">
            {{ userStore.isAdmin ? '管理员' : '普通用户' }}<span v-if="userStore.gender"> · {{ userStore.gender }}</span>
          </div>
        </div>
        <el-popconfirm title="确定要退出登录吗？" @confirm="handleLogout">
          <template #reference>
            <el-button text :icon="SwitchButton" style="color:#bfcbd9" size="small" />
          </template>
        </el-popconfirm>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Cloudy, FolderOpened, Delete, Lock, Setting, SwitchButton, CircleCheckFilled, Connection } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { useTransferStore } from '../stores/transfer'

const transferStore = useTransferStore()
const props = defineProps({
  activeMenu: { type: String, default: 'files' }
})

const router = useRouter()
const userStore = useUserStore()

const avatarUrl = computed(() => {
  const av = userStore.avatar
  if (!av) return ''
  if (av.startsWith('http')) return av
  return import.meta.env.PROD ? av : '/api' + av
})

function handleSelect(index) {
  // router 已通过 :route 处理
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}
</script>
