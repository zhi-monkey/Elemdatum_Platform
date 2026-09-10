import { isNil } from 'lodash-es';
import { reactive, watch, ref } from 'vue';

const BaseModal = {
  name: 'BaseModal',
  inheritAttrs: false,
  model: {
    prop: 'visible',
    event: 'change',
  },
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    width: {
      type: String,
      default: '600px',
    },
    okText: {
      type: String,
      default: '确定',
    },
    cancelText: {
      type: String,
      default: '取消',
    },
    footer: Function,
    showCancel: {
      type: Boolean,
      default: true,
    },
    showOk: {
      type: Boolean,
      default: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
  },
  setup(props, ctx) {
    const dialogRef = ref(null);
    const state = reactive({
      sVisible: !isNil(props.visible) ? props.visible : false,
    });

    const handleCancel = (e) => {
      ctx.emit('cancel', e);
      ctx.emit('change', false);
    };

    const handleOk = (e) => {
      ctx.emit('ok', e);
    };

    const handleClose = (e) => {
      // 这里只针对状态变更进行控制，只转发 element close 事件
      ctx.emit('close', e);
      ctx.emit('change', false);
    };

    watch(
      () => props.visible,
      (next) => {
        Object.assign(state, {
          sVisible: next,
        });
      },
    );

    return {
      state,
      dialogRef,
      handleCancel,
      handleClose,
      handleOk,
    };
  },
  render() {
    const renderFooter = () => {
      return (
        <div class="modal-footer">
          {this.showCancel && (
            <el-button id="cancel" onClick={this.handleCancel}>
              {this.cancelText}
            </el-button>
          )}
          {this.showOk && (
            <el-button
              id="ok"
              type="primary"
              disabled={this.props.disabled}
              onClick={this.handleOk}
              loading={this.props.loading}
            >
              {this.okText}
            </el-button>
          )}
        </div>
      );
    };
    // footer
    const footer = this.$slots.footer || renderFooter();

    const dialogProps = {
      props: {
        closeOnClickModal: false,
        appendToBody: true,
        visible: this.state.sVisible,
        ...this.$props,
        ...this.$attrs,
      },
      on: {
        close: this.handleClose,
        // 转发 el-dialog 事件
        ...this.$listeners,
      },
    };

    return (
      <el-dialog {...dialogProps} ref="dialogRef">
        {this.$slots.default}
        <template v-slot:footer>
          <div>{footer}</div>
        </template>
      </el-dialog>
    );
  },
};

export default BaseModal;
