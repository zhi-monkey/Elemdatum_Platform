<template>
  <div id="digital-flop">
    <div class="digital-flop-item" v-for="item in digitalFlopData" :key="item.title">
      <div class="digital-flop-title">{{ item.title }}</div>
      <div class="digital-flop-content" style="width: 180px; height: 50px">
        <digital-flop :config="item.number" />
        <!--        <div class="unit">{{ item.unit }}</div>-->
      </div>
    </div>

    <decoration10 />
  </div>
</template>
<script lang="ts" setup>
  import { onMounted, onUnmounted, ref, Ref } from 'vue';
  import { Decoration10, DigitalFlop } from '@kjgl77/datav-vue3';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  let digitalFlopData: Ref<any[]> = ref([]);

  function formatter(value: number) {
    if (value >= 10000) {
      return (value / 10000).toFixed(3) + 'w';
    } else return value;
  }

  async function getData() {
    const monitorInfo = await maHttp.get(
      {
        url: 'monitor/getMonitorNumWhetherBindModel',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
    const modelAlertInfo = await maHttp.get(
      {
        url: 'modelAlert/getModelAlertStatusNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    digitalFlopData.value = [
      {
        title: '监控设备总数',
        number: {
          number: [monitorInfo.bindNum + monitorInfo.notBindNum],
          content: '{nt}台',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#4d99fc',
            fontWeight: 'bold',
          },
        },
        unit: '台',
      },
      {
        title: '未绑定算法设备',
        number: {
          number: [monitorInfo.notBindNum],
          content: '{nt}台',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#f46827',
            fontWeight: 'bold',
          },
        },
        unit: '台',
      },
      {
        title: '已绑定算法设备',
        number: {
          number: [monitorInfo.bindNum],
          content: '{nt}台',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#40faee',
            fontWeight: 'bold',
          },
        },
        unit: '台',
      },
      {
        title: '报警信息总数',
        number: {
          number: [modelAlertInfo.sumNum],
          content: '{nt}条',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#4d99fc',
            fontWeight: 'bold',
          },
        },
        unit: '条',
      },
      {
        title: '未处理报警',
        number: {
          number: [modelAlertInfo.unHandledNum],
          content: '{nt}条',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#f46827',
            fontWeight: 'bold',
          },
        },
        unit: '条',
      },
      {
        title: '已处理报警',
        number: {
          number: [modelAlertInfo.handledNum],
          content: '{nt}条',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#40faee',
            fontWeight: 'bold',
          },
        },
        unit: '条',
      },
      {
        title: '已忽略报警',
        number: {
          number: [modelAlertInfo.ignoredNum],
          content: '{nt}条',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#40faac',
            fontWeight: 'bold',
          },
        },
        unit: '条',
      },
    ];
  }

  let timer: NodeJS.Timer;

  onMounted(async () => {
    await getData();
    timer = setInterval(async () => {
      await getData();
    }, 60 * 1000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>

<style lang="less">
  #digital-flop {
    position: relative;
    height: 200px;
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
      width: 11%;
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
    }

    .unit {
      margin-left: 10px;
      display: flex;
      align-items: flex-end;
      box-sizing: border-box;
      padding-bottom: 13px;
    }
  }

  #flop-wrapper {
    padding-bottom: 10px;
  }
</style>
