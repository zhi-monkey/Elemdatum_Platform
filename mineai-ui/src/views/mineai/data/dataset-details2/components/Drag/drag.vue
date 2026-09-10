<script lang="jsx">
  import { onBeforeUnmount, onMounted, reactive } from 'vue';
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
        svgElement: null, // 用于保存语义分割的svg元素
      });

      function getPoint(event, svgElement = null) {
        // 容器尺寸
        const svg = svgElement || findAncestorSvg(event);
        const bound = svg.getBoundingClientRect();
        const { clientX, clientY } = event;

        // 检测并补偿浏览器缩放
        // SVG 的 offsetWidth 是 CSS 像素（未受浏览器缩放影响）
        // getBoundingClientRect 返回的是设备像素（受浏览器缩放影响）
        const browserZoom = svg.offsetWidth > 0 ? bound.width / svg.offsetWidth : 1;

        // 将设备像素转换为 CSS 像素（SVG 坐标系）
        return {
          x: (clientX - bound.left) / browserZoom,
          y: (clientY - bound.top) / browserZoom,
        };
      }

      function dragStart(event) {
        // 在拖拽开始时保存SVG元素
        const svgElement = findAncestorSvg(event);
        const point = getPoint(event, svgElement);

        const nextState = {
          isDragging: true,
          isMoving: false,
          dx: props.resetOnStart ? 0 : state.dx,
          dy: props.resetOnStart ? 0 : state.dy,
          x: props.resetOnStart ? point.x : -state.dx + point.x,
          y: props.resetOnStart ? point.y : -state.dy + point.y,
          svgElement: svgElement, // 保存SVG元素引用
        };

        Object.assign(state, nextState);
        if (typeof props.onDragStart === 'function') props.onDragStart(nextState, event);
      }

      function dragMove(event) {
        if (!state.isDragging) return;

        // 使用保存的SVG元素引用
        const point = getPoint(event, state.svgElement);

        // 避免无效移动
        if (Math.abs(point.x - state.x) < 2 && Math.abs(point.y - state.y) < 2) return;

        const nextState = {
          isDragging: true,
          isMoving: true,
          dx: point.x - state.x,
          dy: point.y - state.y,
          svgElement: state.svgElement, // 保持SVG元素引用
        };

        Object.assign(state, nextState);
        if (typeof props.onDragMove === 'function') props.onDragMove(state, event);
      }

      function dragEnd(event) {
        if (!state.isDragging) return;

        const nextState = {
          isDragging: false,
          isMoving: false,
          svgElement: null, // 清除SVG元素引用
        };

        const prevState = { ...state };
        Object.assign(state, nextState);

        if (typeof props.onDragEnd === 'function') props.onDragEnd(state, event, { prevState });
      }

      // 添加全局鼠标事件监听
      let documentMouseMoveHandler = null;
      let documentMouseUpHandler = null;

      onMounted(() => {
        // 全局鼠标移动事件
        documentMouseMoveHandler = (event) => {
          if (state.isDragging) {
            dragMove(event);
          }
        };

        // 全局鼠标释放事件
        documentMouseUpHandler = (event) => {
          if (state.isDragging) {
            dragEnd(event);
          }
        };

        document.addEventListener('mousemove', documentMouseMoveHandler);
        document.addEventListener('mouseup', documentMouseUpHandler);
      });

      onBeforeUnmount(() => {
        // 移除全局事件监听
        document.removeEventListener('mousemove', documentMouseMoveHandler);
        document.removeEventListener('mouseup', documentMouseUpHandler);
      });

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
