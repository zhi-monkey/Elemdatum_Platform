<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../assets/icons/titles.svg" alt="标识符" /><span
      style="font-size: medium"
      >控制器硬件指标总览</span
    >
  </div>
  <div class="md:flex">
    <Card
      size="small"
      :loading="loading"
      class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
      :can-expan="false"
    >
      <div class="flex flex-row justify-around">
        <div class="flex flex-col gap-y-3 justify-center">
          <Icon icon="flat-color-icons:electronics" size="90" class="pt-2" />
          <span style="text-align: center"
            >CPU核
            <CountTo
              :startVal="0"
              :endVal="data.cpu"
              class="text-1xl"
              color="#ff9828"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />个
          </span>
        </div>
        <div class="flex flex-col gap-y-3 justify-center">
          <Icon icon="flat-color-icons:sim-card" size="90" class="pt-2" />
          <span v-if="data.ram >= 524288" style="text-align: center"
            >内存
            <CountTo
              :startVal="0"
              :endVal="data.ram / 1024 / 1024"
              class="text-1xl"
              color="#009688"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />TB
          </span>
          <span v-else-if="data.ram >= 512" style="text-align: center"
            >内存
            <CountTo
              :startVal="0"
              :endVal="data.ram / 1024"
              class="text-1xl"
              color="#009688"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />GB
          </span>
          <span v-else style="text-align: center"
            >内存
            <CountTo
              :startVal="0"
              :endVal="data.ram"
              class="text-1xl"
              color="#009688"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />MB
          </span>
        </div>
        <div class="flex flex-col gap-y-3 justify-center">
          <Icon icon="flat-color-icons:filing-cabinet" size="90" class="pt-2" />
          <span v-if="data.disk >= 524288" style="text-align: center"
            >硬盘
            <CountTo
              :startVal="0"
              :endVal="data.disk / 1024 / 1024"
              class="text-1xl"
              color="#607d8a"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />TB
          </span>
          <span v-else-if="data.disk >= 512" style="text-align: center"
            >硬盘
            <CountTo
              :startVal="0"
              :endVal="data.disk / 1024"
              class="text-1xl"
              color="#607d8a"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />GB
          </span>
          <span v-else style="text-align: center"
            >硬盘
            <CountTo
              :startVal="0"
              :endVal="data.disk"
              class="text-1xl"
              color="#607d8a"
              style="margin-left: 1px; margin-right: 2px; font-weight: bold"
            />MB
          </span>
        </div>
      </div>
    </Card>
  </div>
</template>
<script lang="ts" setup>
  import { Icon } from '/@/components/Icon';
  import { CountTo } from '/@/components/CountTo';
  import { Card } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const data = ref({});
  const loading = ref(true);
  let dataTimer;

  const getData = async () => {
    const v = await maHttp.get(
      {
        url: 'controller/getControllerHardware',
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
    await getData();
    loading.value = false;
    dataTimer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    //删除数据请求定时器
    clearInterval(dataTimer);
  });
</script>
