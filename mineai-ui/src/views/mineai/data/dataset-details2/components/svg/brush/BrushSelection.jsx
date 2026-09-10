export default {
  name: 'BrushSelection',
  props: {
    stageWidth: Number,
    stageHeight: Number,
    width: Number,
    height: Number,
    updateBrush: Function,
    brush: Object,
    onBrushStart: Function,
    onBrushEnd: Function,
    disableDraggingSelection: {
      type: Boolean,
      default: false,
    },
    selectionStyle: {
      type: Object,
    },
    imageOffset: {
      type: Object,
      default: () => ({ x: 0, y: 0 }),
    },
  },

  render() {
    const { width, height, brush, disableDraggingSelection, selectionStyle, imageOffset } = this;
    
    // 不需要在这里调整坐标，因为brush.extent已经是相对于画布的坐标
    // 在绘制时保持原始坐标，确保选择框与鼠标位置一致

    return (
      <rect
        x={Math.min(brush.extent.x0, brush.extent.x1)}
        y={Math.min(brush.extent.y0, brush.extent.y1)}
        width={width}
        height={height}
        className="db-brush-selection"
        style={{
          ...selectionStyle,
          pointerEvents: brush.isBrushing || brush.activeHandle ? 'none' : 'all',
          cursor: disableDraggingSelection ? null : 'move',
        }}
      />
    );
  },
};
