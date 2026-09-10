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
  import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  let digitalFlopData: Ref<any[]> = ref([]);

  function formatter(value: number) {
    if (value >= 10000) {
      return (value / 10000).toFixed(3) + 'w';
    } else return value;
  }

  async function getData() {
    const info = await Promise.all([
      maHttp.get(
        {
          url: 'datasets/getPeriodData',
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      ),
      maHttp.get(
        {
          url: 'datasets/versions/countByFileAnnotate',
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      ),
    ]);

    digitalFlopData.value = [
      {
        title: '数据集总数',
        number: {
          number: [info[0].total],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#4d99fc',
            fontWeight: 'bold',
          },
        },
        unit: '个',
      },
      {
        title: '本日新增数据集',
        number: {
          number: [info[0].today],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#f46827',
            fontWeight: 'bold',
          },
        },
        unit: '个',
      },
      {
        title: '本周新增数据集',
        number: {
          number: [info[0].thisWeek],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#40faee',
            fontWeight: 'bold',
          },
        },
        unit: '个',
      },
      {
        title: '本月新增数据集',
        number: {
          number: [info[0].thisMonth],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#f434da',
            fontWeight: 'bold',
          },
        },
        unit: '个',
      },
      {
        title: '图片总数',
        number: {
          number: [info[1].allFiles],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#6b27f4',
            fontWeight: 'bold',
          },
        },
        unit: '个',
      },
      {
        title: '未标注',
        number: {
          number: [info[1].unannotatedFiles],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#f4c427',
            fontWeight: 'bold',
          },
        },
        unit: '个',
      },
      {
        title: '已标注',
        number: {
          number: [info[1].annotatedFiles],
          content: '{nt}个',
          formatter,
          textAlign: 'center',
          style: {
            fill: '#27f49f',
            fontWeight: 'bold',
          },
        },
        unit: '个',
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
<style lang="less" scoped>
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
