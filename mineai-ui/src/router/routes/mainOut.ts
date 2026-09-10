/**
The routing of this file will not show the layout.
It is an independent new page.
the contents of the file still need to log in to access
 */
import type { AppRouteModule } from '/@/router/types';

import { LAYOUT } from '/@/router/constant';

// test
// http:ip:port/main-out
export const mainOutRoutes: AppRouteModule[] = [
  {
    path: '/main-out',
    name: 'MainOut',
    component: () => import('/@/views/demo/main-out/index.vue'),
    meta: {
      title: 'MainOut',
      ignoreAuth: true,
    },
  },
  // 点云标注独立路由：后端菜单通常未登记该页面（无菜单项时按钮不可达），
  // 故在此静态兜底注册；但必须包裹在布局组件（LAYOUT）下，与图片标注 /maData/annotate
  // 保持一致的页面外壳（保留系统左侧菜单栏、顶栏），否则会退化成无侧边栏的全屏页面。
  {
    path: '/maData/pointCloudAnnotate',
    name: 'PointCloudAnnotate',
    component: LAYOUT,
    meta: {
      title: '点云 3D Box 标注',
      ignoreAuth: false,
    },
    children: [
      {
        path: ':id?/:name?',
        name: 'PointCloudAnnotatePage',
        component: () => import('/@/views/mineai/data/dataset-details2/pointCloudAnnotate/index.vue'),
        meta: {
          title: '点云 3D Box 标注',
          ignoreAuth: false,
        },
      },
    ],
  },
];

export const mainOutRouteNames = mainOutRoutes.map((item) => item.name);
