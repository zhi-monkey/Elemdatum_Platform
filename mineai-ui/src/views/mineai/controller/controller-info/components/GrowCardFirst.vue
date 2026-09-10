<template>
  <div class="flex py-1 px-1"
    ><img src="../../../../../assets/icons/titles.svg" alt="标识符" /><span
      style="font-size: medium"
      >控制器总览</span
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
          <div class="py-0 flex justify-center text-1xl">{{ name }}</div>
          <div class="py-2 flex justify-center" style="margin-bottom: 30px">
            <CountTo
              :startVal="0"
              :endVal="item.controllerAllNum"
              class="text-3xl"
              color="#7CFFB2"
              style="font-weight: bold"
            />
          </div>

          <a-row>
            <a-col :span="12" style="margin-bottom: 12px">
              <div class="flex justify-center texst-1xl"
                >运行
                <CountTo
                  :startVal="0"
                  :endVal="item.workingNum"
                  class="text-1xl"
                  color="green"
                  style="margin-left: 10px; font-size: large; font-weight: bold"
                />
              </div>
            </a-col>
            <a-col :span="12">
              <div class="flex justify-center text-1xl">
                <span>故障</span>
                <CountTo
                  :startVal="0"
                  :endVal="item.errorNum"
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
  import { Card } from 'ant-design-vue';
  import { Row as ARow, Col as ACol } from 'ant-design-vue';
  import { onMounted, onUnmounted, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const data = ref([{}]);
  const loading = ref(true);
  let dataTimer;
  const getControllerNum = async () => {
    const v = await maHttp.get(
      {
        url: 'controller/getControllerNum',
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
    await getControllerNum();
    loading.value = false;
    dataTimer = setInterval(getControllerNum, 10000);
  });

  onUnmounted(() => clearInterval(dataTimer));
  const name = '算力控制器';
</script>
