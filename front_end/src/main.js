import { createApp } from 'vue'       // Vue 3 应用创建函数
import { createPinia } from 'pinia'    // 状态管理库创建函数
import ElementPlus from 'element-plus' // Element Plus UI 组件库
import 'element-plus/dist/index.css'   // Element Plus 样式文件
import zhCn from 'element-plus/dist/locale/zh-cn.mjs' // Element Plus 中文本地化
import * as ElementPlusIconsVue from '@element-plus/icons-vue' // Element Plus 全部图标
import App from './App.vue'            // 根组件
import router from './router'          // 路由配置（6个页面的映射关系 + 登录守卫）
import './style.css'                   // 全局自定义样式

const app = createApp(App)     // 创建 Vue 应用实例
const pinia = createPinia()    // 创建 Pinia 状态管理实例（用于跨页面共享数据）

app.use(pinia)                 // 挂载 Pinia 插件 → 所有 .vue 文件都能用 useXxxStore()
app.use(router)                // 挂载路由插件 → 支持页面跳转和 URL hash 路由
app.use(ElementPlus, { locale: zhCn }) // 挂载 Element Plus + 中文语言包 → 所有组件文字显示中文

// 把 Element Plus 的所有图标组件全局注册，任何 .vue 文件中可直接使用 <HomeFilled /> 等
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')              // 把整个 Vue 应用挂载到 index.html 中的 <div id="app"> 上
