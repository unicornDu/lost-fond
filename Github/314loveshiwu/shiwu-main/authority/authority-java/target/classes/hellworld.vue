<template>
  <div>角色管理
    <div>

      <el-table
              ref="multipleTable"
              :data="tableData"
              tooltip-effect="dark"
              style="width: 100%"
              border
              stripe
      >

        <el-table-column
                type="selection"
                width="55">
        </el-table-column>

          <el-table-column
                  prop="username"
                  label="姓名"
                  align="center"
                  width="120">
          </el-table-column>

          <el-table-column
                  prop="username"
                  label="姓名"
                  align="center"
                  width="120">
          </el-table-column>

          <el-table-column
                  prop="username"
                  label="姓名"
                  align="center"
                  width="120">
          </el-table-column>

          <el-table-column
                  prop="username"
                  label="姓名"
                  align="center"
                  width="120">
          </el-table-column>









      </el-table>





    </div>


  </div>
</template>

<script>
  import request from "@/utils/axios";

  export default {
    name: "Role",
    data() {
      return{
        tableData:[
          {

            name: '王小虎',
            address: '上海市普陀区金沙江路 1518 弄'
          },
          {

            name: '王小虎',
            address: '上海市普陀区金沙江路 1518 弄'
          },
          {

            name: '王小虎',
            address: '上海市普陀区金沙江路 1518 弄'
          }

        ]
      }
    },
    created() {

    },
    methods: {



    }

  }
</script>

<style scoped>

</style>
