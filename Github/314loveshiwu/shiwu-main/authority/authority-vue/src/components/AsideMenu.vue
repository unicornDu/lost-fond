<template>
  <el-menu
      class="el-menu-vertical-demo"

      text-color="black"
      active-text-color="black"
  >
    <router-link to="/index">
      <el-menu-item index="Index" >
        <template slot="title">
          <i class="el-icon-s-home"></i>
          <span slot="title">系统首页</span>
        </template>
      </el-menu-item>
    </router-link>
    <div  v-for="(menu,subindex) in menuList">
    <el-submenu v-if="menu.component==null||menu.component==''" default-active="Index" :index="menu.name">
      <template slot="title">
        <i :class="menu.icon"></i>
        <span>{{menu.title}}</span>
      </template>
      <router-link :to="item.path" v-for="item in menu.children":key="item.id">
        <el-menu-item :index="item.name" >
          <template slot="title">
            <i :class="item.icon"></i>
            <span slot="title">{{item.title}}</span>
          </template>
        </el-menu-item>
      </router-link>
    </el-submenu>

      <router-link v-else-if="menu.component!=null" :to="menu.path" >
        <el-menu-item :index="''+subindex" >
          <template slot="title">
            <i :class="menu.icon"></i>
            <span slot="title">{{ menu.title }}</span>
          </template>
        </el-menu-item>
      </router-link>
      </div>
  </el-menu>
</template>

<script>
export default {
  name: "AsideMenu",
  data(){
    return{

    }
   },
  computed:  {
    menuList: {
      get() {

        return this.$store.state.menuList
      }
    }
  },
  methods: {
    handleOpen(key, keyPath) {

    },
    transmit(title){

    this.$emit('func',title)
    },
    handleClose(key, keyPath) {

    }
  }
}
</script>

<style scoped>
.el-menu-vertical-demo {
  height: 100%;
}
a{
  text-decoration:none;
}
</style>
