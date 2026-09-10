import { DescItem } from '/@/components/Description';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';

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
export const newLossSeries = ref<seriesItem[]>([]); // 正确初始化
//Acc的数据
export const newAccSeries = ref<seriesItem[]>([]);
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
      if (text === 11) {
        color = 'cyan';
        columnText = '排队中';
      }
      if (text === 12) {
        color = 'cyan';
        columnText = '切分中';
      }
      if (text === 1) {
        color = 'cyan';
        columnText = '训练中';
      }
      //训练成功或者子任务转换中都属于训练成功了
      if (text === 2 || text === 5) {
        color = 'green';
        columnText = '训练成功';
      }
      if (text === -2) {
        color = 'red';
        columnText = '训练失败';
      }
      if (text === -10) {
        color = 'yellow';
        columnText = '作业取消';
      }
      if (text === 7) {
        color = 'blue';
        columnText = '已发布';
      }
      return h(Tag, { color: color }, () => columnText);
    },
  },
  // {
  //   field: 'dataset.name',
  //   label: '数据集',
  // },
  {
    field: 'resourceConfig',
    label: '资源配置',
    render: (_, record) => {
      const formatCpus = (cpus) => {
        if (cpus === null) return '未设置';
        if (cpus == '无限制') return cpus;
        return cpus + '核';
      };

      const formatGpuCount = (gpuCount) => {
        if (gpuCount === null) return '未设置';
        if (gpuCount == '无限制') return '无限制';
        return gpuCount + '张';
      };

      const formatGpuMemory = (gpuMemory) => {
        if (gpuMemory === null) return '未设置';
        if (gpuMemory == '无限制') return '无限制';
        return gpuMemory;
      };

      const formatMemory = (memory) => {
        if (memory === null) return '未设置';
        if (memory == '无限制') return memory;
        return memory + 'B';
      };

      const cpuText = `CPU数量${formatCpus(record.cpus)}`;
      const gpuCountText = `GPU数量${formatGpuCount(record.gpuCount)}`;
      const gpuText = `GPU显存大小${formatGpuMemory(record.gpuMemory)}`;
      const memoryText = `内存上限${formatMemory(record.memory)}`;

      // 方案1：用Tag显示（推荐）
      return h('span', [
        h(Tag, { style: { marginRight: '8px' } }, () => cpuText),
        h(Tag, { style: { marginRight: '8px' } }, () => gpuCountText),
        h(Tag, { style: { marginRight: '8px' } }, () => gpuText),
        h(Tag, () => memoryText),
      ]);
    },
  },
  {
    field: 'trainDatasetVersion',
    label: '训练数据集',
    render: (_, record) => {
      return record.trainDatasetVersionName;
    },
  },

  {
    field: 'createTime',
    label: '开始时间',
  },
  // {
  //   field: 'logFilePath',
  //   label: '日志文件路径',
  // },
  // {
  //   field: 'description',
  //   label: '描述',
  // },

  {
    field: 'defaultPercent',
    label: '作业描述',
    render: (_, record) => {
      if (record.status === -2) {
        return '容器异常退出';
      }
      if (record.status === -10) {
        return '作业被取消';
      } else if (record.status === 2 || record.status === 7) {
        if (record.defaultPercent === 100) return '作业正常结束';
        if (record.defaultPercent < 100) return '精度无提高，提前结束';
      }
      return '尚未结束';
    },
  },
];

export const configSchema: DescItem[] = [];

export const columns = [
  { title: '超参名称', dataIndex: 'paramName', width: 20 },
  { title: '超参值', dataIndex: 'paramValue', width: 20 },
];
