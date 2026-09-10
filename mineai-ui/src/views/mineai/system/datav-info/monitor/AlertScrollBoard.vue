<template>
  <div id="scroll-board">
    <scroll-board :config="config" class="pointer" />
  </div>
</template>

<script lang="ts" setup>
  import { ScrollBoard } from '@kjgl77/datav-vue3';
  import { ref, Ref, onMounted, onUnmounted } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const config: Ref<object> = ref([]);

  const getData = async function () {
    const result = await maHttp.get(
      {
        url: 'modelAlert/getModelAlertListByDataNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );

    // 转换成charts对应格式
    const data = Array.from(result, (k: any) => {
      let tmp: any[] = [];
      tmp.push(
        `<span title="${k.createTime}">${k.createTime}</span>`,
        `<span title="${k.description != '' ? k.description : '无'}">${
          k.description != '' ? k.description : '无'
        }</span>`,
        `<span title="${
          k.subsystem !== 'CENTRAL_PLATFORM' ? k.monitorName : k.monitor?.monitorName
        }">${k.subsystem !== 'CENTRAL_PLATFORM' ? k.monitorName : k.monitor?.monitorName}</span>`,
        `<span title="${k.subsystem !== 'CENTRAL_PLATFORM' ? k.modelName : k.model.modelName}">${
          k.subsystem !== 'CENTRAL_PLATFORM' ? k.modelName : k.model.modelName
        }</span>`,
      );
      return tmp;
    });

    config.value = {
      header: ['上报时间', '报警内容', '监控设备', '算法'],
      data,
      index: true,
      columnWidth: [50, 170, 300],
      align: ['center'],
      rowNum: 7,
      headerBGC: '#1981f6',
      headerHeight: 45,
      oddRowBGC: 'rgba(0, 44, 81, 0.8)',
      evenRowBGC: 'rgba(10, 29, 50, 0.8)',
    };
  };

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
  #scroll-board {
    width: 50%;
    box-sizing: border-box;
    height: 100%;
    overflow: hidden;
    padding-right: 20px;
  }

  .pointer {
    cursor: pointer;
  }
</style>
