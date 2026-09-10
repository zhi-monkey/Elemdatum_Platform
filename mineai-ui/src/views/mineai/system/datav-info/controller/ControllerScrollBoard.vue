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

  const getData = async () => {
    const result = await maHttp.get(
      {
        url: 'controller/getControllerListByDataNum',
        params: { num: 50 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );

    // 转换成charts对应格式
    const data = Array.from(result, (k: any) => {
      let tmp: any[] = [];
      const ip = k?.ip || '未知';
      const name = k?.name || '未知';
      const lastModifiedTime = k?.lastModifiedTime || '未知';
      const status =
        k?.status !== -1
          ? '<span style="color:#7dfb72;" title="正常">正常</span>'
          : '<span style="color:#da3745;" title="故障">故障</span>';
      tmp.push(
        `<span title="${lastModifiedTime}">${lastModifiedTime}</span>`,
        `<span title="${name}">${name}</span>`,
        `<span title="${ip}">${ip}</span>`,
        status,
      );
      return tmp;
    });

    config.value = {
      header: ['更新时间', '设备名称', '设备ip', '状态'],
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
