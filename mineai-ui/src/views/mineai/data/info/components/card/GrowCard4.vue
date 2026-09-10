<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../../assets/icons/titles.svg" /><span style="font-size: medium"
      >报警信息类型总览</span
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
          <div class="py-0 flex justify-center text-1xl">总报警数量</div>
          <div class="py-2 flex justify-center" style="margin-bottom: 8px">
            <CountTo
              :startVal="0"
              :endVal="item.modelAlertNum"
              class="text-3xl"
              color="#7CFFB2"
              style="font-weight: bold"
            />
          </div>
          <div>
            <a-row>
              <a-col :span="24" style="margin-bottom: 10px">
                <div class="flex justify-center text-1xl" style="align-items: center"
                  >图片
                  <CountTo
                    :startVal="0"
                    :endVal="item.image"
                    class="text-1xl"
                    color="red"
                    style="margin-left: 20px; font-size: large; font-weight: bold"
                  />
                </div>
              </a-col>
            </a-row>
          </div>
          <a-row>
            <a-col :span="12" style="margin-bottom: 10px">
              <div class="flex justify-center text-1xl"
                >音频
                <CountTo
                  :startVal="0"
                  :endVal="item.audio"
                  class="text-1xl"
                  color="green"
                  style="margin-left: 10px; font-size: large; font-weight: bold"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span>视频</span>
                <CountTo
                  :startVal="0"
                  :endVal="item.video"
                  class="text-1xl"
                  color="#B5C0E5"
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
        url: 'modelAlert/getModelAlertDataType',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
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

<style scoped>
  @import url('/@/assets/css/via_image_annotator.css');
</style>
