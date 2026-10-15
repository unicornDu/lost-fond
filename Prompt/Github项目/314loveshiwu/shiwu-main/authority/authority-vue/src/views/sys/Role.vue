<template>
<div>
  <div>
    <el-form :inline="true">
      <el-form-item>
        <el-input
            v-model="searchForm.name"
            placeholder="名称"
            clearable
        >
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-button @click="getRoleList">搜索</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" plain @click="reset">重置</el-button>
      </el-form-item>

      <el-form-item>
        <el-popconfirm title="这是确定批量删除吗？" @confirm="delHandle(null)">
          <el-button type="danger" slot="reference" :disabled="delBtlStatu">批量删除</el-button>
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
          width="55">
      </el-table-column>

      <el-table-column
          prop="name"
          label="名称"
          width="120">
      </el-table-column>
      <el-table-column
          prop="code"
          label="唯一编码"
          show-overflow-tooltip>
      </el-table-column>
      <el-table-column
          prop="remark"
          label="描述"
          show-overflow-tooltip>
      </el-table-column>

      <el-table-column
          prop="statu"
          label="状态">
        <template slot-scope="scope">
          <el-tag size="small" v-if="scope.row.statu === 1" type="success">正常</el-tag>
          <el-tag size="small" v-else-if="scope.row.statu === 0" type="danger">禁用</el-tag>
        </template>

      </el-table-column>
      <el-table-column
          prop="icon"
          label="操作">

        <template slot-scope="scope">
          <el-button type="text" @click="permHandle(scope.row.id)">分配权限</el-button>
          <el-divider direction="vertical"></el-divider>


          <template>
            <el-popconfirm title="这是一段内容确定删除吗？" @confirm="delHandle(scope.row.id)">
              <el-button type="text" slot="reference">删除</el-button>
            </el-popconfirm>
          </template>

        </template>
      </el-table-column>

    </el-table>


    <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[5, 10, 15, 20]"
        :current-page="current"
        :page-size="size"
        :total="total">
    </el-pagination>


    <el-dialog
        title="提示"
        :visible.sync="dialogVisible"
        width="600px"
        :before-close="handleClose">

      <el-form :model="editForm" :rules="editFormRules" ref="editForm" label-width="100px" class="demo-editForm">

        <el-form-item label="角色名称" prop="name" label-width="100px">
          <el-input v-model="editForm.name" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="唯一编码" prop="code" label-width="100px">
          <el-input v-model="editForm.code" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="描述" prop="remark" label-width="100px">
          <el-input v-model="editForm.remark" autocomplete="off"></el-input>
        </el-form-item>


        <el-form-item label="状态" prop="statu" label-width="100px">
          <el-radio-group v-model="editForm.statu">
            <el-radio :label=0>禁用</el-radio>
            <el-radio :label=1>正常</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm('editForm')">立即创建</el-button>
          <el-button @click="resetForm('editForm')">重置</el-button>
        </el-form-item>
      </el-form>

    </el-dialog>

    <el-dialog
        title="分配权限"
        :visible.sync="permDialogVisible"
        width="600px">

      <el-form :model="permForm">

        <el-tree
            :data="permTreeData"
            show-checkbox
            ref="permTree"
            :default-expand-all=true
            node-key="id"
            :check-strictly=true
            :props="defaultProps">
        </el-tree>

      </el-form>

      <span slot="footer" class="dialog-footer">
			    <el-button @click="permDialogVisible = false">取 消</el-button>
			    <el-button type="primary" @click="submitPermFormHandle('permForm')">确 定</el-button>
			</span>

    </el-dialog>

  </div>


</div>
</template>

<script>
import request from "@/utils/axios";

export default {
  name: "Role",
  data() {
    return {
      searchForm: {},
      delBtlStatu: true,

      total: 0,
      size: 5,
      current: 1,

      dialogVisible: false,
      editForm: {

      },

      tableData: [],

      editFormRules: {
        name: [
          {required: true, message: '请输入角色名称', trigger: 'blur'}
        ],
        code: [
          {required: true, message: '请输入唯一编码', trigger: 'blur'}
        ],
        statu: [
          {required: true, message: '请选择状态', trigger: 'blur'}
        ]
      },

      multipleSelection: [],

      permDialogVisible: false,
      permForm: {},
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      permTreeData: []
    }
  },
  created() {
    this.getRoleList()

    request.get('/sys/menu/list').then(res => {
      this.permTreeData = res.data
    })
  },
  methods: {reset(){
      this.searchForm.name=""
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
      this.getRoleList()
    },
    handleCurrentChange(val) {
      console.log(`当前页: ${val}`);
      this.current = val
      this.getRoleList()
    },

    resetForm(formName) {


      this.dialogVisible = false


      this[formName] = {};

      this.$nextTick(() => {
        this.$refs[formName].resetFields()
      });

    },
    handleClose() {
      this.resetForm('editForm')
    },

    getRoleList() {
      request.get("/sys/role/page", {
        params: {
          name: this.searchForm.name,
          pageNum: this.current,
          pageSize: this.size
        }
      }).then(res => {
        console.log(res)
        this.tableData = res.data.records
        this.size = res.data.pageSize
        this.current = res.data.pageNum
        this.total = res.data.total
      })
    },

    submitForm(formName) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          request.post('/sys/role/' + (this.editForm.id?'update' : 'save'), this.editForm)

              .then(res => {
                if(res.code==200){


                this.$message({
                  showClose: true,
                  message: '恭喜你，操作成功',
                  type: 'success',
                  onClose:() => {
                    this.getRoleList()
                  }
                });

                this.dialogVisible = false
                this.resetForm(formName)
                this.getRoleList()
                }
                else {
                  this.$message.error(res.data)
                  return false
                }
              })
        }
      });
    },
    editHandle(id) {
      request.get('/sys/role/info/' + id).then(res => {
        this.editForm = res.data
        this.dialogVisible = true
      })
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



      request.post("/sys/role/delete", ids).then(res => {
        if(res.code==200){
          this.$message.success("删除成功")
          this.getRoleList()
        }else {
          this.$message.error("删除失败")
        }

      })
    },
    permHandle(id) {
      this.permDialogVisible = true

      request.get("/sys/role/info/" + id).then(res => {
        console.log(res.data.menuIds)
        this.$refs.permTree.setCheckedKeys(res.data.menuIds)
        this.permForm = res.data
      })
    },

    submitPermFormHandle(formName) {
      var menuIds = this.$refs.permTree.getCheckedKeys()



      this.$axios.post('/sys/role/perm/' + this.permForm.id, menuIds).then(res => {
        this.$message({
          showClose: true,
          message: '恭喜你，操作成功',
          type: 'success',
          onClose:() => {
            this.getRoleList()
          }
        });
        this.permDialogVisible = false

      })
    }
  }

}
</script>

<style scoped>

</style>
