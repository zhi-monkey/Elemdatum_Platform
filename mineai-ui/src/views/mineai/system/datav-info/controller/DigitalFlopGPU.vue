<template>
  <div id="digital-flop">
    <!--    <div class="digital-flop-item" v-for="item in digitalFlopData" :key="item.title">-->
    <!--      <div class="digital-flop-title">{{ item.title }}</div>-->
    <!--      <div class="digital-flop-content" style="width: 180px; height: 50px">-->

    <!--        &lt;!&ndash;        <digital-flop :config="item.number" />&ndash;&gt;-->
    <!--        &lt;!&ndash;        <div class="unit">{{ item.unit }}</div>&ndash;&gt;-->
    <!--      </div>-->
    <!--      -->
    <!--    </div>-->

    <div class="digital-flop-item">
      <div class="digital-flop-title"> 集群节点数 </div>
      <div class="digital-flop-content">
        <div class="digital-flop-number" style="color: #4d99fc">{{ controllerAllNum_show }}</div>
      </div>
    </div>

    <div class="digital-flop-item">
      <div class="digital-flop-title"> CPU占用情况 </div>
      <div class="digital-flop-content">
        <WaterLevelPond :config="cpuUsedConfig" class="digital-flop-water-level-pond" />
      </div>
    </div>

    <div class="digital-flop-item">
      <div class="digital-flop-title"> 内存占用情况 </div>
      <div class="digital-flop-content">
        <WaterLevelPond :config="memoryUsedConfig" class="digital-flop-water-level-pond" />
      </div>
    </div>

    <div class="digital-flop-item">
      <div class="digital-flop-title"> 硬盘占用情况 </div>
      <div class="digital-flop-content">
        <!--        <div ref="diskChart" class="chart-body"></div>-->
        <!--        <WaterLevelPond :config="config" class="digital-flop-water-level-pond" />-->
        <PercentPond :config="hardDiskUsedConfig" style="width: 200px; height: 100px" />
      </div>
    </div>

    <div class="digital-flop-item">
      <div class="digital-flop-title"> 硬盘总空间大小 </div>
      <div class="digital-flop-content">
        <div class="digital-flop-number">{{ hardDiskTotalSize }}GB</div>
      </div>
    </div>

    <div class="digital-flop-item">
      <div class="digital-flop-title"> 硬盘未使用空间 </div>
      <div class="digital-flop-content">
        <div class="digital-flop-number" style="color: #59d909">{{ hardDiskUnusedSize }}GB</div>
      </div>
    </div>

    <decoration10 />
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, reactive, ref, Ref } from 'vue';
  import { Decoration10, WaterLevelPond, PercentPond } from '@kjgl77/datav-vue3';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  interface Controller {
    controllerAllNum: number;
  }
  interface Hardware {
    disk: number;
  }
  interface Load {
    ramUsed: number;
    cpuUsed: number;
    diskUsed: number;
  }
  const controllerNum: Ref<Controller> = ref({ controllerAllNum: 0 });
  const hardwareNum: Ref<Hardware> = ref({ disk: 0 });
  const loadNum: Ref<Load> = ref({ ramUsed: 0, cpuUsed: 0, diskUsed: 0 });
  const diskChart = ref<HTMLDivElement | null>(null);
  let digitalFlopData: Ref<any[]> = ref([]);

  let controllerAllNum_show = ref(0); //算力控制器数量
  let cpuUsed_show = ref(0); //CPU使用率
  let memoryUsed_show = ref(0); //内存使用率
  let hardDiskUsed_show = ref(0); //硬盘使用率
  let hardDiskTotalSize = ref(0); //硬盘总大小
  let hardDiskUnusedSize = ref(0); //硬盘未使用大小

  function formatter(value: number) {
    if (value >= 10000) {
      return (value / 10000).toFixed(3) + 'w';
    } else return value;
  }

  const hardDiskUsedConfig = reactive({
    value: 70,
    lineDash: [10, 2],
    colors: ['#01c4f9', '#c135ff'],
  });

  const cpuUsedConfig = reactive({
    data: [cpuUsed_show.value],
    shape: 'roundRect',
    waveHeight: 10,
  });

  const memoryUsedConfig = reactive({
    data: [memoryUsed_show.value],
    shape: 'roundRect',
    waveHeight: 10,
  });

  // const { setOptions } = useECharts(diskChart as Ref<HTMLDivElement>);
  // setOptions({
  //   tooltip: { trigger: 'item', valueFormatter: (value) => value + ' 个' },
  //   color: ['#00baff', '#3de7c9', '#fff', '#ffc530', '#469f4b'],
  //   series: [
  //     {
  //       type: 'pie',
  //       radius: '70%',
  //       data: [
  //         { name: '已绑定算法', value: 12 },
  //         { name: '未绑定算法', value: 13 },
  //       ],
  //       emphasis: {
  //         itemStyle: {
  //           shadowBlur: 10,
  //           shadowOffsetX: 0,
  //           shadowColor: 'rgba(0, 0, 0, 0.5)',
  //         },
  //       },
  //       labelLine: {
  //         show: true,
  //       },
  //       label: {
  //         show: true,
  //         position: 'outer',
  //         alignTo: 'edge',
  //         margin: '8%',
  //         formatter: '{b}\n({d}%)',
  //         color: 'inherit',
  //         overflow: 'break',
  //       },
  //     },
  //   ],
  // });

  const getControllerNum = async () => {
    controllerNum.value = await maHttp.get(
      {
        url: 'controller/getControllerNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
    controllerAllNum_show.value = controllerNum.value.controllerAllNum;
  };

  const getHardwareNum = async () => {
    hardwareNum.value = await maHttp.get(
      {
        url: 'controller/getControllerHardware',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
    hardDiskTotalSize.value = Math.round(hardwareNum.value.disk / 1024);
  };

  const getLoadNum = async () => {
    loadNum.value = await maHttp.get(
      {
        url: 'controllerLoad/getControllerCurrentLoad',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
    // const clusterInfo: any = await maHttp.get(
    //   {
    //     url: 'controller/getClusterCpuInfo',
    //     headers: {
    //       // @ts-ignore
    //       ignoreCancelToken: true,
    //     },
    //   },
    //   { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    // );
    // let cpuTotal;
    // let cpuUsed;
    // if (clusterInfo?.results[1].metric_name === 'cluster_cpu_usage') {
    //   cpuTotal = clusterInfo?.results[0].data.result[0].value[1];
    //   cpuUsed = clusterInfo?.results[1].data.result[0].value[1];
    // } else {
    //   cpuTotal = clusterInfo?.results[1].data.result[0].value[1];
    //   cpuUsed = clusterInfo?.results[0].data.result[0].value[1];
    // }
    // loadNum.value.cpuUsed = (Number(cpuUsed as string) / Number(cpuTotal as string)) * 100;
    //将cpu，内存，硬盘占用率存储并显示到图表中
    cpuUsed_show.value = Number(loadNum.value.cpuUsed.toFixed(2));
    cpuUsedConfig.data = [cpuUsed_show.value];
    memoryUsed_show.value = Number(loadNum.value.ramUsed.toFixed(2));
    memoryUsedConfig.data = [memoryUsed_show.value];
    hardDiskUsed_show.value = Number(loadNum.value.diskUsed.toFixed(2));
    hardDiskUsedConfig.value = hardDiskUsed_show.value;
  };

  function createData() {
    digitalFlopData.value = [
      {
        title: '算力控制器',
        number: {
          number: [controllerNum.value.controllerAllNum],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#4d99fc',
            fontWeight: 'bold',
          },
        },
      },
      {
        title: '硬盘总大小',
        number: {
          number: [hardwareNum.value.disk / 1024],
          content: '{nt}GB',
          textAlign: 'center',
          style: {
            fill: '#094bd9',
            fontWeight: 'bold',
          },
        },
      },
      {
        title: '硬盘未使用大小',
        number: {
          number: [(hardwareNum.value.disk * (1 - loadNum.value.diskUsed / 100)) / 1024],
          content: '{nt}GB',
          textAlign: 'center',
          style: {
            fill: '#59d909',
            fontWeight: 'bold',
          },
        },
      },
      {
        title: 'CPU占用情况',
        number: {
          number: [Number(loadNum.value.cpuUsed.toFixed(2))],
          content: '{nt}%',
          textAlign: 'center',
          style: {
            fill: '#4d99fc',
            fontWeight: 'bold',
          },
        },
      },
      {
        title: '硬盘占用情况',
        number: {
          number: [Number(loadNum.value.diskUsed.toFixed(2))],
          content: '{nt}%',
          textAlign: 'center',
          style: {
            fill: '#f46827',
            fontWeight: 'bold',
          },
        },
      },
      {
        title: '内存占用情况',
        number: {
          number: [Number(loadNum.value.ramUsed.toFixed(2))],
          content: '{nt}%',
          textAlign: 'center',
          style: {
            fill: '#40faee',
            fontWeight: 'bold',
          },
        },
      },
    ];
  }

  let timer: NodeJS.Timer;

  onMounted(async () => {
    await Promise.all([getControllerNum(), getHardwareNum(), getLoadNum()]);
    await createData();
    hardDiskUnusedSize.value = Math.round(
      hardDiskTotalSize.value * (1 - hardDiskUsed_show.value / 100),
    ); //计算硬盘未使用空间大小（因为使用率和总空间是分开获取的，所以必须要等两个值获取完了再计算）
    timer = setInterval(async () => {
      await Promise.all([getControllerNum(), getHardwareNum(), getLoadNum()]);
      await createData();
      hardDiskUnusedSize.value = Math.round(
        hardDiskTotalSize.value * (1 - hardDiskUsed_show.value / 100),
      ); //计算硬盘未使用空间大小
    }, 10 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style lang="less">
  #digital-flop {
    position: relative;
    height: 250px !important;
    flex-shrink: 0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: rgba(6, 30, 93, 0.5);

    .dv-decoration-10 {
      position: absolute;
      width: 95%;
      left: 2.5%;
      height: 5px;
      bottom: 0;
    }

    .digital-flop-item {
      width: 20% !important;
      height: 80%;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      border-left: 3px solid rgb(6, 30, 93);
      border-right: 3px solid rgb(6, 30, 93);
    }

    .digital-flop-title {
      font-size: 20px;
      margin-bottom: 20px;
    }

    .digital-flop-content {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 300px;

      .digital-flop-number {
        color: #094bd9;
        font-weight: bold;
        text-align: center;
        font-size: 40px;
      }
    }

    .unit {
      margin-left: 10px;
      display: flex;
      align-items: flex-end;
      box-sizing: border-box;
      padding-bottom: 13px;
    }

    .digital-flop-water-level-pond {
      width: 150px;
      height: 150px;
    }
  }

  #flop-wrapper {
    padding-bottom: 10px;
  }
</style>
