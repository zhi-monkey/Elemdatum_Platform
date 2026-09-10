<template>
  <div class="flex py-1 px-1">
    <img src="../../../../../../assets/icons/titles.svg" alt="标识符" />
    <span style="font-size: medium">算法绑定总览</span>
  </div>
  <div class="md:flex">
    <template v-for="item in data" :key="item">
      <Card
        size="small"
        :loading="loading"
        class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
        :canExpan="false"
      >
        <div style="margin-bottom: 2px">
          <div class="py-0 flex justify-center text-1xl">总监控设备</div>
          <div class="py-2 flex justify-center" style="margin-bottom: 34px">
            <CountTo
              :startVal="0"
              :endVal="item.notBindNum + item.bindNum"
              class="text-3xl"
              color="#11B5AA"
              style="font-weight: bold"
            />
          </div>
          <a-row>
            <a-col :span="12">
              <div class="flex justify-center text-1xl"
                >未绑定
                <CountTo
                  :startVal="0"
                  :endVal="item.notBindNum"
                  class="text-1xl"
                  color="green"
                  style="margin-left: 10px; font-weight: bold"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span>绑定</span>
                <CountTo
                  :startVal="0"
                  :endVal="item.bindNum"
                  class="text-1xl"
                  color="orange"
                  style="margin-left: 10px; font-weight: bold"
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
  import { CountTo } from '/@/components/CountTo';
  import { Card, Col as ACol, Row as ARow } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const data = ref([{}]);
  const loading = ref(true);
  let timer;
  const getMonitorNum = async () => {
    const v = await maHttp.get(
      {
        url: 'monitor/getMonitorNumWhetherBindModel',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
    loading.value = false;
    data.value.pop();
    data.value.push(v);
  };

  //先执行一次，再开启定时器，定时更新data
  onMounted(async () => {
    await getMonitorNum();
    timer = setInterval(getMonitorNum, 10000);
  });

  onUnmounted(() => clearInterval(timer));
</script>
