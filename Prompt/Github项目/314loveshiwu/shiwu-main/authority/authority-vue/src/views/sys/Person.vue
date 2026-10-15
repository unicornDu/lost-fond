<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="grid-content bg-purple">
          <el-card style="height: 100%">
            <div style="text-align: center;line-height: 40px;font-size: larger"><h3>个人信息</h3></div>
            <el-divider></el-divider>

            <el-row :getter="24">
              <el-col :span="10" :offset="7">
                <!--                <div style="height: 100px;width: 100%;border: 1px seagreen solid"></div>-->
                <el-upload
                    class="avatar-uploader"

                    action="http://localhost:8888/file/upload"
                    :show-file-list="false"
                    :on-success="handleAvatarSuccess"
                    :before-upload="beforeAvatarUpload">

                  <el-avatar class="avatar" icon="el-icon-user-solid" :src="userInfo.avatar"></el-avatar>

                </el-upload>
              </el-col>


            </el-row>
            <el-divider></el-divider>
            <el-row>
              <el-col :span="12" :offset="1">

                <span style="font-size: 18px;font-weight: inherit"> <i class="el-icon-info"></i>账户名</span>
              </el-col>
              <el-col :span="10" :offset="1">

                <span style="font-size: 18px;font-weight: inherit;">{{ userInfo.username }}</span>
              </el-col>
            </el-row>
            <el-divider></el-divider>
            <el-row>
              <el-col :span="6" :offset="1">

                <span style="font-size: 18px;font-weight: inherit"> <i class="el-icon-edit"></i>邮箱</span>
              </el-col>
              <el-col :span="16" :offset="1">

                <span style="font-size: 18px;font-weight: inherit;">{{ userInfo.email }}</span>
              </el-col>
            </el-row>
            <el-divider></el-divider>
            <el-row>
              <el-col :span="8" :offset="1">

                <span style="font-size: 18px;font-weight: inherit"> <i class="el-icon-phone"></i>手机号</span>
              </el-col>
              <el-col :span="14" :offset="1">

                <span style="font-size: 18px;font-weight: inherit;">{{ userInfo.phone }}</span>
              </el-col>
            </el-row>
            <el-divider></el-divider>
            <el-row>
              <el-col :span="12" :offset="1">

                <span style="font-size: 18px;font-weight: inherit"> <i class="el-icon-user-solid"></i>用户城市</span>
              </el-col>
              <el-col :span="10" :offset="1">
                <el-tag>{{ userInfo.city }}</el-tag>

              </el-col>
            </el-row>
            <el-divider></el-divider>

          </el-card>


        </div>
      </el-col>
      <el-col :span="13" :offset="1">
        <div class="grid-content bg-purple">
          <el-card style="height: 100%">
            <div style="text-align: center;line-height: 40px;font-size: larger"><h3>信息修改</h3></div>
            <el-divider></el-divider>
            <el-form :model="userInfo" :rules="rules" ref="ruleForm" label-width="150px" class="demo-ruleForm">
              <el-form-item label="账户名称" prop="name">
                <el-input v-model="userInfo.username" style="width: 400px;height: 50px" disabled></el-input>
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userInfo.phone" clearable maxlength="11" style="width: 400px;height: 50px"
                >
                  >
                </el-input>
              </el-form-item>
              <el-form-item label="邮箱" prop="email"
                            :rules="[
      { required: true, message: '请输入邮箱地址', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
    ]">
                <el-input v-model="userInfo.email" style="width: 400px;height: 50px"></el-input>
              </el-form-item>
              <el-form-item label="所在城市" prop="city">
                <el-input v-model="userInfo.city" style="width: 400px;height: 50px"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitForm('ruleForm')" style="width: 65%;margin-top: 80px">立即修改
                </el-button>

              </el-form-item>
            </el-form>


          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import request from "@/utils/axios";

export default {
  name: "Menu",
  data() {
    // 验证手机号的校验规则
    let checkMobile = (rule, value, callback) => {
      // 验证手机号的正则表达式
      const regMobile = /^1(3|4|5|6|7|8|9)\d{9}$/;

      if (regMobile.test(value)) {
        // 合法的手机号
        return callback()
      }
      // 不合法
      callback(new Error('请输入合法的手机号'))
    }


    return {
      imageUrl: '',
      userInfo: {},
      rules: {

        city: [
          {required: true, message: '请输入城市名称', trigger: 'blur'},
          {min: 3, max: 5, message: '长度在 3 到 5 个字符', trigger: 'blur'}
        ],
        email: [
          {required: true, message: '请输入城市名称', trigger: 'blur'},
          {min: 3, max: 5, message: '长度在 3 到 5 个字符', trigger: 'blur'}
        ],

        phone: [{
          required: true,
          message: '请填写手机号码',
          trigger: 'blur',
        }, {
          validator: checkMobile,
          trigger: 'change',
        }],

        region: [
          {required: true, message: '请选择活动区域', trigger: 'change'}
        ],
        date1: [
          {type: 'date', required: true, message: '请选择日期', trigger: 'change'}
        ],
        date2: [
          {type: 'date', required: true, message: '请选择时间', trigger: 'change'}
        ],
        type: [
          {type: 'array', required: true, message: '请至少选择一个活动性质', trigger: 'change'}
        ],
        resource: [
          {required: true, message: '请选择活动资源', trigger: 'change'}
        ],
        desc: [
          {required: true, message: '请填写活动形式', trigger: 'blur'}
        ]
      }
    };
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
    handleAvatarSuccess(res) {

      this.userInfo.avatar = res.data
      this.save()
    },

    beforeAvatarUpload(file) {
      const isJPG = file.type === 'image/jpeg';
      const isLt2M = file.size / 1024 / 1024 < 2;


      // return isJPG && isLt2M;
    },
    save() {
      request.post("/sys/user/update", this.userInfo).then(res => {
        this.$message.success({
          message:'操作成功',
          duration:3000
        })
        let _this = this;
        setTimeout(function(){
          _this.$router.go(0);
        },1000);

      })
    },
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.save()
        } else {

          return false;
        }
      });
    }
  },

}

</script>

<style scoped>
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  margin: 0 auto;
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
  width: 120px;
  height: 120px;
  display: block;

}

.el-row {
  margin-bottom: 20px;

&
:last-child {
  margin-bottom: 0;
}

}
.el-col {
  border-radius: 4px;
}

.bg-purple-dark {
  background: #99a9bf;
}

.bg-purple {
  background: #d3dce6;
  height: 600px;
  margin-top: 20px;
}

.bg-purple-light {
  background: #e5e9f2;
}

.grid-content {
  border-radius: 4px;
  min-height: 36px;
}

.row-bg {
  padding: 10px 0;
  background-color: #f9fafc;
}
</style>
