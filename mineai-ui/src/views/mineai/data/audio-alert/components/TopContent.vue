<template>
  <border-box13>
    <div style="height: 220px">
      <a-row :gutter="6">
        <a-col :span="8">
          <div class="h-full w-full flex flex-row justify-around p-6">
            <div
              style="justify-content: center; align-items: center"
              class="flex flex-col justify-center w-1/3"
            >
              <div>
                <CountTo
                  suffix="个"
                  :color="'#094bd9'"
                  :startVal="0"
                  :endVal="normal"
                  :duration="1500"
                  style="font-size: 32px; font-weight: bold"
                />
              </div>
              <div><span style="font-size: 24px; font-weight: bold">正常拾音器</span></div>
            </div>
            <div
              style="justify-content: center; align-items: center"
              class="flex flex-col justify-center w-1/3"
            >
              <div>
                <CountTo
                  suffix="个"
                  :color="'#c490e6'"
                  :startVal="0"
                  :endVal="abnormal"
                  :duration="1500"
                  style="font-size: 32px; font-weight: bold"
                />
              </div>
              <div><span style="font-size: 24px; font-weight: bold">异常拾音器</span></div>
            </div>
            <div
              style="justify-content: center; align-items: center"
              class="flex flex-col justify-center w-1/3"
            >
              <div>
                <CountTo
                  suffix="个"
                  :color="'#d1a002'"
                  :startVal="0"
                  :endVal="unknown"
                  :duration="1500"
                  style="font-size: 32px; font-weight: bold"
                />
              </div>
              <div><span style="font-size: 24px; font-weight: bold">无状态拾音器</span></div>
            </div>
          </div>
        </a-col>
        <a-col :span="16">
          <div class="p-6 grid grid-cols-8 gap-2">
            <template v-for="v in data" :key="v.id">
              <div
                v-if="v.status === 1"
                style="
                  background-color: #0161de;
                  text-align: center;
                  border-radius: 4px;
                  padding-top: 2px;
                  padding-bottom: 2px;
                "
                ><span style="font-size: 16px; font-weight: bold">{{ v.id }}号拾音器</span>
              </div>
              <div
                v-if="v.status === 2"
                style="
                  background-color: #c490e6;
                  text-align: center;
                  border-radius: 4px;
                  padding-top: 2px;
                  padding-bottom: 2px;
                "
                ><span style="font-size: 16px; font-weight: bold">{{ v.id }}号拾音器</span>
              </div>
              <div
                v-if="v.status === 0"
                style="
                  background-color: #d1a002;
                  text-align: center;
                  border-radius: 4px;
                  padding-top: 2px;
                  padding-bottom: 2px;
                "
                ><span style="font-size: 16px; font-weight: bold">{{ v.id }}号拾音器</span>
              </div>
            </template>
          </div>
        </a-col>
      </a-row>
    </div>
  </border-box13>
</template>

<script lang="ts" setup>
  import { onMounted, onUnmounted, ref } from 'vue';
  import { CountTo } from '/@/components/CountTo';
  import { Col as ACol, Row as ARow } from 'ant-design-vue';
  import { BorderBox13 } from '@kjgl77/datav-vue3';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

  const data: any = ref([]);
  const normal = ref(0);
  const abnormal = ref(0);
  const unknown = ref(0);

  let timer;
  const getStatus = async () => {
    interface dataForm {
      id: number;
      status: number;
    }
    const v = await maHttp.get(
      {
        url: 'audio/getAudioMonitoringStatus',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.DATA_MANAGER },
    );
    let temp: dataForm[] = [];
    let temp_normal = 0;
    let temp_abnormal = 0;
    let temp_unknown = 0;
    for (const item in v) {
      temp.push({ id: Number(item), status: v[item] });
      if (v[item] === 0) {
        temp_unknown++;
      } else if (v[item] === 1) {
        temp_normal++;
      } else {
        temp_abnormal++;
      }
    }
    data.value = temp;
    normal.value = temp_normal;
    abnormal.value = temp_abnormal;
    unknown.value = temp_unknown;
  };
  //先执行一次，再开启定时器，定时更新data
  onMounted(async () => {
    await getStatus();
    timer = setInterval(getStatus, 10000);
  });

  onUnmounted(() => clearInterval(timer));
</script>
