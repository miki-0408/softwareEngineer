import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录 / 注册' }
  },
  {
    path: '/main',
    name: 'Main',
    component: () => import('../views/MainView.vue'),
    meta: { title: '我的网盘', requiresAuth: true }
  },
  {
    path: '/recycle',
    name: 'RecycleBin',
    component: () => import('../views/RecycleBinView.vue'),
    meta: { title: '回收站', requiresAuth: true }
  },
  {
    path: '/private',
    name: 'PrivateSpace',
    component: () => import('../views/PrivateSpaceView.vue'),
    meta: { title: '私密空间', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/AdminView.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || 'Netdisk 网盘'
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth && !token) {
    next('/')
    return
  }

  if (to.meta.requiresAdmin) {
    const role = localStorage.getItem('role')
    if (role !== 'admin') {
      next('/main')
      return
    }
  }

  if (to.path === '/' && token) {
    next('/main')
    return
  }

  next()
})

export default router
