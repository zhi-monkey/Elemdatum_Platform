<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../../assets/icons/titles.svg" /><span style="font-size: medium"
      >图片标注信息总览</span
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
          <div class="py-0 flex justify-center text-1xl">图片总数</div>
          <div class="py-2 flex justify-center" style="margin-bottom: 8px">
            <CountTo
              :startVal="0"
              :endVal="item.allFiles"
              class="text-3xl"
              color="#7CFFB2"
              style="font-weight: bold"
            />
          </div>
          <div>
            <a-row style="margin-bottom: 30px">
              <a-col :span="12">
                <div class="flex justify-center text-1xl"
                  ><span>未标注</span>
                  <CountTo
                    :startVal="0"
                    :endVal="item.unannotatedFiles"
                    class="text-1xl"
                    color="#B5C0E5"
                    style="margin-left: 10px; font-size: large; font-weight: bold"
                  />
                </div>
              </a-col>
              <a-col :span="12">
                <div class="flex justify-center text-1xl">
                  <span>已标注</span>
                  <CountTo
                    :startVal="0"
                    :endVal="item.annotatedFiles"
                    class="text-1xl"
                    color="orange"
                    style="margin-left: 10px; font-size: large; font-weight: bold"
                  />
                </div>
              </a-col>
              <a-col :span="24" style="margin-bottom: 10px" />
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
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  const data = ref([{}]);
  const loading = ref(true);
  let timer;

  const getData = async () => {
    const v = await maHttp.get(
      {
        url: 'datasets/versions/countByFileAnnotate',
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
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
