import { maHttp } from '/@/utils/http/axios';
import { getMenuListResultModel } from './model/menuModel';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

enum Api {
  GetMenuList = 'user/menus',
}

/**
 * @description: Get user menu based on id
 */

export const getMenuList = () => {
  return maHttp.get<getMenuListResultModel>(
    {
      url: Api.GetMenuList,
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN,
    },
  );
};
