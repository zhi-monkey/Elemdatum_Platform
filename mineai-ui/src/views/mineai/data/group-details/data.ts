import { BasicColumn, FormSchema } from '/@/components/Table';
import { formatDateTime } from '/@/utils';
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
import { PermissionEnum } from '/@/enums/PermissionEnum';

export const columns: BasicColumn[] = [
  {
    title: '团队ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 150,
    sorter: true,
  },
  {
    title: '团队名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '团队人数',
    dataIndex: 'memberNum',
    width: 200,
  },
  {
    title: '团队描述',
    dataIndex: 'remark',
    width: 150,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 150,
    customRender: ({ record }) => {
      return formatDateTime(record.createTime);
    },
  },
];

export const searchFormSchema: FormSchema[] = [
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    colProps: { xl: 12, xxl: 4 },
  },
];

export const formSchema: FormSchema[] = [
  {
    field: 'id',
    label: 'id',
    component: 'Input',
    show: false,
  },
  {
    field: 'name',
    label: '团队名称',
    component: 'Input',
    required: true,
  },
  {
    field: 'remark',
    label: '团队描述',
    component: 'Input',
    required: true,
  },
  {
    field: 'userIds',
    label: '标注成员',
    component: 'ApiSelect',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: true, // 关闭下拉框垂直位置自适应
        },
      },
      // 将下拉框渲染到body上, 防止被modal遮罩
      getPopupContainer: () => document.body,
      dropdownStyle: {
        maxHeight: '300px',
        zIndex: 2000, // 确保层级高于 modal
      },
      dropdownMatchSelectWidth: false,
      mode: 'multiple',
      api: async () => {
        return await maHttp.get(
          {
            url: '/users/getUsersWithPermissionDoNotExcludeCurrentUser',
            headers: {
              // @ts-ignore
              ignoreCancelToken: true,
            },
            params: { permissionName: PermissionEnum.DATA_ACCEPT_TASK },
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
        );
      },
      labelField: 'username',
      valueField: 'id',
      immediate: true,
      filterOption: (inputValue, option) => {
        return option.label.includes(inputValue);
      },
    },
    required: true,
  },
];
