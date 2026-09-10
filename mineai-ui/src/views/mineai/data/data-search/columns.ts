import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { getAllDatasets, getAllLabels } from './api';

// 元信息下拉选项（与上传表单保持一致）
const scenarioOptions = ['皮带跑偏', '异物检测', '人员行为', '车辆检测', '明火烟雾', '设备状态', '其他'];
const locationOptions = ['采煤工作面', '皮带巷', '运输大巷', '井口', '洗煤厂', '其他'];
const deviceOptions = ['采煤机', '掘进机', '皮带输送机', '矿车', '刮板机', '其他'];
const sourceTypeOptions = ['系统登记', '数据回流(RTSP)', 'HTTP相机', '数据仓库', '其他'];
const lightingOptions = ['正常光照', '低照度', '逆光', '夜间', '井下照明', '强光', '其他'];
const qualityOptions = ['清晰', '轻微模糊', '严重模糊', '过曝', '欠曝', '噪声', '其他'];

const toOptions = (arr: string[]) => arr.map((s) => ({ label: s, value: s }));

export const columns: BasicColumn[] = [
  {
    title: '图片编号',
    dataIndex: 'id',
    fixed: 'left',
    width: 75,
    sorter: true,
  },
  {
    title: '图片预览',
    dataIndex: 'url',
    width: 75,
    slots: { customRender: 'img' },
  },
  {
    title: '名称',
    dataIndex: 'name',
    ellipsis: true,
    width: 150,
  },
  {
    title: '所属数据集',
    dataIndex: 'datasetName',
    ellipsis: true,
    width: 140,
  },
  {
    title: '标注状态',
    dataIndex: 'annotationStatus',
    width: 90,
    customRender: ({ record }) => {
      const s = record.annotationStatus;
      let color = 'gray';
      let text = '未标注';
      if (s === 104) {
        color = 'green';
        text = '已标注';
      } else if (s === 102) {
        color = 'blue';
        text = '标注中';
      } else if (s === 103) {
        color = 'orange';
        text = '自动标注完成';
      }
      return h(Tag, { color }, () => text);
    },
  },
  {
    title: '标签',
    dataIndex: 'labels',
    width: 150,
    ellipsis: true,
    customRender: ({ text }) => h('span', text || '无标签'),
  },
  { title: '业务场景', dataIndex: 'scenario', width: 110, defaultHidden: true },
  { title: '采集地点', dataIndex: 'location', width: 120, defaultHidden: true },
  { title: '数据来源', dataIndex: 'sourceType', width: 110 },
  { title: '采集时间', dataIndex: 'captureTime', width: 150, defaultHidden: true },
  { title: '车辆/设备', dataIndex: 'device', width: 120, defaultHidden: true },
  { title: '设备/相机编号', dataIndex: 'deviceSn', width: 130, defaultHidden: true },
  { title: '光照/环境', dataIndex: 'lighting', width: 100, defaultHidden: true },
  { title: '数据质量', dataIndex: 'quality', width: 100, defaultHidden: true },
  { title: '宽度', dataIndex: 'width', width: 80, defaultHidden: true },
  { title: '高度', dataIndex: 'height', width: 80, defaultHidden: true },
  { title: '更新时间', dataIndex: 'updateTime', width: 150 },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'datasetIds',
    label: '数据集',
    component: 'ApiSelect',
    componentProps: {
      mode: 'multiple',
      api: () => getAllDatasets(),
      labelField: 'name',
      valueField: 'id',
      immediate: false,
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'name',
    label: '文件名',
    component: 'Input',
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'annotationStatus',
    label: '标注状态',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: [
        { label: '未标注', value: 101 },
        { label: '标注中', value: 102 },
        { label: '自动标注完成', value: 103 },
        { label: '已标注', value: 104 },
      ],
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'labelNames',
    label: '标签',
    component: 'ApiSelect',
    componentProps: {
      mode: 'multiple',
      api: () => getAllLabels(),
      labelField: 'name',
      valueField: 'name',
      immediate: false,
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'scenario',
    label: '业务场景',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: toOptions(scenarioOptions),
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'location',
    label: '采集地点',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: toOptions(locationOptions),
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'sourceType',
    label: '数据来源',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: toOptions(sourceTypeOptions),
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'device',
    label: '车辆/设备',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: toOptions(deviceOptions),
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'deviceSn',
    label: '设备/相机编号',
    component: 'Input',
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'lighting',
    label: '光照/环境',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: toOptions(lightingOptions),
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'quality',
    label: '数据质量',
    component: 'Select',
    componentProps: {
      mode: 'multiple',
      options: toOptions(qualityOptions),
    },
    colProps: { xl: 8, xxl: 6 },
  },
  {
    field: 'captureTime',
    label: '采集时间',
    component: 'RangePicker',
    componentProps: {
      showTime: true,
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
    },
    colProps: { xl: 12, xxl: 8 },
  },
  {
    field: 'updateTime',
    label: '更新时间',
    component: 'RangePicker',
    componentProps: {
      showTime: true,
      valueFormat: 'YYYY-MM-DD HH:mm:ss',
    },
    colProps: { xl: 12, xxl: 8 },
  },
];
