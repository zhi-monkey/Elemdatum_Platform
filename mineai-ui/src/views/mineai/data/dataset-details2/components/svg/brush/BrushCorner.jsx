/** Copyright 2020 Tianshu AI Platform. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================
 */

import Drag from '../../Drag';
import { chroma } from '../../../../../../../utils/dubhe';
import { defaultFill } from '../../../util';

export default {
  name: 'BrushCorner',
  props: {
    annotate: Object,
    transformer: Object,
    currentAnnotationId: String,
    stageWidth: Number,
    stageHeight: Number,
    type: String,
    scale: {
      type: Number,
      default: 1,
    },
    x: Number,
    y: Number,
    width: Number,
    height: Number,
    handleBrushStart: Function,
    updateBrush: Function,
    updateBrushEnd: Function,
    getZoom: Function,
  },

  setup(props) {
    const handleDragStart = (drag, event) => {
      // 开始拖拽是选中当前标注
      if (props.handleBrushStart) {
        props.handleBrushStart(drag, event);
      }
    };

    const handleDragMove = (drag) => {
      if (!drag.isDragging) return;
      const { zoom } = props.getZoom();
      props.updateBrush((prevBrush) => {
        const { start, end } = prevBrush;
        let nextState = {};

        let moveX = 0;
        let moveY = 0;

        // 当图片放大时，需要减小缩放因子的影响
        const _scale = zoom;

        const xMax = Math.max(start.x, end.x);
        const xMin = Math.min(start.x, end.x);
        const yMax = Math.max(start.y, end.y);
        const yMin = Math.min(start.y, end.y);
        
        // 确保边界值不会超出舞台边界
        const stageWidth = props.stageWidth;
        const stageHeight = props.stageHeight;

        switch (props.type) {
          case 'topRight':
            moveX = xMax + drag.dx / _scale;
            moveY = yMin + drag.dy / _scale;
            
            // 确保不超出边界
            moveX = Math.min(Math.max(moveX, 0), stageWidth);
            moveY = Math.max(moveY, 0);

            nextState = {
              ...prevBrush,
              activeHandle: props.type,
              extent: {
                ...prevBrush.extent,
                x0: Math.min(moveX, start.x),
                x1: Math.max(moveX, start.x),
                y0: Math.min(moveY, end.y),
                y1: Math.max(moveY, end.y),
              },
            };
            break;
          case 'topLeft':
            moveX = xMin + drag.dx / _scale;
            moveY = yMin + drag.dy / _scale;
            
            // 确保不超出边界
            moveX = Math.max(moveX, 0);
            moveY = Math.max(moveY, 0);

            nextState = {
              ...prevBrush,
              activeHandle: props.type,
              extent: {
                ...prevBrush.extent,
                x0: Math.min(moveX, end.x),
                x1: Math.max(moveX, end.x),
                y0: Math.min(moveY, end.y),
                y1: Math.max(moveY, end.y),
              },
            };
            break;
          case 'bottomLeft':
            moveX = xMin + drag.dx / _scale;
            moveY = yMax + drag.dy / _scale;
            
            // 确保不超出边界
            moveX = Math.max(moveX, 0);
            moveY = Math.min(moveY, stageHeight);

            nextState = {
              ...prevBrush,
              activeHandle: props.type,
              extent: {
                ...prevBrush.extent,
                x0: Math.min(moveX, end.x),
                x1: Math.max(moveX, end.x),
                y0: Math.min(moveY, start.y),
                y1: Math.max(moveY, start.y),
              },
            };
            break;
          case 'bottomRight':
            moveX = xMax + drag.dx / _scale;
            moveY = yMax + drag.dy / _scale;
            
            // 确保不超出边界
            moveX = Math.min(moveX, stageWidth);
            moveY = Math.min(moveY, stageHeight);
            
            nextState = {
              ...prevBrush,
              activeHandle: props.type,
              extent: {
                ...prevBrush.extent,
                x0: Math.min(moveX, start.x),
                x1: Math.max(moveX, start.x),
                y0: Math.min(moveY, start.y),
                y1: Math.max(moveY, start.y),
              },
            };
            break;
          default:
            break;
        }
        return nextState;
      });
    };

    const handleDragEnd = () => {
      props.updateBrushEnd((prevBrush) => {
        const { extent } = { ...prevBrush };
        const start = {
          x: Math.min(extent.x0, extent.x1),
          y: Math.min(extent.y0, extent.y1),
        };
        const end = {
          x: Math.max(extent.x0, extent.x1),
          y: Math.max(extent.y0, extent.y1),
        };
        const nextBrush = {
          ...prevBrush,
          start,
          end,
          activeHandle: undefined,
          isBrushing: false,
          domain: {
            x0: Math.min(start.x, end.x),
            x1: Math.max(start.x, end.x),
            y0: Math.min(start.y, end.y),
            y1: Math.max(start.y, end.y),
          },
        };
        return nextBrush;
      });
    };

    return {
      handleDragStart,
      handleDragMove,
      handleDragEnd,
    };
  },

  render() {
    const {
      annotate,
      transformer,
      currentAnnotationId,
      stageWidth,
      stageHeight,
      type,
      x,
      y,
      width,
      height,
    } = this;

    const cursor = type === 'topLeft' || type === 'bottomRight' ? 'nwse-resize' : 'nesw-resize';

    let transform = null;
    if (annotate.id === transformer.id) {
      transform = `translate(${transformer.dx}, ${transformer.dy})`;
    }

    const { data = {} } = annotate;
    const { color } = data;
    const bgColor = color || defaultFill;
    const isActive = currentAnnotationId === annotate.id;
    // 修改：即使不是活动状态，也保持一定的透明度，确保用户可以看到和交互
    const colorAlpha = isActive ? 0.8 : 0.4;
    const fillColor = chroma(bgColor).alpha(colorAlpha);

    const dragProps = {
      width: stageWidth,
      height: stageHeight,
      resetOnStart: true,
      onDragStart: this.handleDragStart,
      onDragMove: this.handleDragMove,
      onDragEnd: this.handleDragEnd,
    };

    const style = {
      cursor,
      pointerEvents: 'auto', // 确保角落手柄始终可以接收鼠标事件
      zIndex: 1000, // 增加z-index确保角落手柄在最上层
    };

    return (
      <Drag {...dragProps}>
        {(drag) => (
          <rect
            x={x}
            y={y}
            width={width}
            height={height}
            transform={transform}
            fill={fillColor}
            class={`brush-corner-${type}`}
            onMousedown={drag.dragStart}
            onMousemove={drag.dragMove}
            onMouseup={drag.dragEnd}
            style={style}
          />
        )}
      </Drag>
    );
  },
};
