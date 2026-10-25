import Vue from 'vue'
import VueRouter from 'vue-router'
import Home from '../views/Home.vue'
import Index from '../views/Index.vue'

import request from "../utils/axios";
import store from "../store"

Vue.use(VueRouter)

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Home,
        redirect: '/index',

        children: [
            {
                path: '/index',

                name: 'Index',
                meta: {
                    title: "系统首页"
                },
                component: Index
            },
            {
                path: '/person',
                name: 'Person',
                meta: {
                    title: "个人中心"
                },
                component: () => import('@/views/sys/Person.vue')
            },
            {
                path: '/repassword',
                name: 'Repassword',
                meta: {
                    title: "修改密码"
                },
                component: () => import('@/views/sys/Repassword.vue')
            },

            // {
            // 	path: '/sys/roles',
            // 	name: 'SysRole',
            // 	component: Role
            // },
            // {
            // 	path: '/sys/menus',
            // 	name: 'SysMenu',
            // 	component: Menu
            // },
        ]
    },

    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue')
    },


]

const router = new VueRouter({
    mode: 'history',
    base: process.env.BASE_URL,
    routes
})

router.beforeEach((to, from, next) => {


    let hasRoute = store.state.menus.hasRoutes

    let token = localStorage.getItem("token")
    localStorage.setItem("currentPathName", to.meta.title)
    store.commit("setPath")  //触发store的数据更新
    if (to.path == '/login') {
        next()

    } else if (!token) {
        next({path: '/login'})


    } else if (token && !hasRoute) {

        request.get("/sys/menu/nav", {
            headers: {
                Authorization: localStorage.getItem("token")
            }
        }).then(res => {


            // 拿到menuList
            store.commit("setMenuList", res.data.nav)

            // 拿到用户权限
            store.commit("setPermList", res.data.authoritys)


            // 动态绑定路由
            let newRoutes = router.options.routes

            res.data.nav.forEach(menu => {
                if (menu.component != '') {
                    // 转成路由
                    let route = menuToRoute(menu)

                    // 吧路由添加到路由管理中
                    if (route) {
                        newRoutes[0].children.push(route)
                    }
                }
                if ((menu.component == '' || menu.component == null) && menu.children) {
                    menu.children.forEach(e => {

                        // 转成路由
                        let route = menuToRoute(e)

                        // 吧路由添加到路由管理中
                        if (route) {
                            newRoutes[0].children.push(route)
                        }

                    })
                }
            })


            router.addRoutes(newRoutes)

            hasRoute = true
            store.commit("changeRouteStatus", hasRoute)
        })
    }


    next()
})


// 导航转成路由
const menuToRoute = (menu) => {

    if (!menu.component) {
        return null
    }

    let route = {
        name: menu.name,
        path: menu.path,
        meta: {
            icon: menu.icon,
            title: menu.title
        }
    }

    route.component = () => import('@/views/' + menu.component + '.vue')

    return route
}

export default router
