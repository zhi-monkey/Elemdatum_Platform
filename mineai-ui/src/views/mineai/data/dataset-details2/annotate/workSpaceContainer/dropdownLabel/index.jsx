import BasicTooltip from '/@/hooks/dubhe/tooltip/BasicTooltip.jsx';
import './style.scss';
// import { defineComponent } from 'vue';
//
// const DropDownLabel = defineComponent(
//   (props, ctx) => {
//     const { visible, position, hideTooltip, value, handleChange, labels = [] } = props;
//     return () => (
//       <BasicTooltip
//         class="dropdown-label"
//         visible={visible}
//         position={position}
//         hideTooltip={hideTooltip}
//       >
//         <el-select
//           value={value}
//           key={value} // fix: ele-select 每次创建完毕会保留 id
//           onChange={handleChange}
//           filterable
//           allow-create
//           default-first-option
//           placeholder="请选择标签"
//         >
//           {labels.map((label) => (
//             <el-option key={label.id} label={label.name} value={label.id}></el-option>
//           ))}
//         </el-select>
//       </BasicTooltip>
//     );
//   },
//   {
//     // eslint-disable-next-line vue/require-prop-types
//     props: ['visible', 'position', 'hideTooltip', 'value', 'handleChange', 'labels'],
//   },
// );
//
// export default DropDownLabel;

import { h } from 'vue';

const DropDownLabel = (props, context) => {
  const { position, data, value, handleChange, labels = [] } = props;
  // return h(`h${props.level}`, context.attrs, context.slots)
  // console.log('Group', context, props);
  return h(
    <BasicTooltip class="dropdown-label" position={position}>
      <el-select
        model-value={data ? data : value}
        key={value} // fix: ele-select 每次创建完毕会保留 id
        onChange={handleChange}
        filterable
        allow-create
        default-first-option
        placeholder="请选择标签"
      >
        {labels.map((label) => (
          <el-option key={label.id} label={label.name} value={label.id}></el-option>
        ))}
      </el-select>
    </BasicTooltip>,
  );
};

export default DropDownLabel;
