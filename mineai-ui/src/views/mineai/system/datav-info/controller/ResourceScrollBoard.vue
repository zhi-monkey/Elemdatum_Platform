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

  const createSpan = (load: number): string => {
    load = parseFloat(load.toFixed(2));
    if (load >= 0 && load <= 30) {
      return `<span style="color:#7dfb72;">${load}%</span>`;
    } else if (load > 30 && load <= 70) {
      return `<span style="color:#37a2da;">${load}%</span>`;
    } else if (load > 70 && load <= 100) {
      return `<span style="color:#da3745;">${load}%</span>`;
    } else {
      return '未知';
    }
  };

  const getContent = (str: string): string => {
    if (str.includes('span')) {
      let start = str.indexOf('>');
      let end = str.lastIndexOf('<');
      return str.substring(start + 1, end);
    } else {
      return str;
    }
  };

  const getData = async () => {
    const result = await maHttp.get(
      {
        url: 'controllerLoad/getControllerLoadListByDataNum',
        params: { num: 50 },
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );

    const result2 = await maHttp.get(
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
    const data = Array.from(result, (k: any, index: number) => {
      let tmp: any[] = [];
      const ip = k.controller?.ip || '未知';
      const cpuLoad = createSpan(k.cpuLoad);
      const ramLoad = createSpan(((k.ramLoad / k.controller.ramSize) as any).toFixed(2) * 100);
      const diskLoad = createSpan(((k.diskLoad / k.controller.diskSize) as any).toFixed(2) * 100);
      const name = result2[index]?.name || '未知';
      const lastModifiedTime = result2[index]?.lastModifiedTime || '未知';
      const status =
        result2[index]?.status !== -1
          ? '<span style="color:#7dfb72;" title="正常">正常</span>'
          : '<span style="color:#da3745;" title="故障">故障</span>';
      tmp.push(
        `<span title="${name}">${name}</span>`,
        `<span title="${ip}">${ip}</span>`,
        `<span title="${getContent(cpuLoad)}">${cpuLoad}</span>`,
        `<span title="${getContent(ramLoad)}">${ramLoad}</span>`,
        `<span title="${getContent(diskLoad)}">${diskLoad}</span>`,
        `<span title="${lastModifiedTime}">${lastModifiedTime}</span>`,
        status,
      );
      return tmp;
    });

    config.value = {
      header: ['设备名称', '设备ip', 'CPU占用率', 'RAM占用率', '硬盘占用率', '更新时间', '状态'],
      data: data,
      index: true,
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

<style lang="less" scoped>
  #scroll-board {
    width: 100%;
    box-sizing: border-box;
    height: 100%;
    overflow: hidden;
    padding-right: 20px;
    padding-left: 20px;
  }

  .pointer {
    cursor: pointer;
  }
</style>
