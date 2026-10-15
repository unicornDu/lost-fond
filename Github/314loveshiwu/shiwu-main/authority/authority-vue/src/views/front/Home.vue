<template>
  <div>
    <el-row>

      <el-col :span="20" :offset="2">
        <el-menu :default-active="activeIndex" class="el-menu-demo" mode="horizontal" @select="handleSelect">
          <div
              style="height: 60px;min-width: 300px;display: flex; font-size: 20px;line-height: 60px;font-style: initial">
            <i class="el-icon-menu" style="font-size: 60px;color: #409EFF"></i>
            <div><span></span></div>


          </div>
          <el-menu-item index="1">系统主页</el-menu-item>
          <el-menu-item index="2">处理中心</el-menu-item>
          <el-menu-item index="3">处理中心</el-menu-item>
          <el-dropdown style="margin-left: auto;margin-top: auto;margin-bottom: auto">
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


        </el-menu>
      </el-col>
    </el-row>


  </div>
</template>

<script>
import request from "@/utils/axios";

export default {
  name: "Home",
  data() {
    return {
      activeIndex: '1',
      userInfo: {
        id: "",
        username: "",
        avatar: "",

      },
    };
  },
  created() {
    request.get('/sys/user/getuserInfo').then(res => {

      this.userInfo = res.data
    })
  },
  methods: {
    handleSelect(key, keyPath) {
      console.log(key, keyPath);
    },
    logout() {

      this.$axios.post("/logout").then(res => {
        this.$router.push("/login")
        this.$store.commit("resetState")
        localStorage.clear()
      })

    }
  }
}
</script>

<style scoped>
.el-menu {


}

.el-dropdown-link {
  cursor: pointer;
}

.el-menu-demo {
  display: flex;
}

a {
  text-decoration: none;
}
</style>
