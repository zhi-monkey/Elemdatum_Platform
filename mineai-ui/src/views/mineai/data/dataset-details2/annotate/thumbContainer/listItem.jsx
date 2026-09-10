import cx from 'classnames';
import { defineComponent } from 'vue';

// export default {
//   name: 'ListItem',
//   functional: true,
//   render: (h, context) => {
//     const { props } = context;
//     const { item, handleClick, currentImgId } = props;
//     const classnames = cx('thumb-list-item', {
//       active: currentImgId === item.id,
//     });
//     return (
//       <li
//         class={classnames}
//         style={{
//           backgroundImage: `url("${item.url}")`,
//         }}
//         onClick={() => handleClick(item)}
//       ></li>
//     );
//   },
// };

export default defineComponent((props) => {
  const { item, handleClick, currentImgId } = props;
  const classnames = cx('thumb-list-item', {
    active: currentImgId === item.id,
  });
  return () => (
    <li
      class={classnames}
      style={{
        backgroundImage: `url("${item.url}")`,
      }}
      onClick={() => handleClick(item)}
    ></li>
  );
});
