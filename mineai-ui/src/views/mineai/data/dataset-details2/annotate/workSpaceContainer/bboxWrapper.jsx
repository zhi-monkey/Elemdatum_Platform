import { isEmpty } from 'lodash-es';
import { h, nextTick, reactive, watch } from 'vue';
import { inBoundary } from '/@/utils/dubhe';
import Drag from '../../components/Drag';
import { BrushCorner, BrushHandle } from '../../components/svg';
import Bbox from './bbox.vue';
import { useMessage } from '/@/hooks/web/useMessage';

export default {
  name: 'BboxWrapper',
  components: {
    Drag,
    Bbox,
  },
  inheritAttrs: false,
  props: {
    annotate: Object,
    brush: {
      type: Object,
      default: () => ({}),
    },
    onDragStart: Function,
    onDragMove: Function,
    onDragEnd: Function,
    onBrushHandleChange: Function,
    onBrushHandleEnd: Function,
    transformer: Object,
    currentAnnotationId: String,
    setCurAnnotation: Function,
    getZoom: Function,
    handleSize: {
      type: Number,
      default: 6,
    },
    offset: Function,
    scale: {
      type: Number,
      default: 1,
    },
    bounds: {
      type: Object,
    },
    svg: {
      type: Object,
      default: () => ({}),
    },
    actionDisabled: Boolean,
    isMoveMode: Boolean,
    onRightClick: Function,
  },
  setup(props) {
    // eslint-disable-next-line vue/no-setup-props-destructure
    const {
      offset,
      scale,
      onDragStart,
      onDragMove,
      onDragEnd,
      onBrushHandleChange,
      bounds = {},
      onBrushHandleEnd,
      setCurAnnotation,
      getZoom,
    } = props;
    const { createMessage } = useMessage();

    function getExtent() {
      const { data = {} } = props.annotate;
      const { extent = {} } = data;
      return {
        extent,
        start: {
          x: extent.x0,
          y: extent.y0,
        },
        end: {
          x: extent.x1,
          y: extent.y1,
        },
      };
    }

    const state = reactive({
      activeHandle: undefined,
      drag: undefined,
      bounds: { x0: 0, x1: bounds.width * scale, y0: 0, y1: bounds.height * scale },
      ...getExtent(),
    });

    const updateBrush = (updater, callback) => {
      if (props.actionDisabled || !props.isMoveMode) return;
      const newState = updater(state);
      nextTick(() => {
        Object.assign(state, newState);
        if (typeof callback === 'function') {
          callback(state);
        }
      });
    };

    // handler 拖拽事件
    const updateBrushHandler = (updater) => {
      if (props.actionDisabled || !props.isMoveMode) return;
      updateBrush(updater, (state) => {
        if (typeof onBrushHandleChange === 'function') {
          onBrushHandleChange(state, props.annotate);
        }
      });
    };

    // handler 拖拽结束
    const updateBrushHandlerEnd = (updater) => {
      if (props.actionDisabled) return createMessage.info('当前处于查看模式, 不能进行操作');
      if (!props.isMoveMode) {
        createMessage.info('当前处于绘图模式，不能移动标注框');
        return;
      }
      updateBrush(updater, (state) => {
        if (typeof onBrushHandleEnd === 'function') {
          onBrushHandleEnd(state, props.annotate);
        }
      });
    };

    // 计算手柄位置 - 根据缩放比例动态调整
    const handles = () => {
      // 根据缩放比例动态调整手柄大小
      const handleSize = Math.max(2, props.handleSize / props.scale);
      const handleOffset = handleSize / 2;

      // 获取标注框的缩放后位置和尺寸
      const { x, y, width, height } = offset(props.annotate);

      return {
        top: {
          x: x - handleOffset,
          y: y - handleOffset,
          height: handleSize,
          width: width + handleSize,
        },
        bottom: {
          x: x - handleOffset,
          y: y + height - handleOffset,
          height: handleSize,
          width: width + handleSize,
        },
        right: {
          x: x + width - handleOffset,
          y: y - handleOffset,
          height: height + handleSize,
          width: handleSize,
        },
        left: {
          x: x - handleOffset,
          y: y - handleOffset,
          height: height + handleSize,
          width: handleSize,
        },
      };
    };

    // 计算角点位置 - 根据缩放比例动态调整
    const corners = () => {
      // 根据缩放比例动态调整手柄大小
      const handleSize = Math.max(2, props.handleSize / props.scale);
      const handleOffset = handleSize / 2;

      // 获取标注框的缩放后位置和尺寸
      const { x, y, width, height } = offset(props.annotate);

      return {
        topLeft: {
          x: x - handleOffset,
          y: y - handleOffset,
        },
        bottomLeft: {
          x: x - handleOffset,
          y: y + height - handleOffset,
        },
        topRight: {
          x: x + width - handleOffset,
          y: y - handleOffset,
        },
        bottomRight: {
          x: x + width - handleOffset,
          y: y + height - handleOffset,
        },
      };
    };

    const brushHandlerStart = () => {
      setCurAnnotation(props.annotate);
      // 在开始缩放/调整时，更新state为当前的屏幕坐标
      const pos = offset(props.annotate);
      Object.assign(state, {
        start: {
          x: pos.x,
          y: pos.y,
        },
        end: {
          x: pos.x + pos.width,
          y: pos.y + pos.height,
        },
        extent: {
          x0: pos.x,
          y0: pos.y,
          x1: pos.x + pos.width,
          y1: pos.y + pos.height,
        },
      });
    };

    const handleContextMenu = (event) => {
      event.preventDefault(); // 阻止默认右键菜单
      setCurAnnotation(props.annotate);

      // 触发右键事件回调
      if (typeof props.onRightClick === 'function') {
        props.onRightClick(event, props.annotate);
      }
    };
    const selectionDragStart = (drag, event) => {
      if (props.actionDisabled) return createMessage.info('当前处于查看模式, 不能进行操作');
      if (!props.isMoveMode) {
        createMessage.info('当前处于绘图模式，不能移动标注框');
        return;
      }

      // 使用 offset 函数获取缩放后的屏幕坐标
      const pos = offset(props.annotate);
      const start = {
        x: pos.x,
        y: pos.y,
      };
      const end = {
        x: pos.x + pos.width,
        y: pos.y + pos.height,
      };

      // 更新本地 state 为屏幕坐标
      Object.assign(state, {
        start,
        end,
        extent: {
          x0: start.x,
          y0: start.y,
          x1: end.x,
          y1: end.y,
        },
      });

      const transformState = {
        start,
        end,
      };

      // 回调
      if (typeof onDragStart === 'function') {
        onDragStart(transformState, props.annotate, event);
      }
    };

    const selectionDragMove = (drag) => {
      if (props.actionDisabled || !props.isMoveMode) return;
      const { zoom } = getZoom();
      updateBrush(
        (prevBrush) => {
          const { x: x0, y: y0 } = prevBrush.start;
          const { x: x1, y: y1 } = prevBrush.end;
          // 位置比较计算
          const _scale = zoom;
          const validDx =
            drag.dx > 0
              ? Math.min(drag.dx / _scale, prevBrush.bounds.x1 - x1)
              : Math.max(drag.dx / _scale, prevBrush.bounds.x0 - x0);

          const validDy =
            drag.dy > 0
              ? Math.min(drag.dy / _scale, prevBrush.bounds.y1 - y1)
              : Math.max(drag.dy / _scale, prevBrush.bounds.y0 - y0);
          return {
            ...prevBrush,
            isBrushing: true,
            extent: {
              ...prevBrush.extent,
              x0: x0 + validDx,
              x1: x1 + validDx,
              y0: y0 + validDy,
              y1: y1 + validDy,
            },
            drag: {
              ...drag,
              validDx,
              validDy,
            },
          };
        },
        (nextState) => {
          if (typeof onDragMove === 'function') {
            onDragMove(nextState, props.annotate);
          }
        },
      );
    };

    const selectionDragEnd = (state, event, options = {}) => {
      if (props.actionDisabled || !props.isMoveMode) return;
      const { prevState } = options;
      // fix 双击触发移动选框
      if (!prevState.isMoving) return;
      updateBrush(
        (prevBrush) => {
          const nextBrush = {
            ...prevBrush,
            isBrushing: false,
            start: {
              ...prevBrush.start,
              x: Math.min(prevBrush.extent.x0, prevBrush.extent.x1),
              y: Math.min(prevBrush.extent.y0, prevBrush.extent.y1),
            },
            end: {
              ...prevBrush.end,
              x: Math.max(prevBrush.extent.x0, prevBrush.extent.x1),
              y: Math.max(prevBrush.extent.y0, prevBrush.extent.y1),
            },
          };

          return nextBrush;
        },
        (nextState) => {
          // 回调
          if (typeof onDragEnd === 'function') {
            onDragEnd(nextState, props.annotate);
          }
        },
      );
    };

    watch(
      () => [props.bounds, props.scale],
      ([nextBounds, nextScale]) => {
        if (!isEmpty(nextBounds)) {
          Object.assign(state, {
            bounds: {
              x0: 0,
              x1: nextBounds.width * nextScale,
              y0: 0,
              y1: nextBounds.height * nextScale,
            },
          });
        }
      },
    );

    return {
      state,
      updateBrush,
      updateBrushHandler,
      updateBrushHandlerEnd,
      brushHandlerStart,
      handles,
      corners,
      getExtent,
      selectionDragStart,
      selectionDragMove,
      selectionDragEnd,
      handleContextMenu,
    };
  },
  render() {
    const {
      annotate = {},
      scale,
      brush,
      handleSize,
      transformer,
      currentAnnotationId,
      isMoveMode,
    } = this;

    // 动态计算手柄位置和大小
    const handles = this.handles();
    const corners = this.corners();

    const pos = this.offset(annotate);

    const bboxProps = {
      ...this.$attrs,
      annotate,
      pos,
      transformer,
      currentAnnotationId,
    };

    const dragProps = {
      onDragStart: this.selectionDragStart,
      onDragMove: this.selectionDragMove,
      onDragEnd: this.selectionDragEnd,
      resetOnStart: true,
      width: this.svg.width,
      height: this.svg.height,
    };

    return (
      <Drag {...dragProps} key={annotate.id}>
        {(draw) => {
          // 确保在移动模式下允许鼠标事件，特别是右键点击事件
          const style = {
            pointerEvents: isMoveMode ? 'all' : 'none',
          };

          const _props = {
            ...bboxProps,
            ...draw,
            brush: this.state,
            style,
            isMoveMode,
            scale: this.scale,
          };

          const Handles = Object.keys(handles).map((handleKey) => {
            const handle = handles[handleKey];
            return (
              <BrushHandle
                key={`handle-${handleKey}`}
                type={handleKey}
                handle={handle}
                scale={scale}
                stageWidth={this.svg.width}
                stageHeight={this.svg.height}
                handleBrushStart={this.brushHandlerStart}
                updateBrush={this.updateBrushHandler}
                updateBrushEnd={this.updateBrushHandlerEnd}
                getZoom={this.getZoom}
              />
            );
          });

          const Corners = Object.keys(corners).map((cornerKey) => {
            const corner = corners[cornerKey];

            return (
              <BrushCorner
                annotate={annotate}
                transformer={transformer}
                currentAnnotationId={currentAnnotationId}
                key={`corner-${cornerKey}`}
                type={cornerKey}
                x={corner.x}
                y={corner.y}
                width={Math.max(2, handleSize / scale)} // 动态调整手柄大小
                height={Math.max(2, handleSize / scale)} // 动态调整手柄大小
                scale={scale}
                stageWidth={this.svg.width}
                stageHeight={this.svg.height}
                handleBrushStart={this.brushHandlerStart}
                updateBrush={this.updateBrushHandler}
                updateBrushEnd={this.updateBrushHandlerEnd}
                getZoom={this.getZoom}
              />
            );
          });

          return (
            <g
              class={isMoveMode ? null : 'drawing-mode-annotation'}
              style={isMoveMode ? { pointerEvents: 'auto' } : { pointerEvents: 'none' }}
            >
              {draw.state.isDragging && (
                <rect
                  width={this.svg.width}
                  height={this.svg.height}
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
                    pointerEvents: 'auto',
                  }}
                />
              )}
              {h(Bbox, {
                ..._props,
                onContextmenu: this.handleContextMenu,
              })}
              <g class="bbox-handles-group">{Handles}</g>
              <g class="bbox-corners-group">{Corners}</g>
            </g>
          );
        }}
      </Drag>
    );
  },
};
