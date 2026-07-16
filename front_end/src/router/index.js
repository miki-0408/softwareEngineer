import { createRouter, createWebHashHistory } from 'vue-router' // Vue Router：前端路由管理

const routes = [                                     // 路由表：URL 路径 → Vue 页面组件
  {
    path: '/',                                       // 根路径 = 登录页
    name: 'Login',
    component: () => import('../views/LoginView.vue'), // 懒加载：用户访问时才下载该页面的 JS 代码
    meta: { title: '登录 / 注册' }                    // 元信息：页面标题
  },
  {
    path: '/main',                                   // 主文件管理页
    name: 'Main',
    component: () => import('../views/MainView.vue'),
    meta: { title: '我的网盘', requiresAuth: true }  // requiresAuth：需要登录
  },
  {
    path: '/recycle',                                // 回收站
    name: 'RecycleBin',
    component: () => import('../views/RecycleBinView.vue'),
    meta: { title: '回收站', requiresAuth: true }
  },
  {
    path: '/private',                                // 私密空间（加密文件保险箱）
    name: 'PrivateSpace',
    component: () => import('../views/PrivateSpaceView.vue'),
    meta: { title: '私密空间', requiresAuth: true }
  },
  {
    path: '/transfer',                               // 传输队列（上传/下载进度跟踪）
    name: 'Transfer',
    component: () => import('../views/TransferView.vue'),
    meta: { title: '传输', requiresAuth: true }
  },
  {
    path: '/admin',                                  // 管理后台
    name: 'Admin',
    component: () => import('../views/AdminView.vue'),
    meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true } // requiresAdmin：需要管理员角色
  }
]

const router = createRouter({
  history: createWebHashHistory(),                   // Hash 模式：URL 格式为 /#/main，无需服务端配置
  routes
})

router.beforeEach((to, from, next) => {              // 全局路由守卫：每次页面跳转前触发
  document.title = to.meta.title || 'Netdisk 网盘'    // 设置浏览器标签页标题

  const token = localStorage.getItem('token')         // 检查是否已登录
  if (to.meta.requiresAuth && !token) {
    next('/')                                        // 未登录 → 跳回登录页
    return
  }

  if (to.meta.requiresAdmin) {                       // 管理员页面额外检查
    const role = localStorage.getItem('role')         // 角色存储在 localStorage（登录时写入）
    if (role !== 'admin') {
      next('/main')                                  // 非管理员 → 跳回主页
      return
    }
  }

  if (to.path === '/' && token) {
    next('/main')                                    // 已登录访问登录页 → 自动跳到主页
    return
  }

  next()                                              // 通过检查 → 正常跳转
})

export default router
