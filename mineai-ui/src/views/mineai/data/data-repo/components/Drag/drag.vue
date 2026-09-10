<script lang="jsx">
  import { reactive } from 'vue';
  import { findAncestorSvg } from '/@/utils/dubhe';

  export default {
    name: 'Drag',
    props: {
      width: Number,
      height: Number,
      resetOnStart: {
        type: Boolean,
        default: false,
      },
      onDragStart: Function,
      onDragMove: Function,
      onDragEnd: Function,
    },
    setup(props) {
      const state = reactive({
        x: undefined,
        y: undefined,
        dx: 0,
        dy: 0,
        isDragging: false, // 鼠标按下
        isMoving: true, // 鼠标移动
      });

      function getPoint(event) {
        // 容器尺寸
        const bound = findAncestorSvg(event).getBoundingClientRect();
        const { clientX, clientY } = event;

        return {
          x: clientX - bound.left,
          y: clientY - bound.top,
        };
      }

      function dragStart(event) {
        const point = getPoint(event);
        const nextState = {
          isDragging: true,
          isMoving: false,
          dx: props.resetOnStart ? 0 : state.dx,
          dy: props.resetOnStart ? 0 : state.dy,
          x: props.resetOnStart ? point.x : -state.dx + point.x,
          y: props.resetOnStart ? point.y : -state.dy + point.y,
        };
        Object.assign(state, nextState);
        if (typeof props.onDragStart === 'function') props.onDragStart(nextState, event);
      }

      function dragMove(event) {
        if (!state.isDragging) return;
        const point = getPoint(event);
        // 避免无效移动
        if (Math.abs(point.x - state.x) < 2 && Math.abs(point.y - state.y) < 2) return;
        const nextState = {
          isDragging: true,
          isMoving: true,
          dx: point.x - state.x,
          dy: point.y - state.y,
        };
        Object.assign(state, nextState);
        if (typeof props.onDragMove === 'function') props.onDragMove(state, event);
      }

      function dragEnd(event) {
        const nextState = {
          isDragging: false,
          isMoving: false,
        };
        const prevState = { ...state };
        Object.assign(state, nextState);
        // 传递 prevState
        if (typeof props.onDragEnd === 'function')
          props.onDragEnd(state, event, {
            prevState,
          });
      }

      return {
        state,
        dragStart,
        dragMove,
        dragEnd,
      };
    },

    render() {
      const children = this.$slots.default;

      return (
        <g>
          {this.state.isDragging && (
            <rect
              width={this.width}
              height={this.height}
              onMousemove={this.dragMove}
              onMouseup={this.dragEnd}
              fill="transparent"
            />
          )}
          {typeof children === 'function' &&
            children({
              state: this.state,
              dragStart: this.dragStart,
              dragMove: this.dragMove,
              dragEnd: this.dragEnd,
            })}
        </g>
      );
    },
  };
</script>
