<template>
  <div class="bg">
    <el-row>
      <el-col>
        <div class="grid-content bg-purple-dark">
          <span style="color: white;margin-left: 15px">校园失物管理平台</span>
          <span style="color: white;margin-left: 15px">超级管理员账号: admin 密码: 11111</span>
        </div>
      </el-col>

    </el-row>
    <el-card class="box">
      <el-form :model="ruleForm" :rules="rules" ref="ruleForm" label-width="70px" class="demo-ruleForm">
        <h2 style="text-align: center;margin-bottom: 30px">LOGIN</h2>
        <el-form-item label="账户" prop="username" required>
          <el-input v-model="ruleForm.username"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" required>
          <el-input v-model="ruleForm.password" type="password"></el-input>
        </el-form-item>


        <el-form-item label="验证码" prop="code" required>
          <el-col :span="11">

            <el-input v-model="ruleForm.code" style="width: 140px"></el-input>

          </el-col>
          <el-col class="line" :span="4">&nbsp;&nbsp;&nbsp;</el-col>
          <el-col :span="9">
            <el-image :src="captchaImg" @click="getCaptcha"
                      style="width: 100%;height: 35px;border-radius: 5px"></el-image>
          </el-col>
        </el-form-item>

        <el-form-item style="margin-left: 30px">
          <el-button type="primary" plain round @click="submitForm('ruleForm')">立即登录</el-button>
          <el-button plain round @click="resetForm('ruleForm')">重置</el-button>
          <el-button type="success" plain round @click="dialogVisible=true">立即注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!--  注册-->
    <el-dialog
        title="欢迎注册"
        :visible.sync="dialogVisible"
        width="600px"
        :before-close="handleClose">

      <el-form :model="editForm" :rules="editFormRules" ref="editForm" label-width="100px" class="demo-editForm">

        <el-form-item label="账户名" prop="username" label-width="100px">
          <el-input v-model="editForm.username" autocomplete="off"></el-input>
        </el-form-item>


        <!--        <el-form-item label="密码" prop="password" label-width="100px">-->
        <!--          <el-input v-model="editForm.password" autocomplete="off"></el-input>-->
        <!--        </el-form-item>-->
        <!--        <el-form-item label="确认密码" prop="rpassword" label-width="100px">-->
        <!--          <el-input v-model="editForm.rpassword" autocomplete="off"></el-input>-->
        <!--        </el-form-item>-->
        <el-form-item label="密码" prop="password">
          <el-input type="password" v-model="editForm.password" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="rpassword">
          <el-input type="password" v-model="editForm.rpassword" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitRegister('editForm')">立即创建</el-button>
          <el-button @click="resetForm('editForm')">重置</el-button>
        </el-form-item>
      </el-form>

    </el-dialog>


  </div>


</template>

<script>
import request from "@/utils/axios";
import qs from "qs";
import axios from "axios";

export default {
  name: "Login",
  data() {
    var validatePass = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入密码'));
      } else {


        this.$refs.ruleForm.validateField('rpassword');

        callback();
      }
    };
    var validatePass2 = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入密码'));
      } else if (value !== this.editForm.password) {
        callback(new Error('两次输入密码不一致!'));
      } else {
        callback();
      }
    };
    return {
      ruleForm: {},
      editForm: {
        username: '',
        password: '',
        rpassword: ''
      },
      dialogVisible: false,
      captchaImg: '',
      rules: {
        username: [
          {required: true, message: '请输入账户', trigger: ["change", "blur"]},
          {min: 5, max: 10, message: '长度在 5 到 10 个字符', trigger: 'blur'}
        ],
        password: [
          {required: true, message: '请输入密码', trigger: ["change", "blur"]},
          {min: 5, max: 10, message: '长度在 5到 10 个字符', trigger: 'blur'}
        ],
        code: [
          {required: true, message: '输入5位数验证码', trigger: ["change", "blur"]},
          {min: 5, max: 5, message: '输入5个字符', trigger: 'blur'}
        ],
      },
      editFormRules: {
        username: [
          {required: true, message: '请输入账户', trigger: ["change", "blur"]},
          {min: 5, max: 10, message: '长度在 5 到 10 个字符', trigger: 'blur'}
        ],

        password: [
          {validator: validatePass, trigger: 'blur'},
          {min: 5, max: 10, message: '长度在 5 到 10 个字符', trigger: 'blur'}
        ],
        rpassword: [
          {validator: validatePass2, trigger: 'blur'},
          {min: 5, max: 10, message: '长度在 5 到 10 个字符', trigger: 'blur'}
        ],
      }
    };
  },
  created() {
    this.getCaptcha()
  },
  methods: {
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {

        if (valid) {

          axios.post('/login?' + qs.stringify(this.ruleForm)).then(res => {

            if (res.data.code != 200) {
              this.$message.error(res.data.msg)
              this.getCaptcha()
              return false
            }

            localStorage.setItem("token", res.headers['authorization'])
            this.$store.commit('SET_TOKEN', res.headers['authorization'])

            this.$router.push('/index')
          })
        } else {


          return false;
        }
      });
    },
    submitRegister(formName) {
      this.$refs[formName].validate((valid) => {

        if (valid) {

          axios.post('/sys/user/register/' ,this.editForm).then(res => {

            if (res.data.code == 200) {
              this.$message.success('注册成功请登陆!')
              this.dialogVisible = false
              this.getCaptcha()

            }else{
              this.$message.error(res.data.msg)

            }

          })
        }
        // else {
        //
        //
        //   return false
        //
        // }
      });
    },
    getCaptcha() {
      request.get('/captcha').then(res => {
        this.captchaImg = "data:image/gif;base64," + res.data.captchaImg
        this.ruleForm.token = res.data.token

      })
    },
    resetForm(formName) {
      this.$refs[formName].resetFields();
    },

    handleClose() {
      this.resetForm('editForm')
      this.dialogVisible = false
      this.editForm = {}
    },
    handleAvatarSuccess(res) {
      console.log(res.data)
      this.editForm.avator = res.data
    },

    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg';
      const isLt2M = file.size / 1024 / 1024 < 2;

      if (!isJPG) {
        this.$message.error('上传头像图片只能是 JPG 格式!');
      }
      if (!isLt2M) {
        this.$message.error('上传头像图片大小不能超过 2MB!');
      }
      return isJPG && isLt2M;
    }
  }
}
</script>

<style scoped>
.el-row {
  margin-bottom: 20px;

&
:last-child {
  margin-bottom: 0;
}

}
.el-col {
  border-radius: 0px;
}

.bg-purple-dark {
  background: rgba(255, 120, 0, 0.25)
}

.bg-purple {
  background: #d3dce6;
}

.bg-purple-light {
  background: #e5e9f2;
}

.box {
  width: 70%;
  margin: 50px auto;
  border: none;
  background: rgba(255, 120, 0, 0.05)
}

/* 改变input框背景颜色 */
/deep/ .el-input__inner {
  background-color: rgba(0, 0, 0, 0.1);
  border: none;
  color: #f9fafc;
}


.grid-content {
  border-radius: 0px;
  text-align: left;
  font-size: 20px;
  font-family: "仿宋";
  font-weight: normal;
  line-height: 70px;
  min-height: 70px;
}


.bg {
  width: 100%;
  height: 100%;
  background: url("../assets/cbg.png") no-repeat;
  background-size: cover;
}


.demo-ruleForm {
  width: 380px;

  margin: 100px auto;

}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.avatar-uploader .el-upload:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar {
  width: 178px;
  height: 178px;
  display: block;
}


</style>
