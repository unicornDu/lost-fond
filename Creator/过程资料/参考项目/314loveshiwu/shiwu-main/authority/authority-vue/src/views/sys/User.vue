<template>
  <div>
    <el-form :inline="true">
      <el-form-item>
        <el-input
            v-model="searchForm.username"
            placeholder="用户名"
            clearable
        >
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-button @click="getUserList">搜索</el-button>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" plain @click="reset">重置</el-button>
      </el-form-item>
      <el-form-item>
        <el-popconfirm title="这是确定批量删除吗？" @confirm="delHandle(null)">
          <el-button type="danger" slot="reference" :disabled="delBtlStatu"v-if="userInfo.username=='admin'">批量删除</el-button>
        </el-popconfirm>
      </el-form-item>
    </el-form>

    <el-table
        ref="multipleTable"
        :data="tableData"
        tooltip-effect="dark"
        style="width: 100%"
        border
        stripe
        @selection-change="handleSelectionChange">

      <el-table-column
          type="selection"
          width="auto">
      </el-table-column>

      <el-table-column
          label="头像"
          width="auto">
        <template slot-scope="scope">
          <el-avatar size="small" :src="scope.row.avatar"></el-avatar>
        </template>
      </el-table-column>

      <el-table-column
          prop="username"
          label="用户名"
          width="auto">
      </el-table-column>
      <el-table-column
          prop="code"
          align="center"
          label="角色名称"
          width="auto">
        <template slot-scope="scope">
          <el-button size="small" type="text" v-for="item in scope.row.sysRoles":key="item.id">{{ item.name }}</el-button>

        </template>

      </el-table-column>
      <el-table-column
          prop="email"
          label="邮箱"
          width="auto">
      </el-table-column>
      <el-table-column
          prop="phone"
          label="手机号"
          width="auto">
      </el-table-column>



      <el-table-column
          prop="icon"
          width="auto"
          label="操作"
          v-if="userInfo.username=='admin'">

        <template slot-scope="scope">


          <template>
            <el-popconfirm title="这是一段内容确定删除吗？" @confirm="delHandle(scope.row.id)">
              <el-button type="text" slot="reference"v-if="userInfo.username=='admin'">删除</el-button>
            </el-popconfirm>
          </template>

        </template>
      </el-table-column>

    </el-table>

    <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :current-page="current"
        :page-size="size"
        :total="total">
    </el-pagination>


    <!--新增对话框-->
    <el-dialog
        title="提示"
        :visible.sync="dialogVisible"
        width="600px"
        :before-close="handleClose">

      <el-form :model="editForm" :rules="editFormRules" ref="editForm">
        <el-form-item label="用户名" prop="username" label-width="100px">
          <el-input v-model="editForm.username" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="邮箱" prop="email" label-width="100px">
          <el-input v-model="editForm.email" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="手机号" prop="phone" label-width="100px">
          <el-input v-model="editForm.phone" autocomplete="off"></el-input>
        </el-form-item>



      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="resetForm('editForm')">取 消</el-button>
        <el-button type="primary" @click="submitForm('editForm')">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import request from "@/utils/axios";

export default {
  name: "Role",
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
          var checkEmail = (rule, value, cb) => {
            //验证邮箱的正则表达式
            const regEmail = /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/;
            if (regEmail.test(value)) {
              //合法的邮箱
              return cb();
            }
            cb(new Error("请输入合法的邮箱"));
          };

    return {
      userInfo: {
        username: "",

      },
      searchForm: {},
      delBtlStatu: true,

      total: 0,
      size: 10,
      current: 1,

      dialogVisible: false,
      editForm: {},

      tableData: [],

      editFormRules: {
        username: [
          {required: true, message: '请输入用户名称', trigger: 'blur'}
        ],
        email: [
          {required: true, message: '请输入邮箱', trigger: 'blur'},
          {
            validator: checkEmail,
            trigger: 'change',
          }
        ],
        phone: [{
          required: true,
          message: '请填写手机号码',
          trigger: 'blur',
        }, {
          validator: checkMobile,
          trigger: 'change',
        }]

      },

      multipleSelection: [],

      roleDialogFormVisible: false,
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      roleForm: {},
      roleTreeData: [],
      treeCheckedKeys: [],
      checkStrictly: true

    }
  },
  created() {
    this.getUserList()
    this.getUserInfo()

    this.$axios.get("/sys/role/page").then(res => {
      this.roleTreeData = res.data.records
    })
  },
  methods: {
    getUserInfo() {

      request.get('/sys/user/getuserInfo').then(res => {

        this.userInfo = res.data
      })
    },
    reset(){
      this.searchForm.username=""
    },
    toggleSelection(rows) {
      if (rows) {
        rows.forEach(row => {
          this.$refs.multipleTable.toggleRowSelection(row);
        });
      } else {
        this.$refs.multipleTable.clearSelection();
      }
    },
    handleSelectionChange(val) {
      console.log("勾选")
      console.log(val)
      this.multipleSelection = val;

      this.delBtlStatu = val.length == 0
    },

    handleSizeChange(val) {
      console.log(`每页 ${val} 条`);
      this.size = val
      this.getUserList()
    },
    handleCurrentChange(val) {
      console.log(`当前页: ${val}`);
      this.current = val
      this.getUserList()
    },

    resetForm(formName) {
      this.$refs[formName].resetFields();
      this.dialogVisible = false
      this.editForm = {}
    },
    handleClose() {
      this.resetForm('editForm')
    },

    getUserList() {
      this.$axios.get("/sys/user/page", {

        params: {
          username: this.searchForm.username,
          pageNum: this.current,
          pageSize: this.size
        }
      }).then(res => {

        this.tableData = res.data.records
        this.size = res.data.size
        this.current = res.data.current
        this.total = res.data.total
      })
    },

    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.$axios.post('/sys/user/' + (this.editForm.id ? 'update' : 'save'), this.editForm)
              .then(res => {

                this.$message({
                  showClose: true,
                  message: '恭喜你，操作成功',
                  type: 'success',
                  onClose: () => {
                    this.getUserList()
                  }
                });

                this.dialogVisible = false
              })
        } else {
          console.log('error submit!!');
          return false;
        }
      });
    },

    delHandle(id) {

      var ids = []

      if (id) {
        ids.push(id)
      } else {
        this.multipleSelection.forEach(row => {
          ids.push(row.id)
        })
      }


      this.$axios.post("/sys/user/delete", ids).then(res => {
        this.$message({
          showClose: true,
          message: '恭喜你，操作成功',
          type: 'success',
          onClose: () => {
            this.getUserList()
          }
        });
      })
    },

    roleHandle(id) {
      this.roleDialogFormVisible = true

      this.$axios.post('/sys/user/info/' + id).then(res => {
        this.roleForm = res.data

        let roleIds = []
        res.data.roles.forEach(row => {
          roleIds.push(row.id)
        })

        this.$refs.roleTree.setCheckedKeys(roleIds)
      })
    },
    submitRoleHandle(formName) {
      var roleIds = this.$refs.roleTree.getCheckedKeys()


      this.$axios.post('/sys/user/perm/' + this.roleForm.id, roleIds).then(res => {
        this.$message({
          showClose: true,
          message: '恭喜你，操作成功',
          type: 'success',
          onClose: () => {
            this.getUserList()
          }
        });

        this.roleDialogFormVisible = false
      })
    },
    repassHandle(id, username) {

      this.$confirm('将重置用户【' + username + '】的密码, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$axios.post("/sys/user/repass", id).then(res => {
          this.$message({
            showClose: true,
            message: '恭喜你，操作成功',
            type: 'success',
            onClose: () => {
            }
          });
        })
      })
    }
  }
}
</script>

<style scoped>

.el-pagination {
  float: right;
  margin-top: 22px;
}

</style>
