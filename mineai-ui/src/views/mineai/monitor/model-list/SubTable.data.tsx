import { h } from 'vue';
import { Tag, Popover } from 'ant-design-vue';

export default {
  component: { 'a-popover': Popover },
};
export const ModelVersionColumns = [
  // {
  //   title: '算法版本ID',
  //   dataIndex: 'id',
  //   key: 'id',
  //   align: 'center',
  //   width: 15,
  //   // sortDirections: ['descend', 'ascend'],
  //   // defaultSortOrder: 'descend',
  //   // sorter: (a: any, b: any) => a.id - b.id,
  //   // hidden: true,
  //   ifShow: false,
  // },
  // {
  //   title: '算法版本号',
  //   dataIndex: 'name',
  //   align: 'center',
  //   width: 20,
  // },
  { title: '算法版本号', dataIndex: 'showName', align: 'center', width: 20, ifShow: false },
  {
    title: '架构',
    dataIndex: 'architecture',
    width: 15,
    align: 'center',
    customRender: ({ record }) => {
      const architecture = record.architecture;
      let color = 'yellow';
      let text = 'amd64';
      if (architecture === 'arm64') {
        color = 'blue';
        text = 'arm64';
      }
      return h(Tag, { color: color }, () => text);
    },
  },
  {
    title: '支持操作',
    dataIndex: '',
    width: 20,
    align: 'center',
    customRender: ({ record }) => {
      const isTrainable = record.trainable;
      const isInspectable = record.inspectable;
      const isInferable = record.inferable;
      const tagList: any[] = [];
      if (isTrainable) tagList.push(h(Tag, { color: 'yellow' }, '训练'));
      if (isInspectable) tagList.push(h(Tag, { color: 'purple' }, '质检'));
      if (isInferable) tagList.push(h(Tag, { color: 'blue' }, '推理'));
      if (!(isTrainable || isInspectable || isInferable))
        tagList.push(h(Tag, { color: 'gray' }, '无支持操作'));
      return h('div', { class: ['flex', 'flex-row', 'justify-center', 'gap-x-1'] }, tagList);
    },
  },
  { title: '算法版本信息描述', dataIndex: 'description', align: 'center', width: 30 },
  {
    title: '训练情况',
    dataIndex: 'trainNum',
    align: 'center',
    customRender: ({ record }) => {
      let currentText;
      let currentContent;
      if (record.trainable) {
        currentText = record.trainNum;
        currentContent = '正在训练次数-训练成功次数-训练失败次数-训练取消次数-训练作业总数';
      } else {
        currentText = '不可训练';
        currentContent = '不可训练';
      }
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
    width: 20,
  },
  {
    title: '质检情况',
    dataIndex: 'inspectNum',
    align: 'center',
    customRender: ({ record }) => {
      let currentText;
      let currentContent;
      if (record.inspectable) {
        currentText = record.inspectNum;
        currentContent = '正在质检次数-质检成功次数-质检失败次数-质检取消次数-质检作业总数';
      } else {
        currentText = '不可质检';
        currentContent = '不可质检';
      }
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
    width: 20,
  },
  {
    title: '上传时间',
    dataIndex: 'createTime',
    key: 'createTime',
    align: 'center',
    width: 20,
    sortDirections: ['descend', 'ascend'],
    defaultSortOrder: 'descend',
    sorter: (a: any, b: any) => a.id - b.id,
  },
  {
    title: '操作',
    dataIndex: 'action',
    width: 30,
    align: 'center',
    slots: { customRender: 'action' },
  },
];
