<!--监控设备-->
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

  let config = ref({});

  const getData = async () => {
    const result = await maHttp.get(
      {
        url: 'monitor/getMonitorListByDataNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
    // 转换成charts对应格式
    const data = Array.from(result, (k: any) => {
      let tmp: any[] = [];
      const dataType = k?.dataType || '未知';
      const scene = k.scene?.name || '无';
      const monitorName = k?.monitorName || '无';
      const streamStatus = k.isPushStream === 1 ? '是' : '否';
      tmp.push(
        `<span title="${monitorName}">${monitorName}</span>`,
        `<span title="${dataType}">${dataType}</span>`,
        `<span title="${scene}">${scene}</span>`,
        `<span title="${streamStatus}">${streamStatus}</span>`,
      );
      return tmp;
    });

    config.value = {
      header: ['设备名称', '监控数据类型', '设备场景', '推流'],
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
