<template>
  <div class="login-page">
    <div class="login-card">
      <h2>☁️ Netdisk 网盘</h2>

      <el-tabs v-model="activeTab" class="login-tabs" :stretch="true">
        <!-- ==================== 登录 ==================== -->
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" size="large">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"
                show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loginLoading" style="width:100%" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- ==================== 注册 ==================== -->
        <el-tab-pane label="注册" name="register">
          <el-form ref="regFormRef" :model="regForm" :rules="regRules" label-position="top" size="large">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="regForm.username" placeholder="请输入用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="请输入密码"
                show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="regForm.confirmPassword" type="password" placeholder="请再次输入密码"
                show-password :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="regForm.gender">
                <el-radio value="男">男</el-radio>
                <el-radio value="女">女</el-radio>
                <el-radio value="未知">保密</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="头像（可选）">
              <el-upload
                :auto-upload="false"
                :limit="1"
                accept="image/*"
                :on-change="handleAvatarChange"
                :on-remove="handleAvatarRemove"
              >
                <el-button size="small" type="primary" plain>选择图片</el-button>
                <template #tip>
                  <span class="text-muted" style="margin-left:8px">支持 JPG/PNG/GIF</span>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="regLoading" style="width:100%" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { authAPI } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const regLoading = ref(false)

// ==================== 登录表单 ====================
const loginFormRef = ref(null)
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loginLoading.value = true
  try {
    const res = await authAPI.login(loginForm.username, loginForm.password)
    userStore.setLoginData(res.data.data)
    ElMessage.success('登录成功')
    // 根据角色跳转
    if (res.data.data.user.role === 'admin') {
      router.push('/admin')
    } else {
      router.push('/main')
    }
  } catch { /* 拦截器已处理 */ }
  finally { loginLoading.value = false }
}

// ==================== 注册表单 ====================
const regFormRef = ref(null)
const regForm = reactive({ username: '', password: '', confirmPassword: '', gender: '未知' })
const avatarFile = ref(null)

const validateConfirm = (rule, value, callback) => {
  if (value !== regForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

function handleAvatarChange(uploadFile) {
  avatarFile.value = uploadFile.raw
}

function handleAvatarRemove() {
  avatarFile.value = null
}

async function handleRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (avatarFile.value && avatarFile.value.size > 10 * 1024 * 1024) {
    ElMessage.error('头像文件大小不能超过 10MB')
    return
  }
  regLoading.value = true
  try {
    await authAPI.register(regForm.username, regForm.password, regForm.gender, avatarFile.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = regForm.username
    loginForm.password = ''
    regForm.username = ''
    regForm.password = ''
    regForm.confirmPassword = ''
    avatarFile.value = null
  } catch { /* 拦截器已处理 */ }
  finally { regLoading.value = false }
}
</script>
