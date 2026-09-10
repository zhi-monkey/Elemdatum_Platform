<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../assets/icons/titles.svg" alt="标识符" /><span
      style="font-size: medium"
      >服务器实时负载总览</span
    >
  </div>
  <div class="md:flex">
    <template v-for="item in data" :key="item">
      <Card
        size="small"
        :loading="loading"
        class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
        :can-expan="false"
      >
        <div>
          <a-row :gutter="8" style="margin-top: 15px; margin-bottom: 21px">
            <a-col :span="8">
              <div class="flex justify-center text-1xl">
                <span>CPU占用率</span>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="flex justify-center text-1xl">
                <span>硬盘占用率</span>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="flex justify-center text-1xl">
                <span>RAM占用率</span>
              </div>
            </a-col>
          </a-row>
          <a-row :gutter="8" style="margin-top: 10px">
            <a-col :span="8">
              <div class="flex justify-center text-1xl">
                <a-progress
                  :width="80"
                  type="circle"
                  :stroke-color="{
                    '0%': '#108ee9',
                    '100%': '#87d068',
                  }"
                  :percent="item.cpuUsed.toFixed(2) * 1"
                />
              </div>
            </a-col>
            <a-col :span="8">
              <div class="flex justify-center text-1xl">
                <a-progress
                  :width="80"
                  type="circle"
                  :stroke-color="{
                    '0%': '#108ee9',
                    '100%': '#87d068',
                  }"
                  :percent="item.diskUsed.toFixed(2) * 1"
                />
              </div>
            </a-col>
            <a-col :span="8">
              <div class="flex justify-center text-1xl">
                <a-progress
                  :width="80"
                  type="circle"
                  :stroke-color="{
                    '0%': '#108ee9',
                    '100%': '#87d068',
                  }"
                  :percent="item.ramUsed.toFixed(2) * 1"
                />
              </div>
            </a-col>
          </a-row>
        </div>
      </Card>
    </template>
  </div>
</template>
<script lang="ts" setup>
  import { Card, Col as ACol, Progress as AProgress, Row as ARow } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const data = ref([{}]);
  const loading = ref(true);
  let dataTimer;
  const getData = async () => {
    const v = await maHttp.get(
      {
        url: 'controllerLoad/getControllerCurrentLoad',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
    );
    data.value.pop();
    data.value.push(v);
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
