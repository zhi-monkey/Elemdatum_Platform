<template xmlns:a-row="http://www.w3.org/1999/html">
  <div class="flex py-1 px-1">
    <img src="../../../../../../assets/icons/titles.svg" alt="标识符" />
    <span style="font-size: medium">监控类型总览</span>
  </div>
  <div class="md:flex">
    <template v-for="item in data" :key="item">
      <Card
        size="small"
        :loading="loading"
        class="md:w-1/1 !md:mx-2 !md:my-0 !my-4 w-full"
        :canExpan="false"
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
            <a-col :span="12" style="margin-bottom: 13px">
              <div class="flex justify-center text-1xl"
                >音频
                <CountTo
                  :startVal="0"
                  :endVal="item.audio"
                  class="text-1xl"
                  color="#cac10a"
                  style="margin-left: 10px; font-weight: bold"
                />
              </div>
            </a-col>
            <a-col :span="12" style="margin-bottom: 13px">
              <div class="flex justify-center text-1xl">
                <span>视频</span>
                <CountTo
                  :startVal="0"
                  :endVal="item.video"
                  class="text-1xl"
                  color="#32A1C3"
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
              <a-col :span="12">
                <div class="flex justify-center text-1xl"
                  >点云
                  <CountTo
                    :startVal="0"
                    :endVal="item.pointCloud"
                    class="text-1xl"
                    color="#cac10a"
                    style="margin-left: 10px; font-weight: bold"
                  />
                </div>
              </a-col>
              <a-col :span="12">
                <div class="flex justify-center text-1xl">
                  <span>图像</span>
                  <CountTo
                    :startVal="0"
                    :endVal="item.image"
                    class="text-1xl"
                    color="#32A1C3"
                    style="margin-left: 10px; font-weight: bold"
                  />
                </div>
              </a-col>
            </a-row>
          </div>
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
  import { Icon } from '/@/components/Icon';

  const data = ref([{}]);
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
