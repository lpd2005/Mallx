import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@mallx/shared/style.css'
import './style.css'
import App from './App.vue'
import router from './router.js'
import { initApi } from './services.js'
const app = createApp(App),
  pinia = createPinia()
app.use(pinia)
app.provide('api', initApi(router, pinia))
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
