<template>
  <div class="flex py-1 px-1">
    <img src="../../../../../../assets/icons/titles.svg" alt="标识符" />
    <span style="font-size: medium">算力控制器总览</span>
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
          :endVal="data.controllerAllNum"
        />
      </div>
      <div class="flex space-x-4 justify-center flex-wrap pt-2 pb-3">
        <div class="flex space-x-1">
          <span style="font-size: medium; opacity: 0.8">运行</span>
          <CountTo
            style="color: #24ffb8; font-size: large; font-weight: bold"
            :startVal="0"
            :endVal="data.workingNum"
          />
        </div>

        <div class="flex space-x-1">
          <span style="font-size: medium; opacity: 0.8">故障</span>
          <CountTo
            style="color: #a421fa; font-size: large; font-weight: bold"
            :startVal="0"
            :endVal="data.errorNum"
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

  const getControllerNum = async () => {
    const v = await maHttp.get(
      {
        url: 'controller/getControllerNum',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
    data.value = v;
  };

  //先执行一次，再开启定时器，定时更新data
  onMounted(async () => {
    await getControllerNum();
    loading.value = false;
    dataTimer = setInterval(getControllerNum, 10000);
  });

  onUnmounted(() => {
    //删除数据请求定时器
    clearInterval(dataTimer);
  });
  const name = '算力控制器';
</script>
