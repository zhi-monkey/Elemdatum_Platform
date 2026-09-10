import { defHttp, maHttp } from '/@/utils/http/axios';
import { LoginParams, DubheLoginResultModel } from './model/userModel';
import { encrypt } from '/@/utils/dubhe/rsaEncrypt';
import { ErrorMessageMode } from '/#/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

enum Api {
  Login = '/login',
  RealLogin = 'ss/auth/login',
  RealGetUserInfo = 'ss/user/getUserInfo',
  Logout = 'ss/auth/logout',
  GetUserInfo = 'user/info',
  GetPermCode = '/getPermCode',
}

/**
 * @description: user login api
 */
export async function loginApi(params: LoginParams, mode: ErrorMessageMode = 'modal') {
  return maHttp.post<DubheLoginResultModel>(
    {
      url: `auth/login`,
      params: { username: params.username, password: encrypt(params.password) },
    },
    {
      errorMessageMode: mode,
      urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN,
    },
  );
}

/**
 * @description: getUserInfo
 */
export const getUserInfo = () => {
  return maHttp.get(
    {
      url: Api.GetUserInfo,
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN,
    },
  );
};

export function getPermCode() {
  return defHttp.get<string[]>({ url: Api.GetPermCode });
}

export function doLogout() {
  return maHttp.delete(
    {
      url: `auth/logout`,
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN,
    },
  );
}
