// 基础 Brush 组件，用于在手动标注页面框选标注
import cx from 'classnames';

export default function render(_props, _context) {
  const context = {
    ..._context,
    props: _props,
    data: _context.attr,
    children: _context.slots,
  };
  const { props } = context;
  const {
    brush,
    className,
    fill = 'rgba(102, 181, 245, 0.1)',
    stroke = 'rgba(102, 181, 245, 1)',
    strokeWidth = 1,
    ...otherProps
  } = props;

  const { start, end, isBrushing } = brush;
  if (!start) return null;
  if (!end) return null;
  const x = end.x > start.x ? start.x : end.x;
  const y = end.y > start.y ? start.y : end.y;
  const width = Math.abs(start.x - end.x);
  const height = Math.abs(start.y - end.y);

  return (
    <g className={cx('basic-brush', className)}>
      {isBrushing && (
        <rect
          fill={fill}
          stroke={stroke}
          strokeWidth={strokeWidth}
          x={x}
          y={y}
          width={width}
          height={height}
          {...otherProps}
        />
      )}
    </g>
  );
}
