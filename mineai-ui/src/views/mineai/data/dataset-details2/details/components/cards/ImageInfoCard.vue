<template>
  <div class="flex flex-row">
    <div class="flex py-1">
      <span style="font-size: medium">数据集图片信息</span>
    </div>
  </div>

  <Card :loading="loading">
    <div style="width: 100%; height: 250px">
      <div class="pt-10 pb-3 flex justify-center text-2xl">
        <span>{{ name }}</span>
      </div>
      <div class="pt-2 flex justify-center">
        <CountTo
          style="color: #24ffb8; font-weight: bold"
          :startVal="0"
          class="text-4xl"
          :endVal="data.totalImages"
        />
      </div>
      <div class="flex space-x-14 justify-center flex-wrap pt-10 pb-3">
        <div class="flex space-x-2">
          <span style="font-size: medium; opacity: 1">已标记</span>
          <CountTo
            style="color: #218cfc; font-size: large; font-weight: bold"
            :startVal="0"
            :endVal="data.labeledImages"
          />
        </div>

        <div class="flex space-x-2">
          <span style="font-size: medium; opacity: 1">未标记</span>
          <CountTo
            style="color: #9aa297; font-size: large; font-weight: bold"
            :startVal="0"
            :endVal="data.unlabeledImages"
          />
        </div>
      </div>
    </div>
  </Card>
</template>
<script lang="ts" setup>
  import { CountTo } from '/@/components/CountTo';
  import { Card } from 'ant-design-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { onMounted, onUnmounted, ref } from 'vue';
  import Cookies from 'js-cookie';

  let data = ref({});
  let loading = ref(true);
  let dataTimer;

  const props = defineProps({
    id: Number,
  });

  const getControllerNum = async () => {
    data.value = await maHttp.get(
      {
        url: `datasets/${props.id}/files/infos`,
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    );
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
  const name = '图片总数';
</script>
