import { FormSchema } from '/@/components/Form';

export const formSchema: FormSchema[] = [
  {
    field: 'rejectReason',
    label: '拒绝发布的原因:',
    component: 'InputTextArea',
    labelWidth: 120,
  },
];
export const formSchema2: FormSchema[] = [
  {
    field: 'rejectReason',
    label: '拒绝发布的原因:',
    component: 'InputTextArea',
    componentProps: {
      disabled: true,
    },
    labelWidth: 120,
  },
];
