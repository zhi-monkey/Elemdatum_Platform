import { BasicColumn, FormSchema } from '/@/components/Table';
import { h, reactive, ref } from 'vue';
import { Popover } from 'ant-design-vue';

/** 图片/视频标签列：仅 ID + 名称 */
const imageColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
    sorter: true,
  },
  {
    title: '名称',
    dataIndex: 'name',
    sorter: true,
    width: 150,
    customRender: ({ record }) => {
      const currentText = record.name;
      const currentContent = record.name;
      return h(
        Popover,
        {
          content: currentContent,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
];

/** 点云标签列：ID + 中文名称 + 标注名称 + 形状 */
const pointCloudColumns: BasicColumn[] = [
  {
    title: 'ID',
    dataIndex: 'id',
    fixed: 'left',
    width: 50,
    sorter: true,
  },
  {
    title: '中文名称',
    dataIndex: 'displayName',
    width: 180,
    customRender: ({ record }) => {
      const currentText = record.displayName || record.name || '-';
      return h(
        Popover,
        {
          content: currentText,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
  {
    title: '标注名称',
    dataIndex: 'annotationName',
    width: 200,
    customRender: ({ record }) => {
      const currentText = record.annotationName || record.name || '-';
      return h(
        Popover,
        {
          content: currentText,
          trigger: 'hover',
        },
        () => currentText,
      );
    },
  },
  {
    title: '形状',
    dataIndex: 'shape',
    width: 120,
    customRender: ({ record }) => {
      const shape = record.shape || 'RECT';
      return shape === 'RECT' ? '封闭矩形' : shape;
    },
  },
  {
    title: '颜色',
    dataIndex: 'color',
    width: 130,
    customRender: ({ record }) => {
      // 未配置颜色时按标注名称哈希生成默认色（与标注页一致），让列表直观展示每个类别最终显示的颜色
      const hex = record.color || hashLabelColor(record.annotationName || record.name || '');
      return h('div', { style: 'display:flex;align-items:center;gap:6px;' }, [
        h('span', { style: `display:inline-block;width:14px;height:14px;border-radius:3px;background:${hex};border:1px solid #2a3850;` }),
        h('span', { style: 'color:#cdd9e8;font-size:12px;' }, hex),
      ]);
    },
  },
];

/** 按标签名称哈希生成默认颜色（与点云标注页 getLabelColor 算法一致，供未配置颜色的类别展示兜底色） */
function hashLabelColor(label: string): string {
  let h1 = 0x811c9dc5;
  let h2 = 5381;
  for (let i = 0; i < label.length; i++) {
    const c = label.charCodeAt(i);
    h1 ^= c;
    h1 = Math.imul(h1, 0x01000193) >>> 0;
    h2 = ((h2 << 5) + h2 + c) >>> 0;
  }
  const hue = (h1 + h2 * 137.508) % 360;
  return hslToHex(hue, 70, 55);
}

function hslToHex(h: number, s: number, l: number): string {
  s /= 100;
  l /= 100;
  const a = s * Math.min(l, 1 - l);
  const f = (n: number) => {
    const k = (n + h / 30) % 12;
    return l - a * Math.max(-1, Math.min(k - 3, Math.min(9 - k, 1)));
  };
  const toHex = (x: number) => Math.round(255 * x).toString(16).padStart(2, '0');
  return `#${toHex(f(0))}${toHex(f(8))}${toHex(f(4))}`;
}

/** 视频标签列：与点云一致但去掉「形状」列（视频标注均为矩形） */
const videoColumns: BasicColumn[] = pointCloudColumns.filter((c) => c.dataIndex !== 'shape');

/** 按类型返回表格列：0=图片，1=点云，2=视频（视频字段与点云一致，仅去掉形状列） */
export function getColumns(type: number): BasicColumn[] {
  if (type === 0) return imageColumns;
  if (type === 2) return videoColumns;
  return pointCloudColumns;
}

export const searchFormSchema: FormSchema[] = [
  {
    // 自定义插槽字段：渲染"图片标签/点云标签"分类切换（列宽贴合内容，避免中间空隙）
    field: 'templateType',
    label: '',
    slot: 'templateType',
    component: 'Input',
    colProps: { span: 5, style: { display: 'flex', alignItems: 'center' } },
    show: true,
  },
  {
    field: 'name',
    label: '名称',
    component: 'Input',
    // 过滤器整体右对齐：marginLeft:auto 把 名称输入框 推至行尾；input 撑满列消除右侧空隙
    colProps: { span: 6, style: { marginLeft: 'auto' } },
    componentProps: ({ formActionType }) => ({
      style: { width: '100%' },
      allowClear: false,
      onChange: async (e) => {
        const { setFieldsValue } = formActionType;
        const filteredValue = e.target.value.replace(/\s+/g, '');
        e.target.value = filteredValue;
        await setFieldsValue({ name: filteredValue });
      },
    }),
  },
];

//记录label的index
export const labelIndex = ref(1);
//记录label的field
export const labelItemList = ref<string[]>(['label1']);

//记录每个label的颜色
export const defaultColor = '#409EFF';
export const labelsColor = reactive<{ [field: string]: string }>({
  label1: defaultColor,
});