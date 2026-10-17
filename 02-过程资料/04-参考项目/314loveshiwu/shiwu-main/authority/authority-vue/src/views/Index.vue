<template>

  <div id="bg">

    <el-descriptions title="系统简介" direction="vertical" :column="1" border>
      <el-descriptions-item label="平台功能" >
        <el-tag size="small">这是一个校园失物管理平台，它可以帮助你快速寻找丢失的物品，你也可以把自己拾取到的物品发布到该平台上便于失
          主寻找。
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="平台宗旨" >
        <el-tag size="small">让互相帮助成为我们的日常。
        </el-tag>
      </el-descriptions-item>

    </el-descriptions>



   <el-col>
     <el-col :span="17">
       <div style="width:100%;height: 300px" id="firstechaerts"></div>
     </el-col>
     <el-col :span="7">
       <div style="width: 320px;height: 320px;" id="secondecharts"></div>
     </el-col>
   </el-col>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import request from "@/utils/axios";



export default {
  name: "Index",
  data() {
    return {
      value: new Date(),
      ydata:[],
      xdata:[],
      occupy:0
    }
  },
  created() {

  },
  mounted() {
    request.get('/echarts/systemparameter').then(res=>{
      if(res.code=='200'){
      this.ydata=res.data.yAxis
      this.xdata=res.data.series
      this.queryData()
      }
    })
    this.queryinternalStorage()
    request.get('/echarts/queryinternalStorage').then(res=>{
      if(res.code==200){
        this.occupy=res.data
        this.queryinternalStorage()
      }
    })


  },
  methods: {
    queryData() {

      let myChart = echarts.init(document.getElementById('firstechaerts'));
      var option = {
        title: {
          text: '系统参数'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {},
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          boundaryGap: [0, 0.1]
        },
        yAxis: {
          type: 'category',
          data: this.ydata
        },
        series: [
          {
            name: '数量',
            type: 'bar',
            data: this.xdata
          },

        ]
      };

      option && myChart.setOption(option);
    },
  //  获取仪表盘数据
    queryinternalStorage(){
      var chartDom = document.getElementById('secondecharts');
      var myChart = echarts.init(chartDom);
      var option;

      option = {
        series: [
          {
            type: 'gauge',
            axisLine: {
              lineStyle: {
                width: 30,
                color: [
                  [0.3, '#67e0e3'],
                  [0.7, '#37a2da'],
                  [1, '#fd666d']
                ]
              }
            },
            pointer: {
              itemStyle: {
                color: 'inherit'
              }
            },
            axisTick: {
              distance: -30,
              length: 8,
              lineStyle: {
                color: '#fff',
                width: 2
              }
            },
            splitLine: {
              distance: -30,
              length: 30,
              lineStyle: {
                color: '#fff',
                width: 4
              }
            },
            axisLabel: {
              color: 'inherit',
              distance: 40,
              fontSize: 20
            },
            detail: {
              valueAnimation: true,
              formatter: ( this.occupy*100).toFixed(1)+'%',
              color: 'inherit'
            },
            data: [
              {
                value:( this.occupy*100).toFixed(1)
              }
            ]
          }
        ]
      };

      option && myChart.setOption(option);
    }


  }

}
</script>

<style scoped>

</style>
