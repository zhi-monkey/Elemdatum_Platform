<template>
  <el-popover
    :key="state.dialogKey"
    :visible="state.visible"
    placement="top"
    width="240"
    :title="props.title"
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
      <el-form-item label="名称" prop="name">
        <el-input ref="inputRef" v-model="state.form.name" :placeholder="props.title" />
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
  import { reactive, ref, nextTick } from 'vue';
  import { validateName } from '/@/utils/dubhe/validate';
  import { isNil } from 'lodash-es';
  import { Button as AButton } from 'ant-design-vue';
  import { generateRandomHexColor } from '/@/views/mineai/data/dataset-details2/api';

  export default {
    name: 'LabelEditor',
    components: { AButton },
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
          name: props.labelData.name || '',
          color: props.labelData.color || generateRandomHexColor(),
        },
      });

      // 表单规则
      const rules = {
        name: [
          { required: true, message: '请输入标签名称', trigger: ['change', 'blur'] },
          { validator: validateName, trigger: ['change', 'blur'] },
        ],
      };

      const handleCancel = () => {
        Object.assign(state, {
          visible: false,
          form: {
            name: props.labelData.name || '',
            color: props.labelData.color || generateRandomHexColor(),
          },
        });
      };

      // 编辑标注名称
      const handleOk = () => {
        formRef.value.validate().then((valid) => {
          if (!valid) {
            return;
          }
          // 标签信息无改动时，只需关闭弹窗
          if (
            !(
              state.form.color === props.labelData.color && state.form.name === props.labelData.name
            )
          ) {
            ctx.emit('handleOk', props.labelData.id, state.form);
          }
          // 原来的传入数据为空，判断是创建标签，故清空，否则是编辑标签，刷新key
          if (isNil(props.labelData.name)) {
            handleCancel();
          } else {
            Object.assign(state, {
              dialogKey: state.dialogKey + 1,
            });
          }
        });
      };

      const onShow = () => {
        // onShow 的时候重置
        nextTick(() => {
          // const input = inputRef.value && inputRef.value.$refs.input;
          // input && input.focus();
          inputRef.value.focus();
        });
      };

      const onHide = () => {
        Object.assign(state, {
          dialogKey: state.dialogKey + 1,
        });
      };

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
