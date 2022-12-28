import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import App from './App.vue'

import VueRouter from 'vue-router'
import routes from './routers/routes.js'
import axios from 'axios'
//引用全局变量文件
import GLOBAL_VAR from './api/global_variable.js'
// 引入echarts, 'cnpm install echarts'
import echarts from 'echarts'
//  引入echarts-gl, 'cnpm install echarts-gl'
import 'echarts-gl'

import 'babel-polyfill'

Vue.use(ElementUI)
Vue.use(VueRouter)

//绑定到vue属性
Vue.prototype.GLOBAL_VAR = GLOBAL_VAR
Vue.prototype.$ajax = axios
Vue.prototype.$echarts = echarts

/**
 * 创建 router 实例，然后传 `routes` 配置
 */
const router = new VueRouter({
	mode: 'history', //模式设为history，否则跳转时 URL 上会带有 # 符号
	routes//（缩写）相当于 routes: routes
})

new Vue({
  el: '#app',
  router,
  render: h => h(App)
})
