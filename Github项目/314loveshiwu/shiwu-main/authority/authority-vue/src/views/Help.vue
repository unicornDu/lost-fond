<template>
  <div>
    <div>
      <el-form :inline="true">
        <el-form-item>
          <el-input
              v-model="name"
              placeholder="输入查询值"
              clearable
          >
          </el-input>
        </el-form-item>

        <el-form-item >
          <el-button type="primary" plain @click="load1">搜索</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain @click="reset">重置</el-button>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" plain @click="handleAdd">上传丢失的物品</el-button>
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
          @selection-change="handleSelectionChange"
      >

        <el-table-column
            type="selection"
            width="55">
        </el-table-column>

              
                <el-table-column
                    prop="id"
                    label="ID"
                    align="center"
                    width="70">
                </el-table-column>



              
                <el-table-column
                    prop="name"
                    label="丢失物品名称"
                    align="center">
                </el-table-column>



              
                <el-table-column
                    align="center"
                    label="物品图片">
                  <template slot-scope="scope">
                    <el-image style="width: 100px; height: 70px" :src="scope.row.img"  :preview-src-list="[scope.row.img]">
                    </el-image>
                  </template>
                </el-table-column>





              
                <el-table-column
                    prop="date"
                    label="丢失日期"
                    align="center">
                </el-table-column>



              
                <el-table-column
                    prop="place"
                    label="丢失地点"
                    align="center">
                </el-table-column>



              
                <el-table-column
                    prop="phone"
                    label="联系电话"
                    align="center">
                </el-table-column>



              
                <el-table-column
                    prop="remark"
                    label="备注"
                    align="center">
                </el-table-column>



        <el-table-column label="操作" width="180" align="center"v-if="userInfo.username=='admin'">
          <template slot-scope="scope">
            <el-button type="success" plain round @click="editHandle(scope.row)">编辑</el-button>
            <el-popconfirm
                class="ml-5"
                confirm-button-text='确定'
                cancel-button-text='我再想想'
                icon="el-icon-info"
                icon-color="red"
                title="您确定删除吗？"
                @confirm="del(scope.row.id)"
            >
              <el-button type="danger" slot="reference" plain round>删除</el-button>
            </el-popconfirm>
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

<el-form :model="form" :rules="formRules" ref="editForm">

 <el-form-item label="丢失物品名称"
prop="name"

label-width="100px">
<el-input v-model="form.name" autocomplete="off"></el-input>
</el-form-item>
<el-form-item label="物品图片"
label-width="100px">
<el-upload action="http://localhost:8888/file/upload" ref="img" :on-success="handleimg">
<el-button size="small" type="primary">点击上传</el-button>
</el-upload>
</el-form-item>
<el-form-item label="丢失日期"
label-width="100px">
<el-date-picker v-model="form.date" type="date" value-format="yyyy-MM-dd"
placeholder="选择日期">
</el-date-picker>
</el-form-item>
<el-form-item label="丢失地点"
prop="place"

label-width="100px">
<el-input v-model="form.place" autocomplete="off"></el-input>
</el-form-item>
<el-form-item label="联系电话"
prop="phone"

label-width="100px">
<el-input v-model="form.phone" autocomplete="off"></el-input>
</el-form-item>
<el-form-item label="备注"
label-width="100px">
<el-input v-model="form.remark" autocomplete="off"></el-input>
</el-form-item>
 </el-form>

        <div slot="footer" class="dialog-footer">
          <el-button @click="resetForm('editForm')">取 消</el-button>
          <el-button type="primary" @click="submitForm('editForm')">确 定</el-button>
        </div>
      </el-dialog>


    </div>


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
      let validateimg= (rule, value, callback) => {
        if (this.form.img==null||this.form.img=='') {//我控制了FileList 长度代表文件个数
          callback(new Error("必选项不能为空"))
        } else {
          callback()
        }
      }
      return {
        userInfo: {
          username: "",

        },
        total: 0,
        size: 5,
        current: 1,
        name: '',
        form: {},
        formRules: {
      id:   [
              {required: true, message: '必选项不能为空', trigger: 'blur'}],
      name:   [
              {required: true, message: '必选项不能为空', trigger: 'blur'}],
      place:   [
              {required: true, message: '必选项不能为空', trigger: 'blur'}],
          phone: [{
            required: true,
            message: '请填写手机号码',
            trigger: 'blur',
          }, {
            validator: checkMobile,
            trigger: 'change',
          }]},
          multipleSelection: [],
          delBtlStatu:true,
        dialogVisible: false,
        tableData: []
      }
    },
    created() {
      this.load()
      this.getUserInfo()
    },
    methods: {
      reset(){
        this.name=""
      },
      load1(){
        request.post("/help/listPage",{
          // 前面的为属性名
          pageNum:this.current,
          pageSize:this.size,
          param:{
            name:this.name,
          }
        }).then(res => {
         // console.log(res)
          this.tableData = res.data
        })
      },
      getUserInfo() {

        request.get('/sys/user/getuserInfo').then(res => {

          this.userInfo = res.data
        })
      },
      //批量勾选
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
      //图片上传
              handleimg (res) {
                this.form.img = res.data

              },
        //文件上传

      //添加对话框
      handleAdd() {
        this.dialogVisible = true
        this.form = {}
      this.$nextTick(() => {
          if (this.$refs.img) {
            this.$refs.img.clearFiles();
          }
          if (this.$refs.file) {
            this.$refs.file.clearFiles();
          }
        })
      },
      //关闭对话框
      handleClose() {
        this.resetForm('editForm')
      },
      //重置对话框
      resetForm(formName) {

      this.$refs[formName].resetFields()
        this.dialogVisible = false
        this.form = {}
      },
      //获取分页
      load() {
        request.get("/help/page", {
          params: {
            name: this.name,
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
      // 翻页
      handleSizeChange(val) {

        this.size = val
        this.load()
      },
      handleCurrentChange(val) {

        this.current = val
        this.load()
      },
  // 上传成功
  onSuccess(response, file, fileList){
    console.log("onSuccess:response",response)
    console.log("onSuccess:file",file)
    console.log("onSuccess:fileList",fileList)
    //等待所有文件都上传完成，这里注意fileList是所有的文件（包含已上传的）
    if(fileList.every(it => it.status == 'success')) {
      fileList.map(item => {
        //只push新上传的 （带有response）
        item.response && this.fileList.push({name:item.response.data.originalName,url:item.response.data.link});
      })
      setTimeout(()=>{
        this.$emit("input", this.fileList)
      })
    }
  },
  //新增或者修改
  submitForm(formName) {
    this.$refs[formName].validate((valid) => {
    if (valid) {
    request.post("/help" , this.form)
    .then(res => {

    this.$message({
    showClose: true,
    message: '恭喜你，操作成功',
    type: 'success',

    });
    this.load()
    this.resetForm('editForm')
    this.dialogVisible = false
    })
    } else {

    return false;
    }
    });
  },
  //修改赋值
  editHandle(row){
  this.form = JSON.parse(JSON.stringify(row))
  this.dialogVisible = true
    },
  del(id) {
    request.delete("/help/" + id).then(res => {
    this.$message.success('删除成功')
    this.load()
    })
  },
  // 文件下载
  download(url){
    window.open(url)
  },




  //批量删除
  delHandle(id) {

    var ids = []


    this.multipleSelection.forEach(row => {
      ids.push(row.id)
    })




    request.post("/help/delete/"+ ids).then(res => {

      this.$message.success("删除成功")
      this.load()


    })
  },
  }
  }
</script>

<style scoped>

</style>
