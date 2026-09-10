<template>
  <el-popover
    :key="state.dialogKey"
    :visible="state.visible"
    placement="top"
    width="300"
    title="添加标签"
    @show="onShow"
    @hide="onHide"
  >
    <el-form
      ref="formRef"
      :model="state.form"
      :rules="rules"
      label-width="60px"
      style="margin-top: 20px"
    >
      <!-- 标签名称：支持下拉选择或手动输入 -->
      <el-form-item label="名称" prop="name">
        <el-select
          v-model="state.form.name"
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          :filter-method="filterMethod"
          placeholder="选择或输入标签名称"
          :loading="state.loading"
          style="width: 100%"
          @change="onNameChange"
        >
          <el-option
            v-for="template in state.filteredTemplates"
            :key="template.id"
            :label="template.name"
            :value="template.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="颜色" prop="color">
        <el-color-picker v-model="state.form.color" />
      </el-form-item>

      <div class="w-full flex flex-row justify-end">
        <el-button size="small" @click="handleCancel">取消</el-button>
        <el-button size="small" type="primary" @click="handleOk">确定</el-button>
      </div>
    </el-form>
    <template #reference>
      <slot name="trigger">
        <a-button type="primary" size="small" class="add-label-btn" @click="() => (state.visible = true)">
          +
        </a-button>
      </slot>
    </template>
  </el-popover>
</template>

<script>
  import { nextTick, onMounted, reactive, ref } from 'vue';
  import { validateName } from '/@/utils/dubhe/validate';
  import { Button as AButton } from 'ant-design-vue';
  import { generateRandomHexColor, getAllLabelTemplate } from '/@/views/mineai/data/dataset-details2/api';

  export default {
    name: 'AddLabelUnified',
    components: { AButton },
    props: {
      getStyle: Function,
      title: String,
    },
    emits: ['handleCreate'],
    setup(props, ctx) {
      const inputRef = ref(null);
      const formRef = ref(null);

      const state = reactive({
        visible: false,
        dialogKey: 1,
        form: {
          name: '',
          color: generateRandomHexColor(),
        },
        labelTemplates: [],
        filteredTemplates: [], // 过滤后的标签列表
        loading: false,
      });

      // 获取标签库列表
      const fetchLabelTemplates = async () => {
        state.loading = true;
        try {
          const response = await getAllLabelTemplate();
          if (response && Array.isArray(response)) {
            state.labelTemplates = response.map((template) => ({
              id: template.id,
              name: template.name,
              color: template.color,
            }));
            state.filteredTemplates = [...state.labelTemplates];
          }
        } catch (error) {
          console.error('Error fetching label templates:', error);
        } finally {
          state.loading = false;
        }
      };

      // 自定义过滤方法：只显示标签库中匹配的选项，不显示用户输入的内容
      const filterMethod = (query) => {
        if (query) {
          state.filteredTemplates = state.labelTemplates.filter(
            (t) => t.name.toLowerCase().includes(query.toLowerCase()),
          );
        } else {
          state.filteredTemplates = [...state.labelTemplates];
        }
      };

      // 当选择或输入标签名称时，自动填充颜色（如果是从标签库选择的）
      const onNameChange = (name) => {
        const selected = state.labelTemplates.find((t) => t.name === name);
        if (selected && selected.color) {
          state.form.color = selected.color;
        } else {
          // 手动输入的新标签，生成随机颜色
          state.form.color = generateRandomHexColor();
        }
      };

      // 表单规则
      const rules = {
        name: [
          { required: true, message: '请输入或选择标签名称', trigger: ['change', 'blur'] },
          { validator: validateName, trigger: ['change', 'blur'] },
        ],
      };

      const resetForm = () => {
        Object.assign(state, {
          visible: false,
          form: {
            name: '',
            color: generateRandomHexColor(),
          },
        });
      };

      const handleCancel = () => {
        resetForm();
      };

      const handleOk = () => {
        formRef.value.validate().then((valid) => {
          if (!valid) return;

          ctx.emit('handleCreate', null, {
            name: state.form.name,
            color: state.form.color,
          });
          resetForm();
        });
      };

      const onShow = () => {
        nextTick(() => {
          // 弹窗显示时可以聚焦到选择框
        });
      };

      const onHide = () => {
        Object.assign(state, {
          dialogKey: state.dialogKey + 1,
        });
      };

      onMounted(() => {
        fetchLabelTemplates();
      });

      return {
        props,
        state,
        rules,
        formRef,
        handleOk,
        handleCancel,
        onShow,
        onHide,
        onNameChange,
        filterMethod,
      };
    },
  };
</script>

<style lang="scss" scoped>
  .add-label-btn {
    width: 100%;
    height: auto;
    padding: 6px 8px;
    border-radius: 4px;
    font-size: 13px;
    line-height: 1.4;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  </style>
