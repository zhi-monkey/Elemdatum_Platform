import cx from 'classnames';
import { reactive, nextTick } from 'vue';
import Drag from '../../Drag';
import Group from '../group';
import BrushSelection from './BrushSelection';

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Brush',
  components: {
    Group,
  },
  props: {
    stageWidth: Number,
    stageHeight: Number,
    className: String,
    onBrushStart: Function,
    onBrushMove: Function,
    onBrushEnd: Function,
    transformZoom: Function,
    left: {
      type: Number,
      default: 0,
    },
    top: {
      type: Number,
      default: 0,
    },
    brushSelectionStyle: {
      type: Object,
      default: () => ({
        fill: 'rgba(102, 181, 245, 0.1)',
        stroke: 'rgba(102, 181, 245, 1)',
        strokeWidth: 1,
      }),
    },
  },

  setup(props) {
    // eslint-disable-next-line vue/no-setup-props-destructure
    const { onBrushStart, onBrushMove, left, top, onChange, onBrushEnd, transformZoom } = props;
    const state = reactive({
      start: { x: 0, y: 0 },
      end: { x: 0, y: 0 },
      extent: { x0: 0, x1: 0, y0: 0, y1: 0 },
      isBrushing: false,
    });
    const getWidth = () => {
      return Math.abs(state.extent.x1 - state.extent.x0);
    };

    const getHeight = () => {
      return Math.abs(state.extent.y1 - state.extent.y0);
    };

    const getExtent = (start, end) => {
      const x0 = Math.min(start.x, end.x);
      const x1 = Math.max(start.x, end.x);
      const y0 = Math.min(start.y, end.y);
      const y1 = Math.max(start.y, end.y);

      return {
        x0,
        x1,
        y0,
        y1,
      };
    };

    const update = (updater, callback) => {
      Object.assign(state, updater(state));
      nextTick(() => {
        if (callback) {
          callback(state);
        }
        if (onChange) {
          onChange(state);
        }
      });
    };

    const handleDragStart = (draw, event) => {
      const start = transformZoom({
        x: draw.x + draw.dx - left,
        y: draw.y + draw.dy - top,
      });
      if (onBrushStart) {
        onBrushStart(start, event);
      }

      update((prevBrush) => ({
        ...prevBrush,
        start,
        end: undefined,
        extent: {
          x0: -1,
          x1: -1,
          y0: -1,
          y1: -1,
        },
        isBrushing: true,
      }));
    };

    const handleDragMove = (draw, event) => {
      if (!draw.isDragging) return;
      const end = transformZoom({
        x: draw.x + draw.dx - left,
        y: draw.y + draw.dy - top,
      });

      update(
        (prevBrush) => {
          const { start } = prevBrush;
          const extent = getExtent(start, end);
          return {
            ...prevBrush,
            end,
            extent,
          };
        },
        (nextState) => {
          // 回调
          typeof onBrushMove === 'function' && onBrushMove(nextState, event);
        },
      );
    };

    const handleDragEnd = (draw, event, options = {}) => {
      update(
        (prevBrush) => ({
          ...prevBrush,
          isBrushing: false,
        }),
        (state) => onBrushEnd(state, event, options),
      );
    };

    return {
      state,
      getWidth,
      getHeight,
      update,
      handleDragStart,
      handleDragMove,
      handleDragEnd,
      getExtent,
    };
  },

  render() {
    const { stageWidth, stageHeight, className, left, top, brushSelectionStyle } = this;
    const { start, end, isBrushing } = this.state;

    const width = this.getWidth();
    const height = this.getHeight();

    // const dragProps = {
    //   props: {
    //     width: stageWidth,
    //     height: stageHeight,
    //     resetOnStart: true,
    //     onDragStart: this.handleDragStart,
    //     onDragMove: this.handleDragMove,
    //     onDragEnd: this.handleDragEnd,
    //   },
    // };

    const dragProps = {
      width: stageWidth,
      height: stageHeight,
      resetOnStart: true,
      onDragStart: this.handleDragStart,
      onDragMove: this.handleDragMove,
      onDragEnd: this.handleDragEnd,
    };

    return (
      <Group className={cx('db-brush', className)} left={left} top={top}>
        {/* overlay */}
        <Drag {...dragProps}>
          {(drag) => (
            <rect
              className="brush-overlay"
              fill="transparent"
              x={0}
              y={0}
              width={stageWidth}
              height={stageHeight}
              style={{ cursor: 'crosshair' }}
              onMousedown={drag.dragStart}
              onMousemove={drag.dragMove}
              onMouseup={drag.dragEnd}
            />
          )}
        </Drag>
        {start && end && !!isBrushing && (
          <g>
            <BrushSelection
              updateBrush={this.update}
              width={width}
              height={height}
              stageWidth={stageWidth}
              stageHeight={stageHeight}
              brush={{ ...this.state }}
              selectionStyle={brushSelectionStyle}
            />
          </g>
        )}
      </Group>
    );
  },
};
