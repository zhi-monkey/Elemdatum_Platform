import { DescItem } from '/@/components/Description';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';
import { ref } from 'vue';

interface jobItem {
  value: string;
  label: string;
}

//echarts所需的seriesItem
interface seriesItem {
  name: string;
  smooth: boolean;
  data: number[];
  symbolSize: number;
  type?: 'line';
}

interface accItem {
  jobName: string;
  data: number;
}

//对比的job的列表
export const jobName = ref();
export const jobList = ref([] as jobItem[]);
//Loss的数据
export const newLossSeries = ref({} as seriesItem[]);
//Acc的数据
export const newAccSeries = ref({} as seriesItem[]);
export const currentJobMaxAcc = ref();

export const MaxAccList = ref({} as accItem[]);

export const schema: DescItem[] = [
  // {
  //   field: 'name',
  //   label: '作业名称',
  // },
  // {
  //   field: 'modelVersion.name',
  //   label: '作业镜像版本',
  // },

  {
    field: 'jobType',
    label: '作业类型',
    render: (record) => {
      let color;
      const text = record;
      let columnText;
      if (text === 1) {
        color = 'cyan';
        columnText = '模型训练';
      }
      if (text === 2) {
        color = 'purple';
        columnText = '模型质检';
      }
      if (text === 3) {
        color = 'blue';
        columnText = '模型转换';
      }
      return h(Tag, { color: color }, () => columnText);
    },
  },
  {
    field: 'status',
    label: '作业状态',
    render: (record) => {
      let color;
      const text = record;
      let columnText;
      if (text === 2) {
        color = 'cyan';
        columnText = '正在作业';
      }
      if (text === 1) {
        color = 'green';
        columnText = '作业成功';
      }
      if (text === -1) {
        color = 'red';
        columnText = '作业失败';
      }
      if (text === -2) {
        color = 'yellow';
        columnText = '作业取消';
      }
      return h(Tag, { color: color }, () => columnText);
    },
  },
  // {
  //   field: 'dataset.name',
  //   label: '数据集',
  // },
  {
    field: 'cpus',
    label: 'CPU数量',
    render: (record) => {
      const cpus = record;
      let fileSizeString = '';
      if (cpus === null) {
        fileSizeString = '未设置';
      } else if (cpus == '无限制') {
        fileSizeString = cpus;
      } else fileSizeString = cpus + '核';
      return h(Tag, () => fileSizeString);
    },
  },
  {
    field: 'gpus',
    label: 'GPU显存大小',
    render: (record) => {
      const gpus = record;
      let fileSizeString = '';
      if (gpus === null) {
        fileSizeString = '未设置';
      } else if (gpus == '无限制') {
        fileSizeString = '无限制';
      } else fileSizeString = gpus + 'GiB';
      return h(Tag, () => fileSizeString);
    },
  },
  {
    field: 'memory',
    label: '内存上限',
    render: (record) => {
      const memory = record;
      let fileSizeString = '';
      if (memory === null) {
        fileSizeString = '未设置';
      } else if (memory == '无限制') {
        fileSizeString = memory;
      } else fileSizeString = memory + 'B';
      return h(Tag, () => fileSizeString);
    },
  },
  {
    field: 'createTime',
    label: '开始时间',
  },
  {
    field: 'lastJobTime',
    label: '最后更新时间',
  },
  {
    field: 'deviceFirmWire',
    label: '转换对应设备固件信息',
  },
  {
    field: 'SoftPlatformInfo',
    label: '转换对应软件平台',
  },
  {
    field: 'HardPlatformInfo',
    label: '转换对应硬件平台',
  },
  // {
  //   field: 'logFilePath',
  //   label: '日志文件路径',
  // },
  // {
  //   field: 'description',
  //   label: '描述',
  // },
];

export const configSchema: DescItem[] = [];

export const columns = [
  { title: '超参名称', dataIndex: 'paramName', width: 20 },
  { title: '超参值', dataIndex: 'paramValue', width: 20 },
];
