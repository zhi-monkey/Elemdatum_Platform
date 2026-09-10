<template>
  <div class="flex flex-row" style="margin-bottom: 8px; margin-top: 8px">
    <img src="../../../../../assets/icons/titles.svg" alt="标识符" />
    <span style="opacity: 0.8; font-size: medium">算力控制器实时负载总览</span>
  </div>
  <div class="md:flex">
    <template v-for="item in data" :key="item">
      <Card size="small" :loading="loading" class="w-full" :canExpan="false" style="height: 268px">
        <div style="padding-top: 35px">
          <a-row :gutter="12">
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <a-progress
                  type="circle"
                  :stroke-color="{
                    '0%': '#108ee9',
                    '100%': '#87d068',
                  }"
                  :percent="item.cpuUsed.toFixed(2) * 1"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <a-progress
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
          <a-row :gutter="12" style="margin-top: 45px; margin-bottom: 25px">
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span>CPU利用率</span>
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span>RAM利用率</span>
              </div>
            </a-col>
          </a-row>
        </div>
      </Card>
    </template>
  </div>
</template>
<script lang="ts" setup>
  import { Card } from 'ant-design-vue';
  import { Row as ARow, Col as ACol, Progress as AProgress } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  let data = ref([{}]);
  let dataTimer;

  const loading = ref(true);

  const getData = async () => {
    await maHttp
      .get(
        {
          url: 'controllerLoad/getControllerCurrentLoad',
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.CONTROLLER_MANAGER },
      )
      .then((v) => {
        data.value.pop();
        data.value.push(v);
      });
  };

  onMounted(async () => {
    await getData();
    loading.value = false;
    dataTimer = setInterval(getData, 10000);
  });
  onUnmounted(() => {
    clearInterval(dataTimer);
  });
  defineProps({
    loading: {
      type: Boolean,
    },
  });
</script>
