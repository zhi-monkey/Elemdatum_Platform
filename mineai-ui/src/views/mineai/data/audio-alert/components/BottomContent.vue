<template>
  <div id="bottom-content-wrapper" style="height: 100%; width: 100%">
    <a-row :gutter="6">
      <a-col :span="8">
        <div class="h-full w-full flex flex-col p-6 gap-y-25">
          <div style="height: 50%; width: 100%">
            <decoration3 style="width: 115px; height: 20px" :key="key" />
            <div><span style="font-size: 18px; font-weight: bold">上报类别占比</span></div>
            <active-ring-chart :config="ring_config" style="height: 80%" :key="key" />
          </div>
          <div style="height: 50%; width: 100%">
            <decoration3 style="width: 130px; height: 20px" :key="key" />
            <div><span style="font-size: 18px; font-weight: bold">拾音器上报排名</span></div>
            <capsule-chart :config="chart_config" style="height: 80%" :key="key" />
          </div>
        </div>
      </a-col>
      <a-col :span="16">
        <div style="height: 60vh; width: 100%; padding: 8px">
          <scroll-board :config="config" :key="key" />
        </div>
      </a-col>
    </a-row>
  </div>
</template>
<script lang="ts" setup>
  import { Col as ACol, Row as ARow } from 'ant-design-vue';
  import { ScrollBoard, CapsuleChart, Decoration3, ActiveRingChart } from '@kjgl77/datav-vue3';
  import elementResizeDetectorMaker from 'element-resize-detector';
  import { onMounted, onUnmounted, reactive, ref } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import debounce from 'lodash/debounce';

  let timer;
  const config = reactive({
    header: ['拾音器编号', '上报内容', '坐标', '上报时间'],
    data: [],
    index: true,
    columnWidth: [100, 150, 100, 500, 150],
    align: ['center', 'center', 'center', 'left', 'center'],
    rowNum: 15,
    headerBGC: '#1981f6',
    headerHeight: 45,
    oddRowBGC: 'rgba(0, 44, 81, 0.8)',
    evenRowBGC: 'rgba(10, 29, 50, 0.8)',
  });

  const chart_config = reactive({
    data: [],
    colors: ['#e062ae', '#fb7293', '#e690d1', '#32c5e9', '#96bfff'],
    unit: '个',
  });

  const ring_config = reactive({
    radius: '75%',
    activeRadius: '85%',
    data: [{ name: '无报警', value: 0 }],
    lineWidth: 10,
    showOriginValue: true,
    color: ['#fb7293', '#96bfff'], //'#32c5e9'
    digitalFlopStyle: {
      fontSize: 22,
      fill: '#fff',
    },
  });

  const key = ref(0);

  let erd: elementResizeDetectorMaker.Erd = elementResizeDetectorMaker();
  //左侧菜单栏展开与收起切换时，重绘组件，防止组件显示错位
  onMounted(async () => {
    erd.listenTo(
      document.getElementById('bottom-content-wrapper') as HTMLElement,
      // 防抖函数，只执行1s内最后一次回调
      debounce(function () {
        // div宽度变化，子组件重新渲染
        key.value++;
      }, 1000),
    );
  });

  const getData = async () => {
    maHttp
      .get(
        {
          url: 'audioAlert/getAlertList',
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.DATA_MANAGER },
      )
      .then((v) => {
        let temp: any = [];
        if (v) {
          v.content.forEach((value) => {
            let audioData_temp: string[] = [];
            audioData_temp.push(value.pickUpId + '');
            audioData_temp.push(value.content);
            audioData_temp.push(value.position);
            audioData_temp.push(value.alertTime);
            temp.push(audioData_temp);
          });
        }
        // audioData.value = temp;
        config.data = temp;
      });
  };

  const getAlertRank = async () => {
    maHttp
      .get(
        {
          url: 'audioAlert/getAlertRank',
          params: { num: 5 },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.DATA_MANAGER },
      )
      .then((v) => {
        let temp: any = [];
        if (v) {
          for (const item in v) {
            temp.push({ name: item, value: v[item] });
          }
        }
        chart_config.data = temp;
      });
  };

  const getTypeRank = async () => {
    maHttp
      .get(
        {
          url: 'audioAlert/getTypeRank',
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.DATA_MANAGER },
      )
      .then((v) => {
        let temp: any = [];
        let total = 0;
        if (v) {
          for (const item in v) {
            total += v[item];
            temp.push({ name: item, value: v[item] });
          }
        }
        if (total === 0) {
          ring_config.data = [{ name: '无报警', value: 0 }];
        } else {
          ring_config.data = temp;
        }
      });
  };

  //先执行一次，再开启定时器，定时更新data
  onMounted(async () => {
    await Promise.all([getData(), getTypeRank(), getAlertRank()]);
    timer = setInterval(() => Promise.all([getData(), getTypeRank(), getAlertRank()]), 10000);
  });

  onUnmounted(() => {
    clearInterval(timer);
  });
</script>
<style scoped>
  #scroll-board {
    width: 100%;
    box-sizing: border-box;
    height: 60vh;
    overflow: hidden;
    padding: 3px;
  }
</style>
