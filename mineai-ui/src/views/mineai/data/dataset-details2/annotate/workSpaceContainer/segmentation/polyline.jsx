import { isNil, isFunction } from 'lodash-es';
import { line as d3Line } from 'd3/dist/d3';
import { defaultColor } from '../../../util';

export default {
  name: 'PolylineRender',
  functional: true,
  props: {
    points: Array,
    curve: Function,
    fill: String,
    offset: Function,
  },
  render(h, context) {
    const { props } = context;
    const { fill, points = [], offset } = props;

    if (isNil(points)) return null;

    const { style } = context.data;
    const line = d3Line()
      .x((d) => d.x)
      .y((d) => d.y);

    if (props.curve) {
      line.curve(props.curve);
    }

    const bgColor = fill || defaultColor;

    const restProps = {
      attrs: context.data.attrs,
      on: context.data.on,
    };

    const nextData = isFunction(offset) ? points.map(offset) : points;

    return (
      <path
        class="interactive"
        d={line(nextData)}
        stroke={bgColor}
        fill={bgColor}
        stroke-width={2}
        {...restProps}
        style={style}
      />
    );
  },
};
