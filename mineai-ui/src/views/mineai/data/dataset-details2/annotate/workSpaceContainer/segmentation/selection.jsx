import { reactive, watch } from 'vue';
import cx from 'classnames';
import { isEmpty } from 'lodash-es';

import { calcDistance } from '/@/utils/dubhe';
import Drag from '../../../components/Drag';
import Vertice from './vertice';
import PolylineRender from './polyline.vue';
import { MIN_POINT_DISTANCE } from './polygon';

export default {
  name: 'SegmentationSelection',
  components: {
    Vertice,
    PolylineRender,
  },
  props: {
    stageWidth: Number,
    stageHeight: Number,
    className: String,
    state: {
      type: Object,
      default: () => ({}),
    },
    draw: Object,
    handlePointClick: Function,
    handleChange: Function,
    transformZoom: Function,
    isMoveMode: Boolean,
  },

  setup(props) {
    // eslint-disable-next-line vue/no-setup-props-destructure
    const { handleChange, transformZoom } = props;

    const drag = reactive({
      lastPoint: null,
    });

    const resetDrag = () => {
      Object.assign(drag, {
        lastPoint: null,
      });
    };

    const handleDragStart = (draw) => {
      const point = transformZoom({ x: draw.x, y: draw.y });
      Object.assign(drag, {
        lastPoint: point,
      });
      handleChange('DRAW_START', { point });
    };

    const handleDragMove = (draw, event) => {
      const point = transformZoom({
        x: draw.x + draw.dx,
        y: draw.y + draw.dy,
      });
      handleChange('DRAW_MOVE', { point });
      if (event.shiftKey && drag.lastPoint) {
        if (calcDistance(point, drag.lastPoint) > MIN_POINT_DISTANCE) {
          handleChange('DRAW_POINT', { point });
          Object.assign(drag, {
            lastPoint: point,
          });
        }
      }
    };

    const handleDragEnd = () => {
      resetDrag();
    };

    const getFill = (point, index) => (index === 0 ? '#fff' : undefined);

    watch(
      () => props.state.status,
      (next) => {
        if (next === 'FINISHED' || next === '') {
          resetDrag();
        }
      },
    );

    return {
      drag,
      getFill,
      handleDragStart,
      handleDragMove,
      handleDragEnd,
    };
  },

  render() {
    const { stageWidth, stageHeight, className, handlePointClick, draw, getFill } = this;
    const { unfinishedShape = {}, guides } = this.state;
    const { points = [] } = unfinishedShape;

    const style = {
      pointerEvents: draw.isDrawing ? 'none' : 'all',
    };

    const dragProps = {
      width: stageWidth,
      height: stageHeight,
      resetOnStart: true,
      onDragStart: this.handleDragStart,
      onDragMove: this.handleDragMove,
      onDragEnd: this.handleDragEnd,
    };

    return (
      <g class={cx('db-brush', className)} style={{ pointerEvents: this.isMoveMode ? 'none' : 'auto' }}>
        {/* overlay */}
        <Drag {...dragProps}>
          {(drag) => (
            <rect
              class="selection-overlay"
              fill="transparent"
              x={0}
              y={0}
              width={stageWidth}
              height={stageHeight}
              style={{ cursor: this.isMoveMode ? 'default' : 'crosshair' }}
              onMousedown={this.isMoveMode ? null : drag.dragStart}
              onMousemove={this.isMoveMode ? null : drag.dragMove}
              onMouseup={this.isMoveMode ? null : drag.dragEnd}
            />
          )}
        </Drag>
        {!isEmpty(points) && (
          <g>
            <PolylineRender points={guides} fill-opacity="1" stroke-dasharray="5" style={style} />
            <PolylineRender points={points} fill-opacity="0.2" style={style} />
            {points.map((point, index) => (
              <Vertice
                key={index}
                index={index}
                position={point}
                shape={unfinishedShape}
                handlePointClick={handlePointClick}
                fill={getFill(point, index)}
              />
            ))}
          </g>
        )}
      </g>
    );
  },
};
