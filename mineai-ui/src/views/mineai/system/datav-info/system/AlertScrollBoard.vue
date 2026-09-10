<!--报警信息-->
<template>
  <div id="scroll-board">
    <scroll-board :config="config" class="pointer" />
  </div>
</template>

<script lang="ts" setup>
  import { ScrollBoard } from '@kjgl77/datav-vue3';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const config = ref({});

  // 获取报警信息条数
  const getData = async () => {
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
      const modelName = k.model?.modelName || '无';
      const monitorName = k.monitor?.name || '无';
      tmp.push(
        `<span title="${k.createTime}">${k.createTime}</span>`,
        `<span title="${k.description != '' ? k.description : '无'}">${
          k.description != '' ? k.description : '无'
        }</span>`,
        `<span title="${modelName}">${modelName}</span>`,
        `<span title="${monitorName}">${monitorName}</span>`,
      );
      return tmp;
    });

    config.value = {
      header: ['时间', '报警信息', '算法', '描述'],
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
