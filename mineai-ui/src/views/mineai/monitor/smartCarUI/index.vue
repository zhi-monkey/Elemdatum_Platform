<template>
  <PageWrapper dense contentClass="flex px-2" class="bp">
    <BorderBox11
      :key="key"
      title="智慧车识别"
      :title-width="400"
      style="padding: 3.5% 1% 0.5%; height: 97vh"
    >
      <div style="height: 52%; display: flex; justify-content: space-around">
        <div style="width: 49%">
          <div class="flex">
            <img src="../../../../assets/icons/titles.svg" alt="标识符" />
            <span style="font-size: large; font-weight: bold">车辆监控视频</span></div
          >
          <BorderBox10 :key="key">
            <div
              style="
                padding: 1% 5%;
                height: 100%;
                width: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
              "
            >
              <Empty v-if="currentData.streamUrlLeft === ''" />
              <div v-else style="width: 97%; height: 90%">
                <Player
                  :key="key"
                  :off-screen="false"
                  style="margin-top: 1%; align-items: center"
                  :buffer-time="0.2"
                  :has-audio="true"
                  :use-m-s-e="true"
                  :debug="false"
                  :video-url="currentData.streamUrlLeft"
                  :buffer-delay-time="0"
                />
              </div>
            </div>
          </BorderBox10>
        </div>
        <div style="width: 49%">
          <div class="flex">
            <img src="../../../../assets/icons/titles.svg" alt="标识符" />
            <span style="font-size: large; font-weight: bold">车辆红外视频</span></div
          >
          <BorderBox10 :key="key">
            <div
              style="
                padding: 1% 5%;
                height: 100%;
                width: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
              "
            >
              <Empty v-if="currentData.streamUrlRight === ''" />
              <div v-else style="width: 97%; height: 90%">
                <Player
                  :key="key"
                  style="margin-top: 1%; align-items: center"
                  :off-screen="false"
                  :buffer-time="0.2"
                  :has-audio="true"
                  :use-m-s-e="true"
                  :debug="false"
                  :video-url="currentData.streamUrlRight"
                  :buffer-delay-time="0"
                />
              </div>
            </div>
          </BorderBox10>
        </div>
      </div>
      <div style="height: 43%; display: flex; justify-content: space-around; margin-top: 2%">
        <div style="width: 16%">
          <div class="flex">
            <img src="../../../../assets/icons/titles.svg" alt="标识符" />
            <span style="font-size: large; font-weight: bold">车辆信息</span>
          </div>
          <BorderBox9
            :key="key"
            style="height: 94%; padding-left: 3%"
            :color="['#4f65fb', '#273aba']"
          >
            <div style="height: 20%; padding-top: 8%" class="flex flex-center">
              <span style="font-size: 25px; color: #127ff1; width: 50%; text-align: end"
                >车辆编号:</span
              >
              <span
                style="
                  font-size: 25px;
                  width: 50%;
                  text-align: start;
                  color: #7cffb2;
                  margin-left: 3%;
                "
                >{{ currentData.carNumber }}</span
              >
            </div>
            <div style="height: 20%; padding-top: 8%" class="flex flex-center">
              <span style="font-size: 25px; color: #127ff1; width: 50%; text-align: end"
                >车辆时速:</span
              >
              <span
                style="
                  font-size: 25px;
                  width: 50%;
                  text-align: start;
                  color: #7cffb2;
                  margin-left: 3%;
                "
                >{{ currentData.speed }}km/h</span
              >
            </div>
            <div style="height: 20%; padding-top: 8%" class="flex flex-center">
              <span style="font-size: 25px; color: #127ff1; width: 50%; text-align: end"
                >续航里程:</span
              >
              <span
                style="
                  font-size: 25px;
                  width: 50%;
                  text-align: start;
                  color: #7cffb2;
                  margin-left: 3%;
                "
                >{{ currentData.endurance }}km</span
              >
            </div>
            <div style="height: 20%; padding-top: 8%" class="flex flex-center">
              <span style="font-size: 25px; color: #127ff1; width: 50%; text-align: end"
                >当前电量:</span
              >
              <span
                style="
                  font-size: 25px;
                  width: 50%;
                  text-align: start;
                  color: #7cffb2;
                  margin-left: 3%;
                "
                >{{ currentData.power }}%</span
              >
            </div>
            <div style="height: 20%; padding-top: 8%" class="flex flex-center">
              <span style="font-size: 25px; color: #127ff1; width: 50%; text-align: end"
                >电机转速:</span
              >
              <span
                style="
                  font-size: 25px;
                  width: 50%;
                  text-align: start;
                  color: #7cffb2;
                  margin-left: 3%;
                "
                >{{ currentData.rotation }}r/s</span
              >
            </div>
          </BorderBox9>
        </div>
        <div style="width: 26%">
          <div class="flex">
            <img src="../../../../assets/icons/titles.svg" alt="标识符" />
            <span style="font-size: large; font-weight: bold">环境参数</span>
          </div>
          <BorderBox9 :key="key" style="height: 94%; padding-left: 10%">
            <div style="width: 50%; height: 20%; padding-top: 5.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">积水深度:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.pondingDepth }}<span style="font-size: 20px">mm</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 5.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">最高温度:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.maxTemperature }}<span style="font-size: 20px">℃</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 5.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">噪声:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%">
                {{ currentData.noise }}<span style="font-size: 20px">db</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 5.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">粉尘:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.dust }}<span style="font-size: 20px">mg/m³</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 5%; display: inline-block">
              <span style="font-size: 23px; color: #127ff1; text-align: start">风速:</span>
              <span style="font-size: 23px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.windSpeed }}<span style="font-size: 20px">km/h</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">CH₄:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.ch4 }}<span style="font-size: 20px">ppm</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 4.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">CO:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.co }}<span style="font-size: 20px">ppm</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 4.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">CO₂:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.co2 }}<span style="font-size: 20px">ppm</span></span
              >
            </div>
            <div style="width: 50%; height: 20%; padding-top: 4.5%; display: inline-block">
              <span style="font-size: 25px; color: #127ff1; text-align: start">H₂S:</span>
              <span style="font-size: 25px; color: #7cffb2; margin-left: 3%"
                >{{ currentData.h2s }}<span style="font-size: 20px">ppm</span></span
              >
            </div>
          </BorderBox9>
        </div>
        <div style="width: 14%" class="flex flex-col justify-start items-center">
          <div
            style="width: 100%; height: 35%; margin-top: 5%"
            class="flex flex-col justify-center items-center"
          >
            <Decoration7 :key="key" style="height: 30%"
              ><span
                style="
                  font-size: 25px;
                  font-weight: bold;
                  text-align: center;
                  margin-left: 5%;
                  margin-right: 5%;
                "
                >车辆总数</span
              >
            </Decoration7>
            <div
              :key="key"
              style="
                height: 40%;
                width: 70%;
                display: flex;
                justify-content: center;
                position: relative;
                align-items: center;
                margin-top: 5%;
              "
            >
              <Button
                @click="controlSelect"
                style="width: 100%; position: absolute; align-self: flex-end; z-index: 1"
              >
                <span style="font-size: 30px; font-weight: bold">{{ totalNumber }}</span>
              </Button>
              <Select
                style="width: 100%; position: absolute; align-self: flex-end"
                size="large"
                :key="key"
                :open="isOpen"
                :onSelect="jumpTo"
                :bordered="false"
                :showArrow="false"
                dropdownClassName="selectDropdown"
              >
                <SelectOption v-for="(car, index) in carList" :key="index">
                  {{ car }}
                </SelectOption>
              </Select>
            </div>
          </div>
          <Decoration11 :key="key" style="width: 100%; height: 25%">
            <Carousel
              ref="refCarousel"
              autoplay
              :dots="false"
              dotPosition="left"
              :autoplaySpeed="5000"
              :beforeChange="switchData"
              style="width: 80%"
            >
              <template v-for="index in carList" :key="index">
                <div>
                  <div style="width: 100%; height: 100%; display: flex; justify-content: center">
                    <span style="font-size: 30px; color: #7cffb2">{{ index }}</span>
                  </div>
                </div>
              </template>
            </Carousel>
          </Decoration11>
          <div
            v-show="currentData.faultWarn === '1'"
            style="width: 80%; height: 5%; margin-top: 10%"
          >
            <Alert message="发生故障" showIcon type="warning" />
          </div>
          <div
            v-show="currentData.patrolWarn === '1'"
            style="width: 80%; height: 5%; margin-top: 10%"
          >
            <Alert message="巡检异常" showIcon type="warning" />
          </div>
        </div>
        <div style="width: 22%">
          <div class="flex flex-row">
            <img src="../../../../assets/icons/titles.svg" alt="标识符" />
            <span style="font-size: large; font-weight: bold">断面扫描</span>
          </div>
          <BorderBox9 :key="key" style="height: 94%">
            <div
              style="
                width: 100%;
                height: 100%;
                display: flex;
                justify-content: center;
                align-items: center;
              "
            >
              <Empty v-if="currentData.imageUrl === ''" />
              <div v-else style="width: 100%; height: 100%" class="flex flex-col justify-center"
                ><div style="width: 100%; height: 15%" class="flex flex-row justify-center gap-x-1">
                  <div
                    style="
                      width: 20%;
                      height: 100%;
                      display: flex;
                      flex-direction: column;
                      justify-content: center;
                    "
                    ><span
                      style="color: #00ddeb; font-size: 22px; font-weight: bold; text-align: end"
                      >匹配度</span
                    ></div
                  >
                  <PercentPond
                    :key="key"
                    :config="{ value: similarity, colors: ['#01c4f9', '#c135ff'] }"
                    style="width: 70%; height: 100%"
                  />
                </div>
                <div style="height: 60%; margin-top: 5%">
                  <img
                    src="./scan.jpg"
                    style="width: 96%; height: 100%; padding-left: 5%"
                  /> </div></div
            ></div>
          </BorderBox9>
        </div>
        <div style="width: 20%">
          <div class="flex">
            <img src="../../../../assets/icons/titles.svg" alt="标识符" />
            <span style="font-size: large; font-weight: bold">空气质量</span>
          </div>
          <BorderBox9 :key="key" style="height: 94%">
            <div style="width: 100%; height: 25%; padding-top: 5%">
              <BorderBox3 :key="key" style="width: 80%; margin-left: 10%">
                <div style="width: 100%; height: 100%; display: flex; align-items: center"
                  ><span style="width: 50%; font-size: 30px; color: #127ff1; text-align: end"
                    >温度：</span
                  >
                  <span style="width: 50%; font-size: 30px; text-align: start; color: #7cffb2"
                    >{{ currentData.temperature }}℃</span
                  ></div
                >
              </BorderBox3>
            </div>
            <div
              style="width: 100%; height: 65%; padding-top: 5%"
              class="flex flex-row justify-center"
            >
              <div style="height: 100%; width: 50%; align-items: center" class="flex flex-col">
                <WaterLevelPond
                  :key="key"
                  :config="{ data: [currentData.o2], shape: 'roundRect', waveHeight: 10 }"
                  style="height: 90%; width: 85%"
                />
                <span style="height: 10%; font-size: 25px; color: #127ff1; text-align: center"
                  >O₂浓度</span
                >
              </div>
              <div style="height: 100%; width: 50%; align-items: center" class="flex flex-col">
                <WaterLevelPond
                  :key="key"
                  :config="{ data: [currentData.humidity], shape: 'roundRect', waveHeight: 10 }"
                  style="height: 90%; width: 85%"
                />
                <span style="height: 10%; font-size: 25px; color: #127ff1; text-align: center"
                  >湿度</span
                >
              </div>
            </div>
          </BorderBox9>
        </div>
      </div>
    </BorderBox11>
  </PageWrapper>
</template>
<script lang="ts">
  import { PageWrapper } from '/@/components/Page';
  import Player from '../monitor-info/MainPlayer1.vue';
  import { Carousel, Alert, Select, SelectOption, Empty } from 'ant-design-vue';
  import { nextTick, onMounted, onUnmounted, ref, inject } from 'vue';
  import {
    BorderBox3,
    BorderBox9,
    BorderBox10,
    BorderBox11,
    WaterLevelPond,
    Decoration7,
    Decoration11,
    PercentPond,
  } from '@kjgl77/datav-vue3';

  export default {
    components: {
      PageWrapper,
      Alert,
      Select,
      SelectOption,
      Empty,
      Player,
      Carousel,
      BorderBox3,
      BorderBox9,
      BorderBox10,
      BorderBox11,
      WaterLevelPond,
      Decoration7,
      Decoration11,
      PercentPond,
    },
    setup() {
      const reload = inject('reload') as Function;
      let dataTimer;
      let key = ref();
      let similarity = ref(70);
      let data = ref();
      let carList = ref();
      let isOpen = ref(false);
      let totalNumber = ref();
      let currentData = ref({
        id: '',
        carId: '',
        carNumber: '',
        faultWarn: '',
        patrolWarn: '',
        speed: '',
        endurance: '',
        power: '',
        rotation: '',
        windSpeed: '',
        dust: '',
        ch4: '',
        noise: '',
        co: '',
        co2: '',
        h2s: '',
        pondingDepth: '',
        maxTemperature: '',
        o2: '',
        humidity: '',
        temperature: '',
        streamUrlLeft: '',
        streamUrlRight: '',
        imageUrl: '',
      });

      const refCarousel = ref<InstanceType<typeof Carousel>>();

      onMounted(async () => {
        window.addEventListener('resize', repaint);
        await getData();
        getCarouselData();
        currentData.value = data.value[0];
        //每小时刷新一下页面
        dataTimer = setInterval(reload, 3600000);
      });

      onUnmounted(() => {
        clearInterval(dataTimer);
        window.removeEventListener('resize', repaint);
      });
      //在窗口宽高变化后重绘dataV组件
      function repaint() {
        nextTick(() => {
          key.value++;
        });
      }
      //获取数据
      async function getData() {
        data.value = [
          {
            id: '1',
            carId: '矿A-88888',
            carNumber: 'RLX-001',
            faultWarn: '1',
            patrolWarn: '1',
            speed: '100',
            endurance: '1000',
            power: '97',
            rotation: '90',
            windSpeed: '80',
            dust: '100',
            noise: '35',
            ch4: '30000',
            co: '30',
            co2: '407',
            h2s: '3',
            pondingDepth: '300',
            maxTemperature: '70',
            o2: '20',
            humidity: '38',
            temperature: '21',
            streamUrlLeft: 'ws://localhost/live/monitor/stream_1.live.flv',
            streamUrlRight: 'ws://localhost/live/monitor/stream_1.live.flv',
            imageUrl: 'nxa',
          },
          {
            id: '2',
            carId: '矿A-99999',
            carNumber: 'RLX-002',
            faultWarn: '1',
            patrolWarn: '0',
            speed: '90',
            endurance: '999',
            power: '94',
            rotation: '80',
            windSpeed: '70',
            dust: '110',
            ch4: '29800',
            noise: '60',
            co: '40',
            co2: '400',
            h2s: '2',
            pondingDepth: '240',
            maxTemperature: '50',
            o2: '21',
            humidity: '56',
            temperature: '15',
            streamUrlLeft: 'ws://localhost/live/monitor/stream_1.live.flv',
            streamUrlRight: 'ws://localhost/live/monitor/stream_1.live.flv',
            imageUrl: '',
          },
          {
            id: '3',
            carId: '矿A-00000',
            carNumber: 'RLX-003',
            faultWarn: '0',
            patrolWarn: '1',
            speed: '110',
            endurance: '850',
            power: '96',
            rotation: '35',
            windSpeed: '30',
            dust: '105',
            noise: '25',
            ch4: '28000',
            co: '28',
            co2: '400',
            h2s: '4',
            pondingDepth: '150',
            maxTemperature: '25',
            o2: '23',
            humidity: '35',
            temperature: '19',
            streamUrlLeft: 'ws://localhost/live/monitor/stream_1.live.flv',
            streamUrlRight: 'ws://localhost/live/monitor/stream_1.live.flv',
            imageUrl: 'mkza',
          },
        ];
      }
      //切换数据
      function switchData(_, to) {
        repaint();
        currentData.value = data.value[to];
      }
      //获取轮播数据
      function getCarouselData() {
        let temp: string[] = [];
        data.value.forEach((value) => {
          temp.push(value.carId);
        });
        carList.value = temp;
        //获取车辆总数
        totalNumber.value = carList.value.length;
      }
      //展开下拉列表
      function controlSelect() {
        isOpen.value = !isOpen.value;
      }
      //跳转对应车辆
      function jumpTo(target: number) {
        isOpen.value = false;
        refCarousel.value?.goTo(target);
      }
      return {
        key,
        similarity,
        data,
        currentData,
        getData,
        switchData,
        carList,
        isOpen,
        totalNumber,
        refCarousel,
        controlSelect,
        jumpTo,
      };
    },
  };
</script>
<style lang="less" scoped>
  .bp {
    background-image: url(../../../../assets/images/bigScreenBackground.png);
    width: 100%;
    height: 100%;
    background-size: 100% 100%;
  }
  button {
    align-items: center;
    background-image: linear-gradient(144deg, #af40ff, #5b42f3 50%, #00ddeb);
    border: 0;
    border-radius: 8px;
    box-shadow: rgba(151, 65, 252, 0.2) 0 15px 30px -5px;
    box-sizing: border-box;
    color: #ffffff;
    display: flex;
    font-family: Phantomsans, sans-serif;
    font-size: 18px;
    justify-content: center;
    line-height: 1em;
    max-width: 100%;
    min-width: 140px;
    padding: 3px;
    text-decoration: none;
    user-select: none;
    -webkit-user-select: none;
    touch-action: manipulation;
    white-space: nowrap;
    cursor: pointer;
    transition: all 0.3s;
  }

  button:active,
  button:hover {
    outline: 0;
  }

  button span {
    background-color: rgb(5, 6, 45);
    padding: 16px 24px;
    border-radius: 6px;
    width: 100%;
    height: 100%;
    transition: 300ms;
  }

  button:hover span {
    background: none;
  }

  button:active {
    transform: scale(0.9);
  }

  //.ant-select-dropdown {
  //  border-radius: 4px;
  //}
  //.selectDropdown {
  //  padding-left: 4px;
  //  padding-right: 4px;
  //  background-color: #104e8b !important;
  //  .ant-select-item-option-active:not(.ant-select-item-option-disabled) {
  //    color: #00ddeb;
  //    background-color: #00868b;
  //  }
  //  .ant-select-item {
  //    border-radius: 6px;
  //  }
  //}
</style>
