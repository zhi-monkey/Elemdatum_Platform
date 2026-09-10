<template>
  <el-popover
    v-model:visible="state.visible"
    placement="bottom"
    :width="width"
    trigger="click"
    :title="props.title"
    @show="onShow"
  >
    <el-input
      ref="inputRef"
      v-model.trim="state.value"
      clearable
      placeholder=""
      :type="inputType"
      @keyup.enter="handleOk"
    >
      <template v-for="(_, name) in $slots" #[name]>
        <slot :name="name"></slot>
      </template>
    </el-input>
    <div class="tc" style="margin-top: 8px">
      <el-button size="small" type="primary" @click="handleCancel">取消</el-button>
      <el-button size="small" type="primary" @click="handleOk">确定</el-button>
    </div>
    <template #reference>
      <EditOutlined :class="triggerClass" style="color: #108ee9; cursor: pointer" @click.stop />
    </template>
  </el-popover>
</template>
<script>
  import { reactive, ref, watch, computed, nextTick } from 'vue';
  import cx from 'classnames';
  import { EditOutlined } from '@ant-design/icons-vue';

  export default {
    name: 'Edit',
    components: {
      EditOutlined,
    },
    props: {
      row: {
        type: Object,
        default: () => ({}),
      },
      width: {
        type: Number,
        default: 200,
      },
      inputType: {
        type: String,
        default: 'input',
      },
      title: String,
      valueBy: String,
      rules: {
        type: String,
        default: 'required|validName', // 默认规则，详细参考 src/utils/validate
      },
      label: {
        type: String,
        default: '名称', // 错误展示字段
      },
      disabled: {
        type: Boolean,
        default: false,
      },
      // 修改前校验
      beforeChange: {
        type: Function,
      },
    },
    emits: ['handleOk'],
    setup(props, ctx) {
      const { valueBy, beforeChange } = props;
      const observerRef = ref(null);
      const inputRef = ref(null);
      const providerRef = ref(null);

      const state = reactive({
        visible: false,
        value: props.row[valueBy] || '',
      });

      const handleCancel = () => {
        Object.assign(state, {
          visible: false,
          value: '',
        });
      };

      //todo:加入表单校验

      // 编辑标注名称
      const handleOk = () => {
        if (typeof beforeChange === 'function') {
          beforeChange(state.value, props.row, providerRef, { valueBy })
            .then(() => {
              // TODO: 判断是否发生过变更
              ctx.emit('handleOk', state.value, props.row, { valueBy });
              handleCancel();
            })
            .catch((err) => {
              console.error(err);
            });
        } else {
          ctx.emit('handleOk', state.value, props.row, { valueBy });
          handleCancel();
        }
      };

      const onShow = () => {
        // onShow 的时候重置
        state.value = props.row[valueBy];
        nextTick(() => {
          const input =
            (inputRef.value && inputRef.value.$refs.input) ||
            (inputRef.value && inputRef.value.$refs.textarea);
          input && input.focus();
        });
      };

      const triggerClass = computed(() =>
        cx('el-icon-edit primary cp dib', {
          disabled: !!props.disabled,
          pen: !!props.disabled,
        }),
      );

      watch(
        () => props.row,
        (next) => {
          if (next) {
            state.value = next[valueBy];
          }
        },
      );

      return {
        props,
        state,
        inputRef,
        observerRef,
        providerRef,
        handleOk,
        handleCancel,
        triggerClass,
        onShow,
      };
    },
  };
</script>
