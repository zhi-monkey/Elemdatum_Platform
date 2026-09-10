import { isNil } from 'lodash-es';
import { addSuffix, toFixed } from '/@/utils/dubhe';

export default {
  name: 'BrushTip',
  props: {
    brush: Object,
    dimension: Object,
    imageOffset: Object,
    zoom: Number,
  },
  setup(props) {
    // 直接获取刷子的范围
    const getExtent = () => props.brush.extent;

    // 直接获取刷子结束点的相对坐标（已在handleBrushEnd中计算）
    const getEndPoint = () => {
      const { extent = {} } = props.brush;
      return {
        x: extent.x1,
        y: extent.y1,
      };
    };

    // 获取相对尺寸（已在handleBrushEnd中计算）
    const getSize = () => {
      const extent = getExtent();
      if (isNil(extent)) return { width: 0, height: 0 };

      return {
        width: Math.abs(extent.x1 - extent.x0),
        height: Math.abs(extent.y1 - extent.y0),
      };
    };

    return {
      getExtent,
      getEndPoint,
      getSize,
      toFixed,
    };
  },
  render() {
    const extent = this.getExtent();
    if (!extent) return null;

    const { width, height } = this.getSize();
    const endPoint = this.getEndPoint();
    const { svg } = this.dimension;

    // 计算显示位置
    const sizeTipStyle = {
      left: addSuffix(extent.x0),
      top: addSuffix(extent.y0 - 30),
      padding: 0,
    };

    const dimensionTipStyle = {
      right: addSuffix(svg.width - extent.x1),
      top: addSuffix(extent.y1 + 6),
      padding: 0,
    };

    // 到上边缘
    if (extent.y0 < 30) {
      sizeTipStyle.top = addSuffix(extent.y0 + 6);
    }

    return (
      <div class="usn">
        <div
          class="brush-tooltip size-tipper"
          style={{ ...sizeTipStyle, transformOrigin: 'top left' }}
        >
          <div
            class="tooltip-content"
            style={{
              fontSize: '10px',
              padding: '2px 8px',
              borderRadius: '4px',
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
            }}
          >
            {width > 0 && height > 0 && (
              <div class="tooltip-item-row">
                {this.toFixed(width, 0, 0)} * {this.toFixed(height, 0, 0)}
              </div>
            )}
          </div>
        </div>
        <div
          class="brush-tooltip dimension-tipper"
          style={{ ...dimensionTipStyle, transformOrigin: 'top right' }}
        >
          <div
            class="tooltip-content"
            style={{
              fontSize: '10px',
              padding: '2px 8px',
              borderRadius: '4px',
              backgroundColor: 'rgba(0, 0, 0, 0.8)',
              transformOrigin: 'top right',
            }}
          >
            {endPoint && (
              <div class="tooltip-item-row">
                ({this.toFixed(endPoint.x, 0, 0)}, {this.toFixed(endPoint.y, 0, 0)})
              </div>
            )}
          </div>
        </div>
      </div>
    );
  },
};
