import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
    namespaced:true,
    state: {

        menuList: [],
        permList: [],
    },
    getters: {},
    mutations: {
        setMenuList(state, menus) {
            state.menuList = menus
        },
        setPermList(state, perms) {
            state.permList = perms
        },
    },

    actions: {

    },
    modules: {}
})
