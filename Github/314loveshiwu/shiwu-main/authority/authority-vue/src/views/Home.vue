<template>
  <el-container>
    <el-aside width="200px" class="bg" height="1000px">

      <SideMenu class="bg"></SideMenu>

    </el-aside>
    <el-container>
      <el-header class="bg">
        <strong><span class="el-icon-printer"></span>欢迎来到校园失物管理平台</strong>

        <div class="header-avatar">

          <el-tooltip content="全屏显示" placement="top" effect="dark">

            <i class="el-icon-s-grid" @click="fullScreen" style="font-size: 30px"> </i>
          </el-tooltip>
          <el-avatar size="medium" :src="userInfo.avatar"></el-avatar>

          <el-dropdown>
						<span class="el-dropdown-link">
						{{ userInfo.username }}<i class="el-icon-arrow-down el-icon--right"></i>
						</span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item>
                <router-link :to="{name: 'Person'}">个人信息</router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <router-link :to="{name: 'Repassword'}">修改密码</router-link>
              </el-dropdown-item>
              <el-dropdown-item @click.native="logout">退出</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>


        </div>

      </el-header>
      <el-main>

        <div style="margin: 0 15px;">
          <div style="margin-top: 10px;margin-bottom: 30px">
            <el-breadcrumb separator="/" style="font-size: 16px;font-weight: inherit">
              <el-breadcrumb-item :to="{ path: '/' }"><i class="el-icon-s-unfold">{{ currentPathName }}</i>
              </el-breadcrumb-item>

            </el-breadcrumb>
          </div>
          <router-view/>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>

import SideMenu from "@/components/AsideMenu";
import request from "@/utils/axios";
import screenfull from 'screenfull'

export default {
  name: "Home",
  components: {
    SideMenu
  },
  computed: {
    currentPathName() {
      return this.$store.state.currentPathName;
    }
  },
  watch: {  //百度搜索，监听路由的变化
    currentPathName(newVal, oldVal) {

    }
  },

  data() {
    return {
      userInfo: {
        id: "",
        username: "",
        avatar: "",

      },
      breadcrumb: '',
      isFullscreen: false


    }
  },
  created() {
    this.getUserInfo()


  },


  methods: {
    getUserInfo() {

      request.get('/sys/user/getuserInfo').then(res => {

        this.userInfo = res.data
      })
    },

    logout() {

      this.$axios.post("/logout").then(res=>{
        this.$router.push("/login")
        this.$store.commit("resetState")
        localStorage.clear()
      })

    }
    ,
    fullScreen() {
      if (!screenfull.isEnabled) {
        this.$message({message: '你的浏览器不支持全屏', type: 'warning'})
        return false
      }

      screenfull.toggle()
    }


  }
}
</script>

<style scoped>
.el-container {
  padding: 0;
  margin: 0;
  height: 100%;
}

.header-avatar {
  float: right;
  width: 200px;
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
}

.el-header {
  background-color: #D3DCE6;
  color: #333;

  line-height: 60px;
}

.el-aside {
  background-color: #D3DCE6;
  color: #333;
  line-height: 200px;
}

.el-main {
  color: #333;
  padding: 0;
}

a {
  text-decoration: none;
}

.bg {
  background: url("../assets/cbg.png");
}
</style>
