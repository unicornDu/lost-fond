import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import Element from "element-ui"
import "element-ui/lib/theme-chalk/index.css"
import "./assets/all.css"
import VForm from 'vform-builds';
import 'vform-builds/dist/VFormDesigner.css'
// import axios from 'axios'
// Vue.prototype.$axios = axios
import request from "@/utils/axios";
Vue.prototype.$axios = request
Vue.config.productionTip = false
// require("./mock")
Vue.use(Element,{size:'small'})
Vue.use(VForm)
new Vue({
  router,
  store,
  render: function (h) { return h(App) }
}).$mount('#app')
