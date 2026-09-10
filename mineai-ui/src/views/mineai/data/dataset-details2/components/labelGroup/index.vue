<template>
  <el-popover
    :key="state.dialogKey"
    :visible="state.visible"
    placement="top"
    width="300"
    title="从标签库导入"
    @show="onShow"
    @hide="onHide"
  >
    <el-form
      ref="formRef"
      :model="state.form"
      :rules="rules"
      label-width="80px"
      style="margin-top: 20px"
    >
      <el-form-item label="标签库" prop="name">
        <el-select
          v-model="state.form.id"
          placeholder="请选择标签库"
          :loading="state.loading"
          style="width: 100%"
        >
          <el-option
            v-for="template in state.labelTemplates"
            :key="template.id"
            :label="template.name"
            :value="template.id"
          />
        </el-select>
      </el-form-item>
      <div class="w-full flex flex-row justify-end">
        <el-button size="small" @click="handleCancel">取消</el-button>
        <el-button size="small" type="primary" @click="handleOk">确定</el-button>
      </div>
    </el-form>
    <template #reference>
      <slot name="trigger">
        <el-button class="import-label-btn" size="small" @click="() => (state.visible = true)">
          <span class="btn-icon">📥</span>
          <span>从标签库导入</span>
        </el-button>
      </slot>
    </template>
  </el-popover>
</template>
<script>
  import { nextTick, onMounted, reactive, ref } from 'vue';
  import { getAllLabelTemplate } from '/@/views/mineai/data/dataset-details2/api/index.ts';
  import { ElMessage } from 'element-plus';

  export default {
    name: 'LabelGroup',
    components: {},
    props: {
      getStyle: Function,
      title: String,
      labelData: {
        type: Object,
        default: () => ({}),
      },
    },
    emits: ['handleOk'],
    setup(props, ctx) {
      const inputRef = ref(null);
      const formRef = ref(null);

      const state = reactive({
        visible: false,
        dialogKey: 1,
        form: {
          id: null,
        },
        labelTemplates: [], // 标签库列表
        loading: false, // 加载状态
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
            }));
          } else {
            console.log('Failed to fetch label templates');
          }
        } catch (error) {
          console.error('Error fetching label templates');
        } finally {
          state.loading = false;
        }
      };

      // 表单规则
      const rules = {
        id: [{ required: true, message: '请选择标签库', trigger: ['change', 'blur'] }],
      };

      const handleCancel = () => {
        Object.assign(state, {
          visible: false,
          form: {
            id: null, // 清空下拉菜单的选中值
          },
        });
      };

      const handleOk = () => {
        // 验证表单内容
        formRef.value.validate((valid) => {
          if (!valid) {
            //console.log('表单验证未通过'); // 如果表单验证未通过，阻止提交
            return;
          }
          // 检查下拉菜单是否选择了有效的选项
          if (state.form.id === null || state.form.id === '') {
            // 使用 Element Plus 的消息提示功能
            ElMessage({
              message: '请选择要导入的标签',
              type: 'warning',
            });
            return;
          }
          // 触发父组件事件，将选择的标签库 ID 传递给父组件
          ctx.emit('handleOk', state.form.id);
          // 关闭弹窗
          handleCancel();
        });
      };

      const onShow = () => {
        // onShow 的时候重置
        nextTick(() => {
          // const input = inputRef.value && inputRef.value.$refs.input;
          // input && input.focus();
          // inputRef.value.focus();
          if (inputRef.value) {
            inputRef.value.focus(); // 确保 inputRef 不为 null
          }
        });
      };

      const onHide = () => {
        Object.assign(state, {
          dialogKey: state.dialogKey + 1,
        });
      };

      // 在组件挂载时获取数据
      onMounted(() => {
        fetchLabelTemplates();
      });

      return {
        props,
        state,
        rules,
        inputRef,
        formRef,
        handleOk,
        handleCancel,
        onShow,
        onHide,
        fetchLabelTemplates,
      };
    },
  };
</script>
<style lang="scss" scoped>
  .import-label-btn {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    color: #ffffff;
    font-weight: 500;
    padding: 8px 16px;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);

    &:hover {
      background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.5);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
      box-shadow: 0 2px 6px rgba(102, 126, 234, 0.4);
    }

    .btn-icon {
      margin-right: 6px;
      font-size: 14px;
    }
  }
</style>
