<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../../assets/icons/titles.svg" /><span style="font-size: medium"
      >关联设备总览</span
    >
  </div>
  <div class="md:flex">
    <template v-for="item in data" :key="item">
      <Card
        size="small"
        :loading="loading"
        class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
        :canExpan="false"
      >
        <div>
          <div class="py-0 flex justify-center text-1xl">关联设备总数</div>
          <div class="py-2 flex justify-center" style="margin-bottom: 8px">
            <CountTo
              :startVal="0"
              :endVal="item.total"
              class="text-3xl"
              color="#7CFFB2"
              style="font-weight: bold"
            />
          </div>
          <a-row style="margin-bottom: 30px">
            <a-col :span="12" style="margin-bottom: 10px">
              <div class="flex justify-center text-1xl"
                >启动
                <CountTo
                  :startVal="0"
                  :endVal="item.on"
                  class="text-1xl"
                  color="#B5C0E5"
                  style="margin-left: 10px; font-size: large; font-weight: bold"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span>关闭</span>
                <CountTo
                  :startVal="0"
                  :endVal="item.off"
                  class="text-1xl"
                  color="orange"
                  style="margin-left: 10px; font-size: large; font-weight: bold"
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

  const getData = async () => {
    const v = await maHttp.get(
      {
        url: 'monitorDataset/getMonitorState',
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
    await getData();
    timer = setInterval(getData, 10000);
  });
  onUnmounted(() => clearInterval(timer));
</script>
