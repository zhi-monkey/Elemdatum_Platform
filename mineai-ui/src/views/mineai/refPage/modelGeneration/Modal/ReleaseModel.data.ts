import { FormSchema } from '/@/components/Form';

export const publishFormSchema: FormSchema[] = [
  {
    field: 'scene',
    label: '发布到场景',
    component: 'Select',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      options: [
        { value: 'scene1', label: '场景1' },
        { value: 'scene2', label: '场景2' },
        { value: 'scene3', label: '场景3' },
        { value: 'scene4', label: '场景4' },
        { value: 'scene5', label: '场景5' },
      ],
      placeholder: '请选择场景名称',
    },
    required: true,
  },
  {
    field: 'application',
    label: '发布到应用',
    component: 'Select',
    componentProps: {
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      options: [
        { value: 'app1', label: '应用1' },
        { value: 'app2', label: '应用2' },
        { value: 'app3', label: '应用3' },
        { value: 'app4', label: '应用4' },
        { value: 'app5', label: '应用5' },
      ],
      placeholder: '请选择应用名称',
    },
    required: true,
  },
  {
    field: 'platform',
    label: '软硬件平台',
    component: 'Select',
    componentProps: {
      mode: 'multiple', // 允许多选
      dropdownAlign: {
        overflow: {
          adjustY: false, // 关闭下拉框垂直位置自适应
        },
      },
      options: [
        { value: 'rk1806', label: 'RK1806' },
        { value: 'rk3399pro', label: 'RK3399Pro' },
        { value: 'rv1126', label: 'RV1126' },
        { value: 'rv1109', label: 'RV1109' },
        { value: 'x86', label: 'x86' },
        { value: 'linux', label: 'Linux' },
        { value: 'android', label: 'Android' },
        { value: 'windows', label: 'Windows' },
        { value: 'rtos', label: 'RTOS' },
        { value: 'ubuntu', label: 'Ubuntu' },
      ],
      placeholder: '请选择软硬件平台',
    },
    required: true,
  },
];
