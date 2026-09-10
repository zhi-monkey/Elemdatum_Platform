import { BasicColumn } from '/@/components/Table';
import { DescItem } from '/@/components/Description';
import { h, ref } from 'vue';
import { Tag } from 'ant-design-vue';

interface deployItem {
  value: string;
  label: string;
}

export const deployList = ref([] as deployItem[]);

export const monitorColumn: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
    sorter: true,
  },
  {
    title: '监控设备名称',
    dataIndex: 'monitorName',
    width: 80,
  },
  {
    title: '监控设备序列号',
    dataIndex: 'name',
    width: 80,
  },

  {
    title: '监控设备状态',
    dataIndex: 'status',
    width: 80,
  },
];

export const schema: DescItem[] = [
  {
    field: 'id',
    label: '部署Id',
    contentMinWidth: 200,
  },
  {
    field: 'modelVersion.showName',
    label: '算法版本',
  },
  {
    field: 'monitor.name',
    label: '监控设备',
  },
  {
    field: 'controller.name',
    label: '控制器',
  },
  {
    field: 'weightPath',
    label: '权重文件',
  },
  {
    field: 'gpuNum',
    label: 'GPU使用数量',
    render: (record) => {
      const gpus = record;
      let fileSizeString = '';
      if (gpus == '无限制') {
        fileSizeString = '无限制';
      } else fileSizeString = gpus + 'GiB';
      return h(Tag, () => fileSizeString);
    },
  },
  {
    field: 'cpuNum',
    label: 'CPU使用数量',
    render: (record) => {
      const cpus = record;
      let fileSizeString = '';
      if (cpus == '无限制') {
        fileSizeString = cpus;
      } else fileSizeString = cpus + '核';
      return h(Tag, () => fileSizeString);
    },
  },
  {
    field: 'memoryNum',
    label: '内存使用数量',
    render: (record) => {
      const memory = record;
      let fileSizeString = '';
      if (memory == '无限制') {
        fileSizeString = memory;
      } else fileSizeString = memory + 'B';
      return h(Tag, () => fileSizeString);
    },
  },
  {
    field: 'description',
    label: '部署描述',
  },
  {
    field: 'deploymentName',
    label: '部署名字',
  },
  {
    field: 'createTime',
    label: '创造时间',
  },
  {
    field: 'status',
    label: '部署状态',
    render: (record) => {
      let color;
      const text = record;
      let columnText;
      if (text === 2) {
        color = 'cyan';
        columnText = '部署中';
      }
      if (text === 1) {
        color = 'green';
        columnText = '部署成功';
      }
      if (text === -1) {
        color = 'red';
        columnText = '部署失败';
      }
      if (text === -2) {
        color = 'yellow';
        columnText = '未部署';
      }
      return h(Tag, { color: color }, () => columnText);
    },
  },
];
