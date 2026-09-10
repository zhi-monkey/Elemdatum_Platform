import { BasicColumn, FormSchema } from '/@/components/Table';
import { h } from 'vue';
import { Input, Progress, Tag, Tooltip } from 'ant-design-vue';
import { formatDateTime, toFixed } from '/@/utils';
import { useDataSetFormDataStore } from '/@/store/modules/formData';
import { ExclamationCircleOutlined } from '@ant-design/icons-vue';

const datasetFormDataStore = useDataSetFormDataStore();

function convertTimestampsToDateTimeStrings(timestamps) {
  return timestamps.map((timestamp) => {
    const date = new Date(parseInt(timestamp, 10));
    const year = date.getFullYear();
    const month = `0${date.getMonth() + 1}`.slice(-2);
    const day = `0${date.getDate()}`.slice(-2);
    const hours = `0${date.getHours()}`.slice(-2);
    const minutes = `0${date.getMinutes()}`.slice(-2);
    const seconds = `0${date.getSeconds()}`.slice(-2);
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  });
}

export const columns: BasicColumn[] = [
  {
    title: '数据集编号',
    dataIndex: 'datasetCode',
    fixed: 'left',
    width: 100,
    // sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'name',
    width: 150,
  },
  {
    title: '数据类型',
    dataIndex: 'dataType',
    width: 90,
    customRender: ({ record }) => {
      if (record.dataType === 5) {
        return h(Tag, { color: 'cyan' }, () => '点云');
      }
      if (record.dataType === 6) {
        return h(Tag, { color: 'blue' }, () => '视频');
      }
      if (record.dataType === 7) {
        return h(Tag, { color: 'purple' }, () => '多模态');
      }
      return h(Tag, { color: 'orange' }, () => '图片');
    },
  },
  {
    title: '标注类型',
    dataIndex: 'annotateType',
    width: 75,
    customRender: ({ record }) => {
      if (record.dataType === 5 || record.dataType === 6 || record.dataType === 7) {
        return '-';
      }
      // 102是目标检测，103是语义分割
      const annotateType = record.annotateType;
      let color = 'yellow';
      let text = '目标检测';
      if (annotateType === 103) {
        color = 'blue';
        text = '目标分割';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 75,
    align: 'center',
    customRender: ({ record }) => {
      if (record.dataType === 5) {
        const pointCloudStatus = {
          1001: '待上传',
          1002: '上传中',
          1003: '待标注',
          1004: '上传失败',
        }[record.status];
        const pointCloudColor =
          record.status === 1002 ? 'blue' : record.status === 1004 ? 'red' : 'gray';
        return h(Tag, { color: pointCloudColor }, () => pointCloudStatus || '未知');
      }
      if (record.dataType === 6) {
        const videoStatus = {
          2001: '待上传',
          2002: '上传中',
          2003: '已上传',
          2006: '上传失败',
        }[record.status];
        let videoColor = record.status === 2002 ? 'blue' : record.status === 2006 ? 'red' : 'gray';
        let videoText = videoStatus || '未知';
        // 已上传后，状态反映标注进度
        if (record.status === 2003 && record.annotateProgress) {
          const { total, done, annotating } = record.annotateProgress;
          if (total > 0 && done >= total) {
            videoText = '已标注';
            videoColor = 'green';
          } else if (annotating > 0 || done > 0) {
            videoText = '标注中';
            videoColor = 'yellow';
          } else {
            videoText = '未标注';
            videoColor = 'gray';
          }
        }
        return h(Tag, { color: videoColor }, () => videoText);
      }
      if (record.dataType === 7) {
        const multiStatus = {
          3001: '待导入',
          3002: '导入中',
          3003: '已就绪',
          3004: '导入失败',
        }[record.status];
        const multiColor =
          record.status === 3002 ? 'blue' : record.status === 3004 ? 'red' : 'gray';
        return h(Tag, { color: multiColor }, () => multiStatus || '未知');
      }
      //图片 101未标注 102 标注中 105已标注 302抽帧中 401 增强中
      const status = record.status;
      let color = 'gray';
      let text = '未标注';
      if (status === 102) {
        color = 'yellow';
        text = '标注中';
      } else if (status === 105) {
        color = 'green';
        text = '已标注';
      } else if (status === 302) {
        color = 'blue';
        text = '抽帧中';
      } else if (status === 401) {
        color = 'purple';
        text = '数据增强中';
      } else if (status === 403) {
        color = 'brown';
        text = '导入中';
      } else if (status === 103) {
        color = 'orange';
        text = '自动标注中';
      } else if (status === 104) {
        color = 'green';
        text = '自动标注完成';
      } else if (status === 1) {
        color = 'cyan';
        text = '上传中';
      }

      if (record.dataConversion === 4) {
        color = 'orange';
        text = '保存版本中';
      }
      if (record.isPublishing) {
        color = 'orange';
        text = '发布中';
      }

      // 当 hasChanges 为 true 且有版本号时，在状态旁边显示提示
      if (record.hasChanges && status === 105) {
        const warningIcon = h(
          Tooltip,
          {
            title: '数据集有变动，需要先保存版本再进行训练',
            placement: 'right',
          },
          {
            default: () =>
              h(ExclamationCircleOutlined, {
                style: {
                  color: '#faad14',
                  fontSize: '16px',
                  cursor: 'pointer',
                },
              }),
          },
        );

        // 使用左边占位让状态标签真正居中，警告图标在右侧
        return h(
          'div',
          {
            style: {
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            },
          },
          [
            // 左边的占位元素（宽度和警告图标+margin一样）
            h('span', { style: { width: '24px', display: 'inline-block' } }),
            // 状态标签
            h(Tag, { color: color }, () => text),
            // 警告图标
            h('span', { style: { marginLeft: '8px' } }, [warningIcon]),
          ],
        );
      }

      const statusTag = h(Tag, { color: color }, () => text);
      return statusTag;
    },
  },
  {
    title: '进度',
    dataIndex: 'progress',
    width: 100,
    customRender: ({ record }) => {
      // 视频数据集显示标注进度
      if (record.dataType === 6 && record.annotateProgress) {
        const { total, done } = record.annotateProgress;
        const percent = total > 0 ? Math.round((done / total) * 100) : 0;
        return h(Progress, { percent });
      }
      // 原有进度条逻辑
      const progressNode =
        record.progress === 0
          ? h(Progress, { percent: 0 })
          : h(Progress, { percent: toFixed(record.progress, 2, 0) });

      // 新增剩余时间小字
      const remainTimeNode = h(
        'div',
        {
          style: {
            fontSize: '12px',
            color: '#999',
            marginTop: '4px',
            textAlign: 'center',
          },
        },
        record.remainTime === -1
          ? '剩余时间：--'
          : record.remainTime
          ? `剩余时间：${record.remainTime}s`
          : '',
      );

      // 返回数组，进度条在上，小字在下
      return [progressNode, remainTimeNode];
    },
  },
  {
    title: '当前版本',
    dataIndex: 'currentVersionName',
    width: 100,
    ifShow: false,
  },
  {
    title: '更新时间',
    dataIndex: 'updateTime',
    width: 125,
    customRender: ({ record }) => {
      return formatDateTime(record.updateTime);
    },
    // 由于操作列表过长暂时隐藏
    ifShow: false,
  },
  {
    title: '类别',
    dataIndex: 'isGuided',
    width: 75,
    customRender: ({ record }) => {
      return record.isGuided ? '引导式训练数据集' : '标准数据集';
    },
    ellipsis: true,
  },
  {
    title: '描述',
    dataIndex: 'remark',
    width: 75,
    ifShow: false,
  },
  {
    title: '所属组',
    dataIndex: 'groups',
    width: 120,
    customRender: ({ record }) => {
      if (!record.groups || record.groups.length === 0) {
        return '暂无分组';
      }
      return record.groups.map((group) => {
        return h(Tag, { color: 'blue' }, () => group.name);
      });
    },
    ifShow: false,
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
    render: ({ model, field }) => {
      return h(Input, {
        placeholder: '请输入',
        value: model[field],
        onChange: (e: ChangeEvent) => {
          const filteredValue = e.target.value.replace(/\s+/g, '');
          model[field] = filteredValue;
          datasetFormDataStore.setCurrentName(filteredValue);
        },
      });
    },
  },
  {
    field: 'annotateType',
    label: '标注类型',
    component: 'Select',
    componentProps: {
      options: [
        { label: '目标检测', value: 102 },
        { label: '目标分割', value: 103 },
      ],
      onChange: (e) => {
        datasetFormDataStore.setCurrentAnnotateType(e);
      },
    },
    colProps: { xl: 12, xxl: 4 },
  },
  {
    field: 'createTime',
    component: 'RangePicker',
    componentProps: {
      valueFormat: 'x',
      showTime: {
        format: 'YYYY-MM-DD HH:mm:ss',
        showTime: true,
      },
      onChange: (e) => {
        const formattedDateTimes = convertTimestampsToDateTimeStrings(e);
        datasetFormDataStore.setCurrentCreateTime(formattedDateTimes);
      },
    },
    label: '创建时间',
    colProps: {
      xl: 12,
      xxl: 8,
    },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: '编号',
    component: 'Input',
    colProps: { span: 8 },
    show: false,
  },
  {
    field: 'name',
    label: '名称',
    required: true,
    component: 'Input',
    rules: [
      {
        required: true,
        pattern: new RegExp(/^[^\\\/]*$/, 'g'),
        message: '不允许数据集名称为空',
      },
    ],
  },
  {
    field: 'dataTypeTest',
    label: '数据类型',
    required: true,
    component: 'RadioButtonGroup',
    componentProps: {
      options: [
        { label: '图片', value: 201 },
        { label: '点云', value: 5 },
        { label: '视频', value: 6 },
        { label: '多模态', value: 7 },
      ],
    },
    defaultValue: 201,
    // dynamicDisabled: ({ values }) => {
    //   return values.id !== undefined;
    // },
  },
  {
    field: 'annotateType',
    label: '标注类型',
    required: true,
    component: 'RadioButtonGroup',
    componentProps: {
      options: [
        { label: '目标检测', value: 102 },
        { label: '目标分割', value: 103 },
      ],
    },
    defaultValue: 102,
    dynamicDisabled: ({ values }) => {
      return values.id !== undefined;
    },
    ifShow: ({ values }) =>
      values.dataTypeTest !== 5 && values.dataTypeTest !== 6 && values.dataTypeTest !== 7,
  },
  // 标签组相关功能隐藏
  { field: 'labelGroupId', label: '标签组', component: 'Input', slot: 'add', ifShow: false },
  {
    field: 'remark',
    label: '描述',
    component: 'Input',
  },
  {
    field: 'dataType',
    label: '',
    component: 'Input',
    defaultValue: 0,
    show: false,
  },
  {
    field: 'templateType',
    label: '',
    component: 'Input',
    defaultValue: null,
    show: false,
  },
  {
    field: 'module',
    label: '',
    component: 'Input',
    defaultValue: 0,
    show: false,
  },
  {
    field: 'type',
    label: '',
    component: 'Input',
    defaultValue: 0,
    show: false,
  },
  {
    field: 'import',
    label: '',
    component: 'Checkbox',
    defaultValue: false,
    show: false,
  },
  {
    field: 'isPublic',
    label: '',
    component: 'Input',
    defaultValue: 0,
    show: false,
  },
  {
    field: 'isGuided',
    label: '',
    component: 'Input',
    defaultValue: 0,
    show: false,
  },
];
