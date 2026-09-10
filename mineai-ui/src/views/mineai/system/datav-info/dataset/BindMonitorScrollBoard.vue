<template>
  <div id="scroll-board">
    <scroll-board :config="config" class="pointer" />
  </div>
</template>

<script lang="ts" setup>
  import { ScrollBoard } from '@kjgl77/datav-vue3';
  import { onMounted, onUnmounted, ref, Ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const config: Ref<object> = ref([]);

  const getData = async function () {
    const result = await maHttp.get(
      {
        url: 'monitorDataset/findMonitorsLinkedDatasetByNum',
        params: { num: 10 },
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
      tmp.push(
        `<span >${k.monitorName}</span>`,
        `<span >${k.dataType}</span>`,
        `<span >${k.scene?.name || '无'}</span>`,
        `<span >${k.isRecord === 1 ? '正在录制' : '停止录制'}</span>`,
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
