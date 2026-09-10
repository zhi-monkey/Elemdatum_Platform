// 基础 Tooltip 组件，用于在手动标注页面框选标注，选择标签
import './style.scss';

export default {
  name: 'BasicTooltip',
  props: {
    position: {
      type: Object,
      default: () => ({}),
    },
  },

  render() {
    const { position = {} } = this;

    // // this sucks~
    const positionStyle = {};
    if (position.left) {
      positionStyle.left = `${position.left || 0}px`;
    }
    if (position.right) {
      positionStyle.right = `${position.right || 0}px`;
    }
    if (position.top) {
      positionStyle.top = `${position.top || 0}px`;
    }
    if (position.bottom) {
      positionStyle.bottom = `${position.bottom || 0}px`;
    }

    return (
      <div class={`zj-tooltip basic-tooltip`} style={positionStyle}>
        {this.$slots.default()}
      </div>
    );
  },
};
