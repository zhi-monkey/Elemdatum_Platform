<template>
  <div class="flex py-1 px-1">
    <img src="../../../../../../assets/icons/titles.svg" alt="标识符" />
    <span style="font-size: medium">监控设备总览</span>
  </div>
  <div class="md:flex">
    <Card
      size="small"
      :loading="loading"
      class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
      :canExpan="false"
      style="height: calc(100% - 20px)"
    >
      <div class="pt-5 flex justify-center text-1xl">
        <span>{{ name }}</span>
      </div>
      <div class="pt-2 flex justify-center">
        <CountTo
          style="color: #218cfc; font-weight: bold"
          :startVal="0"
          class="text-3xl"
          :endVal="data.monitorNum"
        />
      </div>
      <div class="flex space-x-4 justify-center flex-wrap pt-2 pb-3">
        <div class="flex space-x-1">
          <span style="font-size: medium; opacity: 0.8">启用</span>
          <CountTo
            style="color: #24ffb8; font-size: large; font-weight: bold"
            :startVal="0"
            :endVal="data.monitorOnNum"
          />
        </div>
        <div class="flex space-x-1">
          <span style="font-size: medium; opacity: 0.8">停用</span>
          <CountTo
            style="color: #fc1d7b; font-size: large; font-weight: bold"
            :startVal="0"
            :endVal="data.monitorOffNum"
          />
        </div>
      </div>
    </Card>
  </div>
</template>
<script lang="ts" setup>
  import { CountTo } from '/@/components/CountTo';
  import { Card } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { onMounted, onUnmounted, ref } from 'vue';

  let data = ref({});
  let loading = ref(true);
  let dataTimer;

  const getMonitorNum = async () => {
    const v = await maHttp.get(
      {
        url: 'monitor/getMonitorNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
    data.value = v;
  };

  //先执行一次，再开启定时器，定时更新data
  onMounted(async () => {
    await getMonitorNum();
    loading.value = false;
    dataTimer = setInterval(getMonitorNum, 10000);
  });

  onUnmounted(() => {
    //删除数据请求定时器
    clearInterval(dataTimer);
  });

  const name = '监控设备';
</script>
