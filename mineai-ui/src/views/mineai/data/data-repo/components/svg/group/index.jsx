import cx from 'classnames';
//
// export default {
//   // eslint-disable-next-line vue/multi-word-component-names
//   name: 'Group',
//   functional: true,
//   render(h, context) {
//     const { props, children } = context;
//     const { top = 0, left = 0, transform, className, ...otherProps } = props;
//
//     return (
//       <g
//         class={cx('db-group', className)}
//         transform={transform || `translate(${left}, ${top})`}
//         {...otherProps}
//       >
//         {children}
//       </g>
//     );
//   },
// };

import { h } from 'vue';

const Group = (props, context) => {
  const { top = 0, left = 0, transform, className, ...otherProps } = props;

  // return h(`h${props.level}`, context.attrs, context.slots)
  // console.log('Group', context, props);
  return h(
    <g
      class={cx('db-group', className)}
      transform={transform || `translate(${left}, ${top})`}
      {...otherProps}
    >
      {context.slots.default()}
    </g>,
  );
};

export default Group;
