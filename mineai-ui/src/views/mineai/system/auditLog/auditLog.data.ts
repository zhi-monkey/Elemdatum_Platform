import { BasicColumn, FormSchema } from '/@/components/Table';
import { formatToDateTime } from '/@/utils/dateUtil';
import { h } from 'vue';
import { Tag } from 'ant-design-vue';

export const auditDescriptionMap: Record<string, string> = {
  dataset_add: '新增数据集',
  dataset_update: '修改数据集',
  dataset_delete: '删除数据集',
  dataset_download: '导出数据集版本',
  dataset_delete_minio_directory: '删除MinIO目录',
  dataset_group_add: '新增数据集组',
  dataset_group_update: '修改数据集组',
  dataset_group_delete: '删除数据集组',
  label_add: '新增标签',
  label_update: '修改标签库',
  label_delete: '删除标签',
  data_team_add: '新增多人标注团队',
  data_team_update: '修改多人标注团队',
  data_team_delete: '删除多人标注团队',
  data_team_task_add: '新增多人标注任务',
  data_team_task_update: '修改多人标注任务',
  data_team_task_delete: '删除多人标注任务',
  data_repo_add: '新增数据仓库',
  data_repo_update: '修改数据仓库',
  data_repo_delete: '删除数据仓库',
  model_add: '新增基础算法库',
  model_update: '修改基础算法库',
  model_delete: '删除基础算法库',
  model_generation_add: '新增训练任务',
  model_generation_update: '修改训练任务',
  model_generation_delete: '删除训练任务',
  standard_model_generation_update: '修改标准式训练任务',
  guided_model_generation_update: '修改引导式训练任务',
  standard_model_generation_delete: '删除标准化训练任务',
  guided_model_generation_delete: '删除引导式训练任务',
  model_version_add: '新增镜像',
  model_version_update: '修改镜像',
  model_version_delete: '删除镜像',
  model_weight_download: '下载模型权重',
  self_iteration_add: '新增自迭代训练任务',
  self_iteration_update: '修改自迭代训练任务',
  self_iteration_delete: '删除自迭代训练任务',
  model_application_add: '新增应用任务',
  model_application_update: '修改应用任务',
  model_application_delete: '删除应用任务',
  package_download: '下载应用包',
  chip_add: '新增算力芯片',
  chip_update: '修改算力芯片',
  chip_delete: '删除算力芯片',
  device_add: '新增设备固件',
  device_update: '修改设备固件',
  device_delete: '删除设备固件',
  scene_add: '新增场景名称',
  scene_update: '修改场景名称',
  scene_delete: '删除场景名称',
  application_name_add: '新增应用名称',
  application_name_update: '修改应用名称',
  application_name_delete: '删除应用名称',
  user_add: '新增用户',
  user_update: '修改用户',
  user_delete: '删除用户',
  department_add: '新增部门',
  department_update: '修改部门',
  department_binding_update: '修改部门用户绑定',
  department_delete: '删除部门',
};

export const getAuditDescriptionText = (description?: string) => {
  if (!description) {
    return '-';
  }
  return auditDescriptionMap[description] || description;
};

export const isDatasetDeleteAudit = (description?: string) => description === 'dataset_delete';

const operationTypeMap = {
  0: '新增',
  1: '删除',
  2: '修改',
  4: '下载',
};

const operationTypeOptions = [
  { label: '新增', value: 0 },
  { label: '删除', value: 1 },
  { label: '修改', value: 2 },
  { label: '下载', value: 4 },
];

const statusOptions = [
  { label: '异常', value: 1 },
  { label: '正常', value: 0 },
];

const statusMap = {
  0: { text: '正常', color: 'green' },
  1: { text: '异常', color: 'red' },
};

// 获取状态文本和颜色
export const getStatusInfo = (status: number) => {
  return statusMap[status] || { text: `未知状态(${status})`, color: 'default' };
};

// 获取操作类型文本
export const getOperationTypeText = (type: number) => {
  return operationTypeMap[type] || `未知类型(${type})`;
};

export const getStatusText = (status: number) => {
  return statusMap[status]?.text || `未知状态(${status})`;
};

export const columns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '操作时间',
    dataIndex: 'createDate',
    width: 160,
    customRender: ({ text }) => {
      return formatToDateTime(text);
    },
  },
  {
    title: '操作人',
    dataIndex: 'uname',
    width: 120,
  },
  {
    title: 'IP地址',
    dataIndex: 'ip',
    width: 120,
  },
  {
    title: '操作描述',
    dataIndex: 'description',
    width: 200,
    ellipsis: true,
    customRender: ({ text }) => getAuditDescriptionText(text),
  },
  {
    title: '状态',
    dataIndex: 'requestStatus',
    width: 100,
    customRender: ({ text }) => {
      const { text: statusText, color } = getStatusInfo(text);
      return h(Tag, { color }, () => statusText);
    },
  },
  {
    title: '操作类型',
    dataIndex: 'operationType',
    width: 120,
    customRender: ({ text }) => getOperationTypeText(text),
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'uname',
    label: '操作人',
    component: 'Input',
    colProps: { span: 6 },
    componentProps: {
      placeholder: '请输入操作人姓名',
    },
  },
  {
    field: 'operationType',
    label: '操作类型',
    component: 'Select',
    componentProps: {
      options: operationTypeOptions,
      placeholder: '请选择操作类型',
    },
    colProps: { span: 6 },
  },
  {
    field: 'requestStatus',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: statusOptions,
      placeholder: '请选择状态',
    },
    colProps: { span: 6 },
  },
  {
    field: 'timeRange',
    label: '操作时间',
    component: 'RangePicker',
    componentProps: {
      showTime: true,
      style: { width: '100%' },
      placeholder: ['开始时间', '结束时间'],
      format: 'YYYY-MM-DD HH:mm:ss',
    },
    colProps: { span: 12 },
  },
];

export const transformSearchParams = (params: Recordable) => {
  const searchParams: Recordable = { ...params };

  if (searchParams.timeRange) {
    searchParams.startTime = searchParams.timeRange[0];
    searchParams.endTime = searchParams.timeRange[1];
    delete searchParams.timeRange;
  }

  Object.keys(searchParams).forEach((key) => {
    if (searchParams[key] === '' || searchParams[key] === null || searchParams[key] === undefined) {
      delete searchParams[key];
    }
  });

  return searchParams;
};

// 导出弹窗表单配置
export const exportFormSchema: FormSchema[] = [
  {
    field: 'timeRange',
    label: '操作时间',
    component: 'RangePicker',
    required: true,
    componentProps: {
      showTime: true,
      style: { width: '100%' },
      placeholder: ['开始时间', '结束时间'],
      format: 'YYYY-MM-DD HH:mm:ss',
    },
    colProps: { span: 24 },
  },
  {
    field: 'uname',
    label: '操作人',
    component: 'Input',
    componentProps: {
      placeholder: '请输入操作人姓名',
    },
    colProps: { span: 24 },
  },
  {
    field: 'ip',
    label: 'IP地址',
    component: 'Input',
    componentProps: {
      placeholder: '请输入IP地址',
    },
    colProps: { span: 24 },
  },
  {
    field: 'description',
    label: '操作描述',
    component: 'Input',
    componentProps: {
      placeholder: '请输入操作描述关键词',
    },
    colProps: { span: 24 },
  },
  {
    field: 'requestStatus',
    label: '状态',
    component: 'Select',
    componentProps: {
      options: statusOptions,
      placeholder: '请选择状态',
    },
    colProps: { span: 24 },
  },
  {
    field: 'operationType',
    label: '操作类型',
    component: 'Select',
    componentProps: {
      options: operationTypeOptions,
      placeholder: '请选择操作类型',
      mode: 'multiple',
      allowClear: true,
      dropdownStyle: { maxHeight: '200px' },
      placement: 'bottomLeft',
    },
    colProps: { span: 24 },
  },
];
