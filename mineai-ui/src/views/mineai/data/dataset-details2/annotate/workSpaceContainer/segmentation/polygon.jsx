import { nextTick, reactive } from 'vue';
import { curveLinearClosed } from 'd3/dist/d3';

import Drag from '../../../components/Drag';
import { calcDistance, getPolygonExtent, inBoundary, midPoint } from '/@/utils/dubhe';

import PolylineRender from './polyline.vue';
import Vertice from './vertice';

// 锚点最小距离
export const MIN_POINT_DISTANCE = 20;

export default {
  name: 'PolygonRender',
  components: {
    PolylineRender,
    Vertice,
  },
  props: {
    guides: Array,
    stageWidth: Number,
    stageHeight: Number,
    currentAnnotationId: String,
    handleChange: Function,
    draw: Object,
    transformer: Object,
    scale: Number, // 图片相对于原始大小的缩放比例
    offset: Function,
    getZoom: Function,
    bounds: Object,
    shape: Object,
    onDragStart: Function,
    onDragMove: Function,
    onDragEnd: Function,
    setTransformer: Function,
    onRightClick: Function,
  },
  setup(props) {
    const state = reactive({
      drag: undefined,
    });

    const updateState = (updater, callback) => {
      const newState = updater(state);
      nextTick(() => {
        Object.assign(state, newState);
        if (typeof callback === 'function') {
          callback(state);
        }
      });
    };

    const renderMidPoints = (points) => {
      return points
        .map((pos, index) => [pos, points[(index + 1) % points.length], index])
        .filter(([a, b]) => calcDistance(a, b) > MIN_POINT_DISTANCE * 2);
    };

    const selectionDragStart = (drag, event) => {
      const start = {
        x: drag.x + drag.dx,
        y: drag.y + drag.dy,
      };
      const end = { ...start };

      const transformState = {
        start,
        end,
      };

      // 回调
      if (typeof props.onDragStart === 'function') {
        props.onDragStart(transformState, props.shape, event);
      }
    };

    const selectionDragMove = (drag) => {
      const { zoom } = props.getZoom();
      updateState(
        (prevState) => {
          // 位置比较计算
          // 修改：使用更合适的缩放因子，确保拖拽速度与鼠标移动速度匹配
          const _scale = zoom;

          const { x0, x1, y0, y1 } = getPolygonExtent(props.shape.data.points);

          const validDx =
            drag.dx > 0
              ? Math.min(drag.dx / _scale, props.bounds.width - x1)
              : Math.max(drag.dx / _scale, -x0);

          const validDy =
            drag.dy > 0
              ? Math.min(drag.dy / _scale, props.bounds.height - y1)
              : Math.max(drag.dy / _scale, -y0);

          return {
            ...prevState,
            drag: {
              ...drag,
              validDx,
              validDy,
            },
          };
        },
        (nextState) => {
          if (typeof props.onDragMove === 'function') {
            props.onDragMove(nextState, props.shape);
          }
        },
      );
    };

    const selectionDragEnd = (drag, event, options = {}) => {
      const { prevState } = options;
      props.setTransformer({
        isDragging: false,
        id: props.shape.id,
        x: drag.x,
        y: drag.y,
        dx: 0,
        dy: 0,
      });
      // fix 双击触发移动选框
      if (!prevState.isMoving) return;
      updateState(
        (state) => ({
          ...state,
          drag: {
            ...state.drag,
            ...drag,
          },
        }),
        (nextState) => {
          // 拖拽结束以后重新生成路径
          if (typeof props.onDragEnd === 'function') {
            props.onDragEnd(nextState, props.shape);
          }
        },
      );
    };

    const handleContextMenu = (event) => {
      event.preventDefault();
      event.stopPropagation();
      if (typeof props.onRightClick === 'function') {
        props.onRightClick(event, props.shape);
      }
    };

    return {
      state,
      updateState,
      renderMidPoints,
      selectionDragStart,
      selectionDragMove,
      selectionDragEnd,
      handleContextMenu,
    };
  },
  render() {
    const {
      shape = {},
      transformer = {},
      renderMidPoints,
      handleChange,
      draw,
      stageWidth,
      stageHeight,
      offset,
    } = this;

    let transform = null;
    // 匹配当前标注
    if (shape.id === transformer.id) {
      transform = {
        translate: `translate(${transformer.dx}, ${transformer.dy})`,
        isDragging: transformer.isDragging,
      };
    }

    const style = {
      pointerEvents: draw.isDrawing || transform?.isDragging ? 'none' : 'all',
    };

    const dragProps = {
      resetOnStart: true,
      width: stageWidth,
      height: stageHeight,
      onDragStart: this.selectionDragStart,
      onDragMove: this.selectionDragMove,
      onDragEnd: this.selectionDragEnd,
    };

    const isActive = shape.id === this.currentAnnotationId;

    return (
      <Drag {...dragProps} key={shape.id}>
        {(draw) => {
          return (
            <g>
              {draw.state.isDragging && (
                <rect
                  width={stageWidth}
                  height={stageHeight}
                  fill="transparent"
                  onMouseup={draw.dragEnd}
                  onMousemove={draw.dragMove}
                  onMouseleave={(event) => {
                    // 超出边界判断
                    if (!inBoundary(event, event.target)) {
                      draw.dragEnd();
                    }
                  }}
                  style={{
                    cursor: 'move',
                  }}
                />
              )}
              {
                // 针对拖拽节点状态
                isActive &&
                  this.guides.map((guide, index) => {
                    return (
                      <PolylineRender
                        key={index}
                        points={guide}
                        offset={offset}
                        fill={shape.data.color}
                        stroke-opacity={0.7}
                        stroke-dasharray="5"
                      />
                    );
                  })
              }
              <g transform={transform?.translate} style={style}>
                <PolylineRender
                  key={shape.id}
                  points={shape.data.points}
                  fill={shape.data.color}
                  stroke-dasharray={isActive ? '10' : undefined}
                  fill-opacity={isActive ? 0.4 : 0.1}
                  curve={curveLinearClosed}
                  offset={offset}
                  onMousedown={draw.dragStart}
                  onMousemove={draw.dragMove}
                  onMouseup={draw.dragEnd}
                  onContextmenu={this.handleContextMenu}
                />
                {isActive && [
                  shape.data.points.map((point, index) => {
                    // 已有节点
                    const pointProps = {
                      handlePointClick: () =>
                        handleChange('REMOVE_POINT', {
                          index,
                          shape,
                        }),
                      fill: shape.data.color,
                    };
                    return (
                      <Vertice
                        key={index}
                        index={index}
                        position={point}
                        shape={shape}
                        offset={offset}
                        draggable={true}
                        stageWidth={stageWidth}
                        stageHeight={stageHeight}
                        handleChange={handleChange}
                        {...pointProps}
                      />
                    );
                  }),
                  renderMidPoints(shape.data.points).map(([a, b, index]) => {
                    // 新增节点
                    const addPointProps = {
                      handlePointClick: () =>
                        handleChange('ADD_POINT', {
                          point: midPoint(a, b),
                          index: index + 1,
                          shape,
                        }),
                    };
                    return (
                      <Vertice
                        key={`${index}-mid`}
                        index={index}
                        position={midPoint(a, b)}
                        shape={shape}
                        offset={offset}
                        innerRadius={3}
                        outerRadius={8}
                        fill="#fff"
                        fillOpacity={0.6}
                        className="add-point"
                        {...addPointProps}
                      />
                    );
                  }),
                ]}
              </g>
            </g>
          );
        }}
      </Drag>
    );
  },
};
