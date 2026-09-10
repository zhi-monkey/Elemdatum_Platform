<template>
  <div style="width: 100%; height: 100%; position: absolute" class="bp">
    <a-row style="height: 100%" type="flex" :wrap="false">
      <a-col
        v-show="settings.alertTableShow || settings.treeShow"
        flex="300px"
        style="padding: 5px"
      >
        <a-spin :spinning="treeLoading">
          <div class="tree-select">
            <Tree
              v-if="settings.treeShow"
              v-model:checked-keys="checkedKeys"
              v-model:expanded-keys="expandedKeys"
              draggable
              :treeData="treeData"
              checkable
              title="场景"
              @drop="onDrop"
              @dragstart="onDragStart"
            />
          </div>
        </a-spin>
        <alert-table v-if="settings.alertTableShow" :columns="alertTableColumns" />
      </a-col>
      <a-col flex="auto">
        <div
          style="
            height: 92vh;
            width: 100%;
            display: flex;
            flex-wrap: wrap;
            align-content: flex-start;
          "
        >
          <div
            v-for="videoSourceFull in treeItemsThisPage"
            :key="videoSourceFull.key"
            :style="getCardStyle"
          >
            <main-player
              v-if="settings.playerType === PlayerTypeEnum.JESSIBUCA"
              :buffer-time="settings.jessibuca.bufferTime"
              :buffer-delay-time="settings.jessibuca.bufferDelayTime"
              :debug="settings.jessibuca.debug"
              :has-audio="settings.hasAudio"
              :off-screen="settings.jessibuca.offScreen"
              :title="videoSourceFull.useCustomInfo ? videoSourceFull.fullTitle : ''"
              :content-list="videoSourceFull.areaLabel"
              :monitor-stream="videoSourceFull.fullMonitorStreamUrl"
              :model-stream="videoSourceFull.fullModelStreamUrl"
              :monitor-info="videoSourceFull.monitorInfo"
              :use-m-s-e="settings.jessibuca.decodeMode === DecodeModeEnum.MSE"
              :is-model-stream-show="videoSourceFull.pushStream"
              :show-btn="hasPermission([RoleEnum.IntelligentMonitorViewSwitch])"
            />
            <backup-player
              v-else-if="settings.playerType === PlayerTypeEnum.LIVE_PLAYER"
              :has-audio="settings.hasAudio"
              :title="videoSourceFull.useCustomInfo ? videoSourceFull.fullTitle : ''"
              :monitor-stream="videoSourceFull.fullMonitorStreamUrl"
              :model-stream="videoSourceFull.fullModelStreamUrl"
              :monitor-info="videoSourceFull.monitorInfo"
              :is-model-stream-show="videoSourceFull.pushStream"
              :show-btn="hasPermission([RoleEnum.IntelligentMonitorViewSwitch])"
            />
            <div class="flex flex-row gap-x-2 ml-0.5">
              <div>
                <SvgIcon name="camera" />
              </div>
              <Ellipsis
                :full-str="
                  videoSourceFull.monitorInfo.scene + '-' + videoSourceFull.monitorInfo.monitor
                "
              />
            </div>
          </div>

          <div v-for="item in videoNotShownNum" :style="getCardStyle" :key="item">
            <div class="ant-empty" style="height: 96%; margin: 0">
              <div class="ant-empty-image" style="height: 100%; background-color: rgb(20, 24, 41)">
                <svg
                  t="1680504585131"
                  class="icon"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="1101"
                  width="128"
                  height="128"
                >
                  <path
                    d="M926.826667 86.506667c15.914667 15.893333 15.936 41.941333 0.576 59.605333l-2.304 2.474667-90.730667 90.688A382.208 382.208 0 0 1 896 448c0 141.077333-76.8 267.669333-195.306667 334.549333l-6.741333 3.712-6.165333 3.221334 54.378666 108.778666c13.226667 26.453333-3.84 57.301333-31.978666 61.312l-3.050667 0.32L704 960H320c-30.656 0-50.965333-31.189333-39.466667-58.88l1.301334-2.88 54.378666-108.757333-6.144-3.2c-9.173333-4.949333-18.133333-10.24-26.816-15.893334l-144 144.042667c-17.642667 17.621333-45.44 18.389333-62.08 1.728-15.914667-15.893333-15.936-41.941333-0.576-59.605333l2.304-2.474667 765.866667-765.845333c17.621333-17.621333 45.397333-18.389333 62.08-1.728zM772.266667 301.397333l-96.938667 96.96a170.666667 170.666667 0 0 1-212.970667 212.970667l-96.981333 96.96c13.866667 7.829333 28.48 14.592 43.733333 20.202667a42.666667 42.666667 0 0 1 24.832 56.149333l-1.365333 2.986667L389.034667 874.666667h245.930666l-43.52-87.04a42.666667 42.666667 0 0 1 17.536-56.448l2.88-1.450667 3.050667-1.258667A298.773333 298.773333 0 0 0 810.666667 448c0-53.290667-13.952-103.296-38.4-146.602667zM512 64c76.416 0 147.605333 22.314667 207.424 60.8l-62.208 62.165333A297.322667 297.322667 0 0 0 512 149.333333c-164.949333 0-298.666667 133.717333-298.666667 298.666667 0 52.224 13.525333 101.909333 37.653334 145.216L188.8 655.424a383.125333 383.125333 0 0 1-60.373333-189.568l-0.341334-9.877333L128 448c0-212.074667 171.925333-384 384-384z m0 213.333333c16.682667 0 32.789333 2.389333 48.021333 6.848l-211.84 211.84A170.666667 170.666667 0 0 1 512 277.333333z"
                    fill="#333333"
                    p-id="1102"
                  />
                </svg>
              </div>
            </div>
          </div>
        </div>
        <div
          :style="{
            'padding-left': '8px',
            'padding-right': '20px',
            display: 'flex',
            'flex-direction': hasPermission([RoleEnum.IntelligentMonitorViewSetting])
              ? 'row'
              : 'row-reverse',
            'justify-content': 'space-between',
            'vertical-align': 'middle',
          }"
        >
          <div
            style="align-self: center"
            v-if="hasPermission([RoleEnum.IntelligentMonitorViewSetting])"
          >
            <a-button
              preIcon="ant-design:setting-twotone"
              type="default"
              @click="settingsButtonOnClick"
            >
              设置
            </a-button>
          </div>
          <div class="flex flex-row justify-end">
            <a-pagination
              v-model:current="pageNum"
              v-model:page-size="pageSize"
              :show-total="(total) => `共 ${total} 条`"
              :total="selectedTreeItems.length"
              :showSizeChanger="false"
              style="display: inline-block"
            />
            <div class="flex flex-row justify-around gap-x-1">
              <Button @click="pageSize = 1">
                <template #icon>
                  <SvgIcon name="square" />
                </template>
              </Button>
              <Button @click="pageSize = 4">
                <template #icon>
                  <SvgIcon name="grid-view" />
                </template>
              </Button>
              <Button @click="pageSize = 9">
                <template #icon>
                  <SvgIcon name="grid-on-sharp" />
                </template>
              </Button>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
    <a-modal
      v-model:visible="settingModalVisible"
      :maskClosable="false"
      title="设置"
      v-show="hasPermission([RoleEnum.IntelligentMonitorViewSetting])"
    >
      <a-form
        :label-col="{ span: 8 }"
        :model="settings"
        :wrapper-col="{ span: 16 }"
        autocomplete="off"
        name="basic"
      >
        <a-form-item label="显示筛选列表">
          <a-switch v-model:checked="settings.treeShow" />
        </a-form-item>
        <a-form-item label="显示报警列表">
          <a-switch v-model:checked="settings.alertTableShow" />
        </a-form-item>
        <!--        <a-form-item label="显示算法推理流">-->
        <!--          <a-tooltip>-->
        <!--            <template #title>显示算法推理标注后的结果<br />而非摄像头原始视频流</template>-->
        <!--            <a-switch v-model:checked="settings.modelStreamShow" />-->
        <!--          </a-tooltip>-->
        <!--        </a-form-item>-->
        <a-form-item label="播放器">
          <a-tooltip>
            <template #title>建议选择主播放器，除非播放异常</template>
            <a-radio-group v-model:value="settings.playerType" button-style="solid">
              <a-radio-button :value="PlayerTypeEnum.JESSIBUCA">主</a-radio-button>
              <a-radio-button :value="PlayerTypeEnum.LIVE_PLAYER">备选</a-radio-button>
            </a-radio-group>
          </a-tooltip>
        </a-form-item>
        <a-form-item v-if="settings.playerType === PlayerTypeEnum.JESSIBUCA" label="串流协议">
          <a-tooltip>
            <template #title>
              稳定模式：ws-flv协议，更加流畅；
              <br /><br />
              极低延迟模式：webrtc协议，延迟不超过500ms，多路显示容易卡顿，最多支持4路播放，仅限能直连视频服务器的无NAT内网环境使用
            </template>
            <a-radio-group v-model:value="settings.jessibuca.protocolType" button-style="solid">
              <a-radio-button :value="ProtocolTypeEnum.WS_FLV"> 稳定模式</a-radio-button>
              <a-radio-button :value="ProtocolTypeEnum.WEBRTC"> 极低延迟模式</a-radio-button>
            </a-radio-group>
          </a-tooltip>
        </a-form-item>

        <a-form-item
          v-if="
            settings.playerType === PlayerTypeEnum.JESSIBUCA &&
            settings.jessibuca.protocolType === ProtocolTypeEnum.WS_FLV
          "
          label="解码器"
        >
          <a-tooltip>
            <template #title>
              硬解：浏览器CPU占用率低，播放流畅，若浏览器不支持会自动降级为软解；
              <br /><br />
              软解：浏览器CPU占用率高，电脑配置不足会卡顿；理想情况延迟比硬解低100ms左右
            </template>
            <a-radio-group v-model:value="settings.jessibuca.decodeMode" button-style="solid">
              <a-radio-button :value="DecodeModeEnum.MSE">硬解</a-radio-button>
              <a-radio-button :value="DecodeModeEnum.WASM">软解</a-radio-button>
            </a-radio-group>
          </a-tooltip>
        </a-form-item>
        <a-form-item label="播放音频">
          <a-tooltip>
            <template #title> 关闭后不渲染音频，略微提升性能</template>
            <a-switch v-model:checked="settings.hasAudio" />
          </a-tooltip>
        </a-form-item>
        <a-form-item v-if="settings.playerType === PlayerTypeEnum.JESSIBUCA" label="开启离屏渲染">
          <a-tooltip>
            <template #title>
              实验性功能：
              <br />
              打开后可提升渲染能力，但在某些浏览器可能内存泄漏，开启需谨慎！
            </template>
            <a-switch v-model:checked="settings.jessibuca.offScreen" />
          </a-tooltip>
        </a-form-item>
        <a-form-item v-if="settings.playerType === PlayerTypeEnum.JESSIBUCA" label="调试模式">
          <a-tooltip>
            <template #title>
              开启后可在网页控制台看到调试输出，便于定位问题，一般建议关闭
            </template>
            <a-switch v-model:checked="settings.jessibuca.debug" />
          </a-tooltip>
        </a-form-item>
        <a-tooltip
          v-if="
            settings.playerType === PlayerTypeEnum.JESSIBUCA &&
            settings.jessibuca.protocolType === ProtocolTypeEnum.WS_FLV
          "
        >
          <template #title>
            调小可以降低延迟，但遇网络波动、浏览器卡顿时会卡顿；
            <br /><br />
            调大可以提高播放流畅性，但会提升延迟；
            <br /><br />
            内网环境建议设为0.2秒
          </template>
          <a-form-item label="最大缓冲时长">
            <a-slider
              v-model:value="settings.jessibuca.bufferTime"
              :marks="{ 0.2: '0.2', 2: '2' }"
              :max="2"
              :min="0.2"
              :step="0.1"
              :tip-formatter="(num) => `${num} 秒`"
              tooltipPlacement="bottom"
            />
          </a-form-item>
        </a-tooltip>
        <a-tooltip
          v-if="
            settings.playerType === PlayerTypeEnum.JESSIBUCA &&
            settings.jessibuca.protocolType === ProtocolTypeEnum.WS_FLV
          "
        >
          <template #title>
            调小可以降低延迟，但遇网络波动、浏览器卡顿时会卡顿；
            <br /><br />
            调大可以提高播放流畅性，但会提升延迟；
            <br /><br />
            内网环境建议设为1秒
          </template>
          <a-form-item label="缓冲延迟">
            <a-slider
              v-model:value="settings.jessibuca.bufferDelayTime"
              :marks="{ 0: '0', 1: '1', 2: '2' }"
              :max="2"
              :min="0"
              :step="0.1"
              :tip-formatter="(num) => `${num} 秒`"
              tooltipPlacement="bottom"
            />
          </a-form-item>
        </a-tooltip>
        <a-form-item label="内存使用情况">
          <a-tooltip>
            <template #title>
              已分配的堆体积 / 最大堆体积
              <br />
              适用于长期播放时内存泄漏情况监控
              <br /><br />
              如遇内存不断增长的情况，请尝试：
              <br /><br />
              1. 禁用所有浏览器插件（开启无痕模式可直接不加载插件）
              <br />
              2. 更新浏览器至最新版本
            </template>
            <a-tag color="cyan">{{ ramUsage }}</a-tag>
          </a-tooltip>
        </a-form-item>
      </a-form>

      <template #footer>
        <a-button key="submit" :loading="settingsUploading" type="primary" @click="settingsUpload">
          上传配置
        </a-button>
      </template>
    </a-modal>
  </div>
</template>

<script lang="ts" setup name="MonitorMonitorInfo">
  import AlertTable from '/@/views/mineai/monitor/monitor-info/AlertTable.vue';
  import Ellipsis from './Ellipsis.vue';
  import { columns as alertTableColumns } from './alert.data';
  import {
    Button,
    Col as ACol,
    Form as AForm,
    FormItem as AFormItem,
    Modal as AModal,
    Pagination as APagination,
    Radio as ARadio,
    Row as ARow,
    Slider as ASlider,
    Spin as ASpin,
    Switch as ASwitch,
    Tag as ATag,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import { TreeItem } from '/@/components/Tree';
  import { SvgIcon } from '/@/components/Icon';
  import {
    computed,
    ComputedRef,
    CSSProperties,
    onUnmounted,
    reactive,
    Ref,
    ref,
    watch,
  } from 'vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import MainPlayer from './MainPlayer.vue';
  import BackupPlayer from './BackupPlayer.vue';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useAlertStore } from '/@/store/modules/alert';
  import AButton from '/@/components/Button/src/BasicButton.vue';
  import type { DropEvent, TreeDataItem } from 'ant-design-vue/es/tree/Tree';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import { Tree } from 'ant-design-vue';

  const { hasPermission } = usePermission();

  const name = 'MonitorMonitorInfo_';

  const alertStore = useAlertStore();

  const ARadioGroup = ARadio.Group;
  const ARadioButton = ARadio.Button;

  const { createMessage } = useMessage();

  const settingModalVisible = ref(false);
  const treeLoading = ref(true);
  const pageSize = ref(9);
  const pageNum = ref(1);
  const checkedKeys = ref<string[]>([]);
  const expandedKeys = ref<string[]>([]);
  const settingsUploading = ref(false);
  const ramUsage = ref<string>('当前浏览器不支持');
  const hostUrl = location.host;

  onUnmounted(() => {
    setSettings();
  });

  enum ProtocolTypeEnum {
    WS_FLV = 'ws-flv',
    WEBRTC = 'webrtc',
  }

  enum PlayerTypeEnum {
    JESSIBUCA = 'Jessibuca',
    LIVE_PLAYER = 'LivePlayer',
  }

  enum DecodeModeEnum {
    MSE = 'mse',
    WASM = 'wasm',
  }

  //单个TreeItem的结构，从后端传输而来
  interface VideoSource {
    title: string;
    areaLabel: Array<any>;
    key: string;
    fullTitle: string;
    monitorPath: string;
    streamPath: string;
    monitorWebRTCUrl: string;
    streamWebRTCUrl: string;
    pushStream: boolean;
    useCustomInfo: boolean;
    monitorInfo: { scene: string; model: string; monitor: string };
  }

  interface VideoSourceFull extends VideoSource {
    fullMonitorStreamUrl: string;
    fullModelStreamUrl: string;
  }

  interface Settings {
    treeShow: boolean;
    alertTableShow: boolean;
    modelStreamShow: boolean;
    playerType: PlayerTypeEnum;
    hasAudio: boolean;
    jessibuca: {
      decodeMode: DecodeModeEnum;
      bufferTime: number;
      bufferDelayTime: number;
      protocolType: ProtocolTypeEnum;
      offScreen: boolean;
      debug: boolean;
    };
  }

  const settingsVersion = '1.0';
  const settings = reactive<Settings>({
    treeShow: true,
    alertTableShow: false,
    modelStreamShow: false,
    playerType: PlayerTypeEnum.JESSIBUCA,
    hasAudio: true,
    jessibuca: {
      decodeMode: DecodeModeEnum.MSE,
      bufferTime: 0.2,
      bufferDelayTime: 0,
      protocolType: ProtocolTypeEnum.WS_FLV,
      offScreen: false,
      debug: false,
    },
  });

  // 配置保存
  function setSettings() {
    localStorage.setItem(name + 'pageNum', JSON.stringify(pageNum.value));
    localStorage.setItem(name + 'pageSize', JSON.stringify(pageSize.value));
    localStorage.setItem(name + 'checkedKeys', JSON.stringify(checkedKeys.value));
    localStorage.setItem(name + 'settings', JSON.stringify(settings));
    localStorage.setItem(name + 'settingsVersion', JSON.stringify(settingsVersion));
  }

  // 用于记录树结构的接口
  interface TreeItem {
    key: string | number;
    children?: TreeItem[];
    [propName: string]: any;
  }

  // 用于遍历data并提取所有的key
  function flattenKeys(
    data: TreeItem[],
    parentKey?: string | number,
  ): { key: string | number; parentKey?: string | number }[] {
    let keys: { key: string | number; parentKey?: string | number }[] = [];
    data.forEach((item) => {
      // 将当前元素的key和父key添加到keys数组中
      keys.push({ key: item.key, parentKey });
      // 如果当前元素有children属性，递归调用flattenKeys函数并将结果添加到keys数组中
      if (item.children) {
        keys = keys.concat(flattenKeys(item.children, item.key));
      }
    });
    // 返回包含所有key和它们的父key的数组
    return keys;
  }

  // 递归函数，用于根据给定的key顺序对data进行排序
  function sortData(
    data: TreeItem[],
    keys: { key: string | number; parentKey?: string | number }[],
  ): TreeItem[] {
    // 获取data中所有元素的key
    const dataKeys = data.map((item) => item.key);
    // 过滤掉keys数组中不存在于data中的key
    const filteredKeys = keys.filter((keyObj) => dataKeys.includes(keyObj.key));
    // 获取data中不存在于keys数组中的key
    const extraKeys = dataKeys.filter(
      (key) => !filteredKeys.map((keyObj) => keyObj.key).includes(key),
    );
    // 将不存在于keys数组中的key添加到filteredKeys数组的末尾
    extraKeys.forEach((key) => {
      filteredKeys.push({ key, parentKey: data.find((item) => item.key === key)?.parentKey });
    });
    // 根据filteredKeys数组中的顺序对data进行排序
    data.sort(
      (a, b) =>
        filteredKeys.findIndex((keyObj) => keyObj.key === a.key) -
        filteredKeys.findIndex((keyObj) => keyObj.key === b.key),
    );
    // 如果元素有children属性，递归调用sortData函数对children进行排序
    data.forEach((item) => {
      if (item.children) {
        // 获取对应子列表的keys子数组
        const childKeys = keys.filter((keyObj) => keyObj.parentKey === item.key);
        item.children = sortData(item.children, childKeys);
      }
    });
    // 返回排序后的data
    return data;
  }

  // 拖动逻辑
  const onDrop = (info: DropEvent) => {
    // !dropToGap 代表拖到内容之上 dropToGap 代表拖到内容缝隙之间
    // 禁止拖到内容之上
    if (!info.dropToGap) return;
    // 禁止跨级拖动
    if (info.node.pos.split('-').length !== info.dragNode.pos.split('-').length) return;
    const dropKey = info.node.eventKey;
    const dragKey = info.dragNode.eventKey;
    const dropPos = info.node.pos.split('-');
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1]);
    // 禁止跨越父节点拖动
    const nodePrefix = info.node.pos.split('-').slice(0, -1).join('-');
    const dragNodePrefix = info.dragNode.pos.split('-').slice(0, -1).join('-');
    if (nodePrefix !== dragNodePrefix) return;

    // loop:找到tree中key位置的元素,然后执行callback
    const loop = (data: TreeDataItem[], key: string | number, callback: any) => {
      data.forEach((item, index) => {
        if (item.key === key) {
          return callback(item, index, data);
        }
        if (item.children) {
          return loop(item.children, key, callback);
        }
      });
    };
    // 不破坏原有treeData
    const data = [...treeData.value];

    let dragObj: TreeDataItem;
    // 首先递归地找到要移动的obj，在tree中移除该obj
    loop(data, dragKey, (item: TreeDataItem, index: number, arr: TreeDataItem[]) => {
      // splice两个参数，移除操作
      arr.splice(index, 1);
      dragObj = item;
    });
    let ar: TreeDataItem = [];
    let i = 0;
    // 找到obj要放置的位置index
    loop(data, dropKey, (_item: TreeDataItem, index: number, arr: TreeDataItem[]) => {
      ar = arr;
      i = index;
    });
    // dropPosition为-1说明拖动到了最顶上
    if (dropPosition === -1) {
      ar.splice(i, 0, dragObj);
    } else {
      ar.splice(i + 1, 0, dragObj);
    }
    treeData.value = data;
    //将列表信息存储到localStorage中
    const keys = flattenKeys(data);
    localStorage.setItem('MonitorMonitorTreeDataKeys', JSON.stringify(keys));
  };
  // 拖动开始时收起所有子树
  const onDragStart = () => {
    //expandedKeys.value = [];
  };
  //存储 场景.children -> 算法模型.children -> 监控设备 三级
  const treeData: Ref<TreeItem[]> = ref([]);
  //从localSrtorage中读取treeData

  //flatten，只存储监控设备这一级
  const treeItemList: ComputedRef<VideoSource[]> = computed(() => {
    const t = treeData.value
      .map((scene) => {
        return scene.children!.map((model) => {
          return model.children!.map((monitor) => {
            return monitor;
          });
        });
      })
      .flat(3) as VideoSource[];
    return t.sort((a, b) => a.key.localeCompare(b.key)); //按照key排序，保证稳定
  });

  const initData = async () => {
    const rawTreeData: TreeItem[] = await maHttp.get(
      {
        url: 'monitor/getMonitorTree',
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MONITOR_ACCESSOR },
    );

    // 如果localStorage中存在配置，则从中加载配置
    if (
      localStorage.getItem(name + 'checkedKeys') !== 'null' &&
      localStorage.getItem(name + 'pageSize') !== 'null'
    ) {
      pageSize.value = JSON.parse(localStorage.getItem(name + 'pageSize') as string);
      pageNum.value = JSON.parse(localStorage.getItem(name + 'pageNum') as string);
      checkedKeys.value = JSON.parse(localStorage.getItem(name + 'checkedKeys') as string);
      Object.assign(settings, JSON.parse(localStorage.getItem(name + 'settings') as string));
    }

    //两个接口的数据在同一个tick更新，避免触发错误的watch
    //按照存储的keys的顺序来对列表进行排序
    const storedTreeDataKeys = localStorage.getItem('MonitorMonitorTreeDataKeys');
    if (storedTreeDataKeys) {
      const keys = JSON.parse(storedTreeDataKeys);
      treeData.value = sortData(rawTreeData, keys);
    } else {
      treeData.value = rawTreeData;
    }
    treeLoading.value = false;
  };
  initData();

  // checkedKeys发生变化时，上传配置
  watch(checkedKeys, async () => {
    setSettings();
  });

  //九宫格布局发生变化时，上传配置
  watch(pageSize, async () => {
    pageNum.value = 1;
    setSettings();
  });

  const getCardStyle = computed((): CSSProperties => {
    const lineNum = Math.sqrt(pageSize.value);
    return {
      height: `${99 / lineNum}%`,
      width: `${99 / lineNum}%`,
      // backgroundColor: 'black',
      borderWidth: '5px',
      borderColor: 'transparent',
    };
  });

  const selectedTreeItems: ComputedRef<VideoSource[]> = computed(() => {
    // 如果有选中，显示选中的部分
    if (checkedKeys.value.length > 0) {
      return treeItemList.value.filter((monitor) =>
        checkedKeys.value.includes(monitor.key as never),
      );
    }
    // 否则什么都不显示
    return [] as VideoSource[];
  });

  // 未显示的视频流个数
  const videoNotShownNum: ComputedRef<number> = computed(() => {
    return pageSize.value - treeItemsThisPage.value.length;
  });

  watch(selectedTreeItems, (s) => {
    // 触发setAlertItems
    alertStore.setAlertItems(s);
    //调整页码，保证不超出最大页数，不低于第1页
    if (Math.ceil(s.length / pageSize.value) < pageNum.value) {
      pageNum.value = Math.ceil(s.length / pageSize.value);
    }
    if (s.length > 0 && pageNum.value < 1) {
      pageNum.value = 1;
    }
  });

  const treeItemsThisPage: ComputedRef<VideoSourceFull[]> = computed(() => {
    //可以假设pageNum已经是合理，不超过max page的
    const beginIdx = (pageNum.value - 1) * pageSize.value;
    const endIdx = pageNum.value * pageSize.value;
    //slice时end超过数组长度不会报错
    //计算并添加了fullUrl字段
    return selectedTreeItems.value.slice(beginIdx, endIdx).map((v: VideoSource) => {
      return { ...v, ...getFullUrl(v) };
    });
  });

  //将后端传来的数据分情况转为全URL
  const getFullUrl: (VideoSource) => {
    fullwMonitorStreamUrl: string;
    fullModelStreamUrl: string;
  } = (videoSource: VideoSource) => {
    switch (settings.playerType) {
      case PlayerTypeEnum.LIVE_PLAYER:
        //LivePlayer仅播放ws-flv
        return {
          fullMonitorStreamUrl:
            videoSource.monitorPath != null && videoSource.monitorPath != ''
              ? `ws://${hostUrl}${videoSource.monitorPath}`
              : '',
          fullModelStreamUrl:
            videoSource.streamPath != null && videoSource.streamPath != ''
              ? `ws://${hostUrl}${videoSource.streamPath}`
              : '',
        };

      case PlayerTypeEnum.JESSIBUCA:
        //Jessibuca根据设置播放ws-flv或webrtc
        switch (settings.jessibuca.protocolType) {
          case ProtocolTypeEnum.WS_FLV:
            //播放ws-flv
            return {
              fullMonitorStreamUrl:
                videoSource.monitorPath != null && videoSource.monitorPath != ''
                  ? `ws://${hostUrl}${videoSource.monitorPath}`
                  : '',
              fullModelStreamUrl:
                videoSource.streamPath != null && videoSource.streamPath != ''
                  ? `ws://${hostUrl}${videoSource.streamPath}`
                  : '',
            };
          case ProtocolTypeEnum.WEBRTC:
            //播放webrtc
            return {
              fullMonitorStreamUrl:
                videoSource.monitorWebRTCUrl != null && videoSource.monitorWebRTCUrl != ''
                  ? `ws://${hostUrl}${videoSource.monitorWebRTCUrl}`
                  : '',
              fullModelStreamUrl:
                videoSource.streamWebRTCUrl != null && videoSource.streamWebRTCUrl != ''
                  ? `ws://${hostUrl}${videoSource.streamWebRTCUrl}`
                  : '',
            };
        }
    }
  };

  const settingsButtonOnClick = () => {
    settingModalVisible.value = true;
    try {
      //@ts-ignore
      const ram = performance.memory;
      ramUsage.value = `${(ram.totalJSHeapSize / 1024 / 1024).toFixed(2)} / ${(
        ram.jsHeapSizeLimit /
        1024 /
        1024
      ).toFixed(2)} MB`;
    } catch (e) {
      console.error(e);
    }
  };

  const settingsUpload = async () => {
    settingsUploading.value = true;
    setSettings();
    createMessage.success('配置保存成功');
    settingsUploading.value = false;
  };
</script>
<style lang="less" scoped>
  .ant-col {
    width: unset;
  }

  .bp {
    background-image: url(../../../../assets/images/bigScreenBackground.png);
    width: 100%;
    height: 100%;
    background-size: 100% 100%;
  }

  .tree-select :deep(.vben-basic-tree) {
    background-color: transparent !important;
  }
</style>
<style>
  video {
    height: 100%;
  }

  .ant-modal-body {
    margin: 20px !important;
  }

  body {
    background-repeat: repeat;
    background-image: url(../../../../assets/images/bigScreenBackground.png);
    background-size: 100% 100%;
  }

  object {
    display: none !important;
  }
</style>
