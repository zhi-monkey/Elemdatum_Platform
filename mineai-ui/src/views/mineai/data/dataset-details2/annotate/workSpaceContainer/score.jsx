import { isNil } from 'lodash-es';
import { addSuffix, colorByLuminance, chroma } from '/@/utils/dubhe';

import { defaultColor } from '../../util';

// 分数最小宽度
const MinWidth = 48;

export default {
  name: 'Score',
  functional: true,
  props: {
    annotate: Object,
    offset: Function,
    transformer: Object,
    brush: Object,
    currentAnnotationId: String,
  },
  render(h, context) {
    const { props } = context;
    const { annotate = {}, offset, transformer, brush } = props;

    const { data = {}, id } = annotate;
    const { bbox, color = defaultColor, score = 1 } = data;

    // 当前在拖拽中不展示
    if (props.currentAnnotationId === id && brush.isBrushing) return null;

    if (isNil(bbox)) return null;
    const pos = offset(props.annotate);

    const style = {
      width: addSuffix(pos.width),
      left: addSuffix(pos.x + Math.min((pos.width - MinWidth) / 2, 0)),
      top: addSuffix(Math.max(pos.y - 30, 0)),
      minWidth: addSuffix(MinWidth),
    };

    // 匹配当前标注
    if (annotate.id === transformer.id) {
      style.transform = `translate(${transformer.dx}px, ${transformer.dy}px)`;
    }

    const boxStyle = {
      backgroundColor: chroma(color).alpha(0.8),
      color: colorByLuminance(color),
    };

    return (
      <div class="annotation-score-row tc" style={style}>
        <span class="score" style={boxStyle}>
          {Math.floor(score * 100)}
          <span class="unit">分</span>
        </span>
      </div>
    );
  },
};
