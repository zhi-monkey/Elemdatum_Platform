<template>
  <div class="flex py-1 px-1">
    <img src="../../../../../../assets/icons/titles.svg" alt="标识符" />
    <span style="font-size: medium">监控类型总览</span>
  </div>
  <div class="md:flex">
    <Card
      size="small"
      :loading="loading"
      class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
      :canExpan="false"
      style="height: calc(100% - 20px)"
    >
      <div style="display: inline">
        <a-row>
          <a-col :span="12" style="margin-bottom: 13px">
            <div class="flex justify-center text-1xl">
              <Icon icon="ant-design:audio-twotone" size="25" />
            </div>
          </a-col>
          <a-col :span="12" style="margin-bottom: 13px">
            <div class="flex justify-center text-1xl">
              <Icon icon="ant-design:video-camera-twotone" size="25" />
            </div>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="12" style="margin-bottom: 14px">
            <div class="pt-1 flex justify-center text-1xl"
              ><span style="opacity: 0.8">音频</span>
              <CountTo
                :startVal="0"
                :endVal="data.audio"
                class="text-1xl"
                color="#fc1d7b"
                style="margin-left: 10px; font-weight: bold"
              />
            </div>
          </a-col>
          <a-col :span="12">
            <div class="pt-1 flex justify-center text-1xl">
              <span style="opacity: 0.8">视频</span>
              <CountTo
                :startVal="0"
                :endVal="data.video"
                class="text-1xl"
                color="#218cfc"
                style="margin-left: 10px; font-weight: bold"
              />
            </div>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="12" style="margin-bottom: 12px">
            <div class="flex justify-center text-1xl">
              <Icon icon="ant-design:cloud-twotone" size="25" />
            </div>
          </a-col>
          <a-col :span="12" style="margin-bottom: 12px">
            <div class="flex justify-center text-1xl">
              <Icon icon="ant-design:file-image-twotone" size="25" />
            </div>
          </a-col>
        </a-row>
        <div>
          <a-row>
            <a-col :span="12" style="margin-bottom: 3px">
              <div class="flex justify-center text-1xl"
                ><span style="opacity: 0.8">点云</span>
                <CountTo
                  :startVal="0"
                  :endVal="data.pointCloud"
                  class="text-1xl"
                  color="#24ffb8"
                  style="margin-left: 10px; font-weight: bold"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span style="opacity: 0.8">图像</span>
                <CountTo
                  :startVal="0"
                  :endVal="data.image"
                  class="text-1xl"
                  color="#a421fa"
                  style="margin-left: 10px; font-weight: bold"
                />
              </div>
            </a-col>
          </a-row>
        </div>
      </div>
    </Card>
  </div>
</template>
<script lang="ts" setup>
  import { CountTo } from '/@/components/CountTo';
  import { Card } from 'ant-design-vue';
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { Icon } from '/@/components/Icon';
  let data = ref({});
  const loading = ref(true);
  let timer;

  const getMonitorNum = async () => {
    const v = await maHttp.get(
      {
        url: 'monitor/getDataType',
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
    timer = setInterval(getMonitorNum, 10000);
  });

  onUnmounted(() => {
    //删除数据请求定时器
    clearInterval(timer);
  });
</script>
