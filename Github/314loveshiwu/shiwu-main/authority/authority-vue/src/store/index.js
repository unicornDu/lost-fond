import Vue from 'vue'
import Vuex from 'vuex'
import menus from './modules/menus'
Vue.use(Vuex)

export default new Vuex.Store({
    state: {
        token: '',
        hasRoute:false,
        menuList: [],
        permList: [],
        currentPathName: ''


    },
    getters: {},
    mutations: {
        setPath (state) {
            state.currentPathName = localStorage.getItem("currentPathName")
        },
        SET_TOKEN: (state, token) => {
            state.token = token
            localStorage.setItem("token", token)
        },
        resetState:(state)=>{
            state.token=''
            state.menuList=[]
            state.permList=[]

        },

        changeRouteStatus(state,hasRoute){
            state.hasRoute=hasRoute
        },
        setMenuList(state, menus) {
            state.menuList = menus
        },
        setPermList(state, perms) {
            state.permList = perms
        }
    },
    actions: {},
    modules: {
        menus
    }
})
