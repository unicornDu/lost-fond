<template>
  <div>

    <el-form :inline="true">
      <el-form-item>
        <el-button type="primary" @click="dialogVisible = true" plain>新增目录或者菜单</el-button>


      </el-form-item>

    </el-form>

    <el-table
        :data="tableData"
        style="width: 100%;margin-bottom: 20px;"
        row-key="id"
        border
        stripe

        :tree-props="{children: 'children', hasChildren: 'hasChildren'}">
      <el-table-column
          prop="icon"
          width="auto"
          align="center"
          label="图标">
        <template slot-scope="scope">
          <span :class="scope.row.icon"></span>
          <span v-if="scope.row.icon==null" class="el-icon-coin"></span>
        </template>
      </el-table-column>
      <el-table-column
          prop="name"
          label="名称"
          sortable
          align="center"
          width="auto">
      </el-table-column>
      <el-table-column
          prop="perms"
          label="权限编码"
          sortable
          align="center"
          width="auto">
      </el-table-column>


      <el-table-column
          prop="type"
          align="center"
          width="auto"
          label="类型">
        <template slot-scope="scope">
          <el-tag size="small" v-if="scope.row.type === 0">目录</el-tag>
          <el-tag size="small" v-else-if="scope.row.type === 1" type="success">菜单</el-tag>


        </template>

      </el-table-column>

      <el-table-column
          prop="path"
          align="center"
          width="auto"
          label="菜单URL">
      </el-table-column>
      <el-table-column
          prop="component"
          align="center"
          width="auto"
          label="菜单组件">
      </el-table-column>
      <el-table-column
          prop="orderNum"
          align="center"
          width="auto"
          label="排序号">
      </el-table-column>

      <el-table-column
          prop="icon"
          align="center"
          label="操作"
          width="auto"
      >

        <template slot-scope="scope">

          <el-button type="text" @click="editHandle(scope.row)">编辑</el-button>
          <el-divider direction="vertical"></el-divider>


          <template>
            <el-popconfirm title="这是一段内容确定删除吗？" @confirm="delHandle(scope.row.id)">
              <el-button type="text" plain slot="reference">删除</el-button>
            </el-popconfirm>
          </template>
          &nbsp;


        </template>
      </el-table-column>

    </el-table>


    <!--新增对话框-->
    <el-dialog
        title="提示"
        :visible.sync="dialogVisible"
        width="600px"
        :before-close="handleClose">

      <el-form :model="editForm" :rules="editFormRules" ref="editForm" label-width="100px" class="demo-editForm">

        <el-form-item label="上级菜单" prop="parentId">
          <el-select v-model="editForm.parentId" placeholder="请选择上级菜单">
            <el-option :label="0" :value="0"></el-option>
            <template v-for="item in tableData">
              <el-option v-if="item.parentId==0&&item.type==0" :label="item.name" :value="item.id"></el-option>

            </template>
          </el-select>
        </el-form-item>

        <el-form-item label="名称" prop="name" label-width="100px">
          <el-input v-model="editForm.name" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="权限编码" prop="perms" label-width="100px">
          <el-input v-model="editForm.perms" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="图标" prop="icon" label-width="100px">
          <el-select clearable v-model="editForm.icon" placeholder="请选择" style="width: 100%">
            <el-option v-for="item in options" :key="item" :label="item" :value="item">
              <i :class="item"/> {{ item }}
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="URL" prop="path" label-width="100px">
          <el-input v-model="editForm.path" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="组件" prop="component" label-width="100px">
          <el-input v-model="editForm.component" autocomplete="off"></el-input>
        </el-form-item>

        <el-form-item label="类型" prop="type" label-width="100px">
          <el-radio-group v-model="editForm.type">
            <el-radio :label=0>目录</el-radio>
            <el-radio :label=1>菜单</el-radio>

          </el-radio-group>
        </el-form-item>

        <el-form-item label="排序号" prop="orderNum" label-width="100px">
          <el-input-number v-model="editForm.orderNum" :min="1" label="排序号">1</el-input-number>
        </el-form-item>


        <el-form-item>
          <el-button type="primary" @click="submitForm('editForm')">立即创建</el-button>
          <el-button @click="resetForm('editForm')">重置</el-button>
        </el-form-item>
      </el-form>

    </el-dialog>

  </div>

</template>

<script>
export default {
  name: "Menu",
  data() {
    return {
      dialogVisible: false,
      editForm: {
        path: '',
        component: '',
      },
      editFormRules: {

        name: [
          {required: true, message: '请输入名称', trigger: 'blur'}
        ],
        perms: [
          {required: true, message: '请输入权限编码', trigger: 'blur'}
        ],
        type: [
          {required: true, message: '请选择状态', trigger: 'blur'}
        ],
        orderNum: [
          {required: true, message: '请填入排序号', trigger: 'blur'}
        ],
        statu: [
          {required: true, message: '请选择状态', trigger: 'blur'}
        ]
      },
      tableData: [],
      options: ["el-icon-platform-eleme",
        "el-icon-eleme",
        "el-icon-delete-solid",
        "el-icon-delete",
        "el-icon-s-tools",
        "el-icon-setting",
        "el-icon-user-solid",
        "el-icon-user",
        "el-icon-phone",
        "el-icon-phone-outline",
        "el-icon-more",
        "el-icon-more-outline",
        "el-icon-star-on",
        "el-icon-star-off",
        "el-icon-s-goods",
        "el-icon-goods",
        "el-icon-warning",
        "el-icon-warning-outline",
        "el-icon-question",
        "el-icon-info",
        "el-icon-remove",
        "el-icon-circle-plus",
        "el-icon-success",
        "el-icon-error",
        "el-icon-zoom-in",
        "el-icon-zoom-out",
        "el-icon-remove-outline",
        "el-icon-circle-plus-outline",
        "el-icon-circle-check",
        "el-icon-circle-close",
        "el-icon-s-help",
        "el-icon-help",
        "el-icon-minus",
        "el-icon-plus",
        "el-icon-check",
        "el-icon-close",
        "el-icon-picture",
        "el-icon-picture-outline",
        "el-icon-picture-outline-round",
        "el-icon-upload",
        "el-icon-upload2",
        "el-icon-download",
        "el-icon-camera-solid",
        "el-icon-camera",
        "el-icon-video-camera-solid",
        "el-icon-video-camera",
        "el-icon-message-solid",
        "el-icon-bell",
        "el-icon-s-cooperation",
        "el-icon-s-order",
        "el-icon-s-platform",
        "el-icon-s-fold",
        "el-icon-s-unfold",
        "el-icon-s-operation",
        "el-icon-s-promotion",
        "el-icon-s-home",
        "el-icon-s-release",
        "el-icon-s-ticket",
        "el-icon-s-management",
        "el-icon-s-open",
        "el-icon-s-shop",
        "el-icon-s-marketing",
        "el-icon-s-flag",
        "el-icon-s-comment",
        "el-icon-s-finance",
        "el-icon-s-claim",
        "el-icon-s-custom",
        "el-icon-s-opportunity",
        "el-icon-s-data",
        "el-icon-s-check",
        "el-icon-s-grid",
        "el-icon-menu",
        "el-icon-share",
        "el-icon-d-caret",
        "el-icon-caret-left",
        "el-icon-caret-right",
        "el-icon-caret-bottom",
        "el-icon-caret-top",
        "el-icon-bottom-left",
        "el-icon-bottom-right",
        "el-icon-back",
        "el-icon-right",
        "el-icon-bottom",
        "el-icon-top",
        "el-icon-top-left",
        "el-icon-top-right",
        "el-icon-arrow-left",
        "el-icon-arrow-right",
        "el-icon-arrow-down",
        "el-icon-arrow-up",
        "el-icon-d-arrow-left",
        "el-icon-d-arrow-right",
        "el-icon-video-pause",
        "el-icon-video-play",
        "el-icon-refresh",
        "el-icon-refresh-right",
        "el-icon-refresh-left",
        "el-icon-finished",
        "el-icon-sort",
        "el-icon-sort-up",
        "el-icon-sort-down",
        "el-icon-rank",
        "el-icon-loading",
        "el-icon-view",
        "el-icon-c-scale-to-original",
        "el-icon-date",
        "el-icon-edit",
        "el-icon-edit-outline",
        "el-icon-folder",
        "el-icon-folder-opened",
        "el-icon-folder-add",
        "el-icon-folder-remove",
        "el-icon-folder-delete",
        "el-icon-folder-checked",
        "el-icon-tickets",
        "el-icon-document-remove",
        "el-icon-document-delete",
        "el-icon-document-copy",
        "el-icon-document-checked",
        "el-icon-document",
        "el-icon-document-add",
        "el-icon-printer",
        "el-icon-paperclip",
        "el-icon-takeaway-box",
        "el-icon-search",
        "el-icon-monitor",
        "el-icon-attract",
        "el-icon-mobile",
        "el-icon-scissors",
        "el-icon-umbrella",
        "el-icon-headset",
        "el-icon-brush",
        "el-icon-mouse",
        "el-icon-coordinate",
        "el-icon-magic-stick",
        "el-icon-reading",
        "el-icon-data-line",
        "el-icon-data-board",
        "el-icon-pie-chart",
        "el-icon-data-analysis",
        "el-icon-collection-tag",
        "el-icon-film",
        "el-icon-suitcase",
        "el-icon-suitcase-1",
        "el-icon-receiving",
        "el-icon-collection",
        "el-icon-files",
        "el-icon-notebook-1",
        "el-icon-notebook-2",
        "el-icon-toilet-paper",
        "el-icon-office-building",
        "el-icon-school",
        "el-icon-table-lamp",
        "el-icon-house",
        "el-icon-no-smoking",
        "el-icon-smoking",
        "el-icon-shopping-cart-full",
        "el-icon-shopping-cart-1",
        "el-icon-shopping-cart-2",
        "el-icon-shopping-bag-1",
        "el-icon-shopping-bag-2",
        "el-icon-sold-out",
        "el-icon-sell",
        "el-icon-present",
        "el-icon-box",
        "el-icon-bank-card",
        "el-icon-money",
        "el-icon-coin",
        "el-icon-wallet",
        "el-icon-discount",
        "el-icon-price-tag",
        "el-icon-news",
        "el-icon-guide",
        "el-icon-male",
        "el-icon-female",
        "el-icon-thumb",
        "el-icon-cpu",
        "el-icon-link",
        "el-icon-connection",
        "el-icon-open",
        "el-icon-turn-off",
        "el-icon-set-up",
        "el-icon-chat-round",
        "el-icon-chat-line-round",
        "el-icon-chat-square",
        "el-icon-chat-dot-round",
        "el-icon-chat-dot-square",
        "el-icon-chat-line-square",
        "el-icon-message",
        "el-icon-postcard",
        "el-icon-position",
        "el-icon-turn-off-microphone",
        "el-icon-microphone",
        "el-icon-close-notification",
        "el-icon-bangzhu",
        "el-icon-time",
        "el-icon-odometer",
        "el-icon-crop",
        "el-icon-aim",
        "el-icon-switch-button",
        "el-icon-full-screen",
        "el-icon-copy-document",
        "el-icon-mic",
        "el-icon-stopwatch",
        "el-icon-medal-1",
        "el-icon-medal",
        "el-icon-trophy",
        "el-icon-trophy-1",
        "el-icon-first-aid-kit",
        "el-icon-discover",
        "el-icon-place",
        "el-icon-location",
        "el-icon-location-outline",
        "el-icon-location-information",
        "el-icon-add-location",
        "el-icon-delete-location",
        "el-icon-map-location",
        "el-icon-alarm-clock",
        "el-icon-timer",
        "el-icon-watch-1",
        "el-icon-watch",
        "el-icon-lock",
        "el-icon-unlock",
        "el-icon-key",
        "el-icon-service",
        "el-icon-mobile-phone",
        "el-icon-bicycle",
        "el-icon-truck",
        "el-icon-ship",
        "el-icon-basketball",
        "el-icon-football",
        "el-icon-soccer",
        "el-icon-baseball",
        "el-icon-wind-power",
        "el-icon-light-rain",
        "el-icon-lightning",
        "el-icon-heavy-rain",
        "el-icon-sunrise",
        "el-icon-sunrise-1",
        "el-icon-sunset",
        "el-icon-sunny",
        "el-icon-cloudy",
        "el-icon-partly-cloudy",
        "el-icon-cloudy-and-sunny",
        "el-icon-moon",
        "el-icon-moon-night",
        "el-icon-dish",
        "el-icon-dish-1",
        "el-icon-food",
        "el-icon-chicken",
        "el-icon-fork-spoon",
        "el-icon-knife-fork",
        "el-icon-burger",
        "el-icon-tableware",
        "el-icon-sugar",
        "el-icon-dessert",
        "el-icon-ice-cream",
        "el-icon-hot-water",
        "el-icon-water-cup",
        "el-icon-coffee-cup",
        "el-icon-cold-drink",
        "el-icon-goblet",
        "el-icon-goblet-full",
        "el-icon-goblet-square",
        "el-icon-goblet-square-full",
        "el-icon-refrigerator",
        "el-icon-grape",
        "el-icon-watermelon",
        "el-icon-cherry",
        "el-icon-apple",
        "el-icon-pear",
        "el-icon-orange",
        "el-icon-coffee",
        "el-icon-ice-tea",
        "el-icon-ice-drink",
        "el-icon-milk-tea",
        "el-icon-potato-strips",
        "el-icon-lollipop",
        "el-icon-ice-cream-square",
        "el-icon-ice-cream-round"]
    }
  },
  created() {
    this.getMenuTree()
  },
  methods: {
    getMenuTree() {
      this.$axios.get("/sys/menu/list").then(res => {
        this.tableData = res.data
      })
    },
    submitForm(formName) {
      this.$refs[formName].validate((valid) => {


        if (valid) {
          if (this.editForm.type == 0) {
            this.editForm.path = '';
            this.editForm.component = '';
            this.editForm.parentId = 0
          }

          this.$axios.post('/sys/menu/save', this.editForm)
              .then(res => {

                this.$message.success('操作成功')
                this.getMenuTree()
                this.dialogVisible = false
              })
        } else {

          return false;
        }
      });
    },
    editHandle(row) {
      this.editForm = JSON.parse(JSON.stringify(row))
      this.dialogVisible = true
    },
    resetForm(formName) {
      this.$refs[formName].resetFields();
      this.dialogVisible = false
      this.editForm = {}
    },
    handleClose() {
      this.resetForm('editForm')
    },
    delHandle(id) {
      this.$axios.post("/sys/menu/delete/" + id).then(res => {
        this.$message({
          showClose: true,
          message: '恭喜你，操作成功',
          type: 'success',
          onClose: () => {
            this.getMenuTree()
          }
        });

      })
    }
  }
}
</script>

<style scoped>

</style>
