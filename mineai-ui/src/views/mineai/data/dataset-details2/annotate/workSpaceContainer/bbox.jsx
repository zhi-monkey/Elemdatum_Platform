import cx from 'classnames';
import { isNil } from 'lodash-es';
import { chroma } from '/@/utils/dubhe';

import { defaultColor, defaultFill } from '../../util';

export default {
  // eslint-disable-next-line vue/multi-word-component-names
  name: 'Bbox',
  functional: true,
  props: {
    annotate: Object,
    brush: Object,
    scale: {
      type: Number,
      default: 1,
    },
    pos: {
      type: Object,
      default: () => ({}),
    },
    dragStart: Function,
    dragMove: Function,
    dragEnd: Function,
    currentAnnotationId: String,
    transformer: Object,
    imgRef: HTMLImageElement,
  },
  render(h, context) {
    const { props } = context;
    const { style } = context.data;
    const {
      annotate = {},
      currentAnnotationId,
      dragStart,
      dragMove,
      dragEnd,
      brush,
      transformer,
      ...rest // does this work?
    } = props;
    const { data = {} } = annotate;
    const { bbox, color } = data;

    if (isNil(bbox)) return null;

    const bgColor = color || defaultFill;

    const isActive = currentAnnotationId === annotate.id;
    const colorAlpha = isActive ? 0.4 : 0.1;

    const fill = chroma(bgColor).alpha(colorAlpha);

    let transform = null;
    // 匹配当前标注
    if (annotate.id === transformer.id) {
      transform = `translate(${transformer.dx}, ${transformer.dy})`;
    }

    return (
      <g
        class={cx('bbox-group', {
          active: isActive,
        })}
      >
        <rect
          fill={fill}
          stroke={color || defaultColor}
          strokeWidth={4}
          // {...bounding} spread operator sucks...
          x={props.pos.x}
          y={props.pos.y}
          width={props.pos.width}
          height={props.pos.height}
          transform={transform}
          onMousemove={dragMove}
          onMouseup={dragEnd}
          onMousedown={dragStart}
          style={style}
          {...rest}
        />
      </g>
    );
  },
};
