<template>
  <div id="scroll-board">
    <scroll-board :config="config" class="pointer" />
  </div>
</template>

<script lang="ts" setup>
  import { ScrollBoard } from '@kjgl77/datav-vue3';
  import { onMounted, onUnmounted, ref, Ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { formatDateTime } from '/@/utils';

  const config: Ref<object> = ref([]);

  const getData = async function () {
    const result = await maHttp
      .get(
        {
          url: 'datasets',
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      )
      .then((v) => {
        if (v.result.length != 0) return v.result;
        else return [];
      });

    // 转换成charts对应格式
    const data = Array.from(result, (k: any) => {
      let tmp: any[] = [];
      tmp.push(
        `<span title="${k.id}">${k.id}</span>`,
        `<span title="${k.name}">${k.name}</span>`,
        `<span title="${k.annotateType}">${k.annotateType}</span>`,
        `<span title="${formatDateTime(k.createTime)}">${formatDateTime(k.createTime)}</span>`,
      );
      return tmp;
    });

    config.value = {
      header: ['ID', '名称', '标注类型', '创建时间'],
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
