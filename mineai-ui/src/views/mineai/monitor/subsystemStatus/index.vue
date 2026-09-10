<template>
  <div class="w-full h-full flex flex-col myPage">
    <div class="w-full pl-1 pr-1 mb-2">
      <Card title="子系统总览" style="border-radius: 6px">
        <div class="flex flex-row justify-around">
          <Card style="border-radius: 6px" class="h-full w-1/5">
            <div class="h-full w-full flex flex-row">
              <Icon icon="flat-color-icons:electronics" size="72" />
              <CountTo
                color="#ff9800"
                :startVal="0"
                :endVal="total"
                :duration="1500"
                style="font-size: 48px"
              />
              <span style="font-size: 20px; align-self: end; padding-bottom: 12px; margin-left: 8px"
                >子系统总数</span
              >
            </div>
          </Card>
          <Card style="border-radius: 6px" class="h-full w-1/5">
            <div class="h-full w-full flex flex-row">
              <Icon icon="flat-color-icons:globe" size="72" />
              <CountTo
                color="#7cb342"
                :startVal="0"
                :endVal="online"
                :duration="1500"
                style="font-size: 48px"
              />
              <span style="font-size: 20px; align-self: end; padding-bottom: 12px; margin-left: 8px"
                >子系统在线</span
              >
            </div>
          </Card>
          <Card style="border-radius: 6px" class="h-full w-1/5">
            <div class="h-full w-full flex flex-row">
              <Icon icon="flat-color-icons:high-priority" size="72" />
              <CountTo
                color="#7cb342"
                :startVal="0"
                :endVal="offline"
                :duration="1500"
                style="font-size: 48px"
              />
              <span style="font-size: 20px; align-self: end; padding-bottom: 12px; margin-left: 8px"
                >子系统离线</span
              >
            </div>
          </Card>
        </div>
      </Card>
    </div>
    <div class="w-full pl-1 pr-1">
      <BasicTable
        title="子系统列表"
        :columns="columns"
        :dataSource="data"
        :loading="loading"
        bordered
        :showIndexColumn="false"
        style="border-radius: 6px !important"
      />
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { Icon } from '/@/components/Icon';
  import { CountTo } from '/@/components/CountTo';
  import { Card } from 'ant-design-vue';
  import { BasicTable } from '/@/components/Table';
  import { columns } from './table';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  let timer;
  let data = ref();
  let total = ref(0);
  let online = ref(0);
  let offline = ref(0);
  let loading = ref(true);

  const getData = async () => {
    const v = await maHttp.get(
      {
        url: 'subSystem/getSubsystemsStatus',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );
    data.value = v;
    total.value = v.length;
    let temp_on = 0;
    let temp_off = 0;
    v.forEach((item) => {
      if (item.status === 0) {
        temp_on++;
      } else {
        temp_off++;
      }
    });
    online.value = temp_on;
    offline.value = temp_off;
  };

  onMounted(async () => {
    await getData();
    loading.value = false;
    timer = setInterval(getData, 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
<style scoped>
  .myPage >>> .ant-card-head {
    min-height: 30px;
    display: flex;
  }
  .myPage >>> .ant-card-head-title {
    padding: 8px 0;
  }
</style>
