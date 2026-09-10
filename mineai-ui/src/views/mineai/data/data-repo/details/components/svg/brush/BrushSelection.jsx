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
  },

  render() {
    const { width, height, brush, disableDraggingSelection, selectionStyle } = this;

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
