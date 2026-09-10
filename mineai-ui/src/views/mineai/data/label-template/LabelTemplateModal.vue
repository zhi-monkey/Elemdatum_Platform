<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    showFooter
    :title="getTitle"
    width="500px"
    @ok="handleSubmit"
    @cancel="clearForm"
    ref="modalRef"
  >
    <div class="modal-content">
      <BasicForm @register="registerForm" class="myForm">
        <!-- 类别颜色：自定义插槽渲染 el-color-picker（vben 表单组件表无 ColorPicker，element-plus 已全局注册） -->
        <template #color="{ color }">
          <el-color-picker
            :model-value="color"
            color-format="hex"
            :predefine="['#4fc3f7', '#8bdd3c', '#ffd666', '#ff7875', '#b388ff', '#42a5f5', '#ef5350', '#66bb6a']"
            @change="handleColorChange"
          />
          <span class="color-preview-hex">{{ color || '未选择' }}</span>
        </template>
      </BasicForm>
    </div>
  </BasicModal>
</template>

<script lang="ts">
  import { computed, defineComponent, ref } from 'vue';
  import { BasicForm, useForm } from '/@/components/Form/index';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

  export default defineComponent({
    name: 'NameModal',
    components: { BasicModal, BasicForm },
    emits: ['success', 'register'],
    setup(_, { emit }) {
      const isUpdate = ref(false);
      const labelTemplateId = ref<number>();
      /** 标签类型：0=图片，1=点云，2=视频 */
      const currentType = ref<number>(0);
      /** 是否点云/视频标签（字段一致） */
      const isSpecial = computed(() => currentType.value === 1 || currentType.value === 2);
      /** 接口前缀：0=图片 labelTemplate，1=点云 labelTemplatePointcloud，2=视频 labelTemplateVideo */
      function getUrlPrefix(type: number): string {
        if (type === 1) return 'labelTemplatePointcloud';
        if (type === 2) return 'labelTemplateVideo';
        return 'labelTemplate';
      }
      /** 用户是否已手动选择过颜色（true 后不再被标注名称输入的哈希默认色覆盖） */
      const colorTouched = ref(false);

      // 初始化表单：图片标签仅 name；点云标签为 中文名称/标注名称/形状（封闭矩形）
      const formSchema = [
        {
          field: 'name',
          label: '名称',
          component: 'Input',
          rules: [
            { required: true, message: '名称不能为空' },
            { max: 30, message: '名称不能超过30个字符' },
            {
              validator: async (_: any, value: string) => {
                if (value && !/^[a-zA-Z0-9_-]+$/.test(value)) {
                  return Promise.reject('名称只能包含英文字母、数字、下划线和连字符');
                }
                return Promise.resolve();
              },
            },
          ],
          componentProps: { readonly: false },
        },
        {
          field: 'displayName',
          label: '中文名称',
          component: 'Input',
          rules: [
            { required: true, message: '中文名称不能为空' },
            { max: 30, message: '中文名称不能超过30个字符' },
          ],
          componentProps: { readonly: false },
        },
        {
          field: 'annotationName',
          label: '标注名称',
          component: 'Input',
          rules: [
            { required: true, message: '标注名称不能为空' },
            { max: 30, message: '标注名称不能超过30个字符' },
            {
              validator: async (_: any, value: string) => {
                if (value && !/^[a-zA-Z0-9_-]+$/.test(value)) {
                  return Promise.reject('标注名称只能包含英文字母、数字、下划线和连字符');
                }
                return Promise.resolve();
              },
            },
          ],
          // 输入标注名称时，若用户尚未手动选色，自动按名称哈希生成默认色（未选默认用哈希）
          componentProps: {
            readonly: false,
            onChange: (e: any) => {
              if (!colorTouched.value) {
                setFieldsValue({ color: getLabelColor(e?.target?.value || '') });
              }
            },
          },
        },
        {
          field: 'shape',
          label: '形状',
          component: 'Select',
          defaultValue: 'RECT',
          rules: [{ required: true, message: '请选择形状' }],
          componentProps: {
            options: [{ label: '封闭矩形', value: 'RECT' }],
            disabled: true, // 点云标签固定只能选封闭矩形
          },
        },
        {
          field: 'color',
          label: '类别颜色',
          component: 'Input', // 占位组件（componentMap 必须有映射），实际渲染走 #color 插槽的 el-color-picker
          slot: 'color',
          rules: [],
        },
      ];

      /**
       * 根据标注名称确定性生成默认颜色（与点云标注页哈希算法一致）：
       * 用户未选择颜色时，用该颜色作为默认，保证同一类别恒定同色、不同类别不同色。
       */
      function getLabelColor(label: string): string {
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

      const [registerForm, { resetFields, updateSchema, validate, setFieldsValue }] = useForm({
        labelWidth: 110,
        schemas: formSchema,
        showActionButtonGroup: false,
      });

      const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
        isUpdate.value = data.isUpdate ?? false;
        labelTemplateId.value = data.labelTemplateId;
        currentType.value = data.type ?? 0;

        // 根据当前类型，仅显示对应字段：点云标签显示形状（固定封闭矩形），
        // 视频标注均为矩形，隐藏形状选择；图片标签仅显示名称。
        const visibleFields = isSpecial.value
          ? currentType.value === 1
            ? ['displayName', 'annotationName', 'shape', 'color']
            : ['displayName', 'annotationName', 'color']
          : ['name'];
        await updateSchema(
          formSchema.map((s) => ({
            field: s.field,
            label: s.label,
            component: s.component,
            rules: s.rules,
            defaultValue: s.defaultValue,
            componentProps: s.componentProps,
            slot: s.slot, // 颜色字段走自定义插槽渲染，必须透传
            show: visibleFields.includes(s.field),
          })),
        );

        if (isUpdate.value && labelTemplateId.value) {
          // 编辑时获取数据填充表单（点云标签走独立表接口）
          const url = `${getUrlPrefix(currentType.value)}/slectById/${labelTemplateId.value}`;
          const res = await maHttp.get(
            { url },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
          await resetFields();
          // 已配置颜色视为用户已选定（后续标注名称输入不再覆盖）；未配置则用哈希默认色
          colorTouched.value = !!res.color;
          await setFieldsValue({
            id: res.id,
            name: res.name,
            displayName: res.displayName,
            annotationName: res.annotationName,
            shape: res.shape || 'RECT',
            // 已配置颜色用配置值；未配置则用哈希默认色（同一类别恒定同色）
            color: res.color || getLabelColor(res.annotationName || res.name || ''),
          });
        } else {
          await resetFields();
          colorTouched.value = false;
          // 新增点云标签时默认形状为封闭矩形（颜色随标注名称输入哈希生成，也可手动选择）
          if (isSpecial.value) {
            await setFieldsValue({ shape: 'RECT' });
          }
        }

        setModalProps({ confirmLoading: false });
      });

      const getTitle = computed(() => (isUpdate.value ? '编辑标签' : '新增标签'));

      async function handleSubmit() {
        try {
          const values = await validate();
          setModalProps({ confirmLoading: true });

          const payload = isSpecial.value
            ? {
                name: values.annotationName, // 点云/视频标签：name 复用标注名称（数据库唯一键约束）
                displayName: values.displayName,
                annotationName: values.annotationName,
                // 点云标签有形状字段（固定封闭矩形）；视频标注均为矩形，无形状字段，不传 shape
                ...(currentType.value === 1 ? { shape: values.shape || 'RECT' } : {}),
                // 用户未选颜色时，提交哈希默认色（保证同一类别恒定同色、不同类别不同色）
                color: values.color || getLabelColor(values.annotationName || ''),
              }
            : {
                type: 0,
                name: values.name,
              };

          if (isUpdate.value && labelTemplateId.value) {
            const url = `${getUrlPrefix(currentType.value)}/update/${labelTemplateId.value}`;
            await maHttp.put(
              {
                url,
                params: { id: labelTemplateId.value, ...payload },
              },
              { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
            );
          } else {
            const url = `${getUrlPrefix(currentType.value)}/create`;
            await maHttp.post(
              {
                url,
                params: payload,
              },
              { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
            );
          }

          closeModal();
          emit('success');
        } finally {
          setModalProps({ confirmLoading: false });
        }
      }

      async function clearForm() {
        await resetFields();
      }

      /** 颜色选择器变更：写回表单 color 字段（值为 hex 字符串或 {hex} 对象，统一取 hex） */
      function handleColorChange(v: string | { hex: string } | null) {
        const hex = typeof v === 'string' ? v : v?.hex || '';
        setFieldsValue({ color: hex });
        colorTouched.value = !!hex;
      }

      return {
        registerModal,
        registerForm,
        setFieldsValue,
        getTitle,
        handleSubmit,
        clearForm,
        handleColorChange,
      };
    },
  });
</script>

<style scoped>
  .modal-content {
    max-height: 400px;
    overflow-y: auto;
    padding: 10px;
  }
</style>