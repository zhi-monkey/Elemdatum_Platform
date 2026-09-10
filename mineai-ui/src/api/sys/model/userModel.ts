/**
 * 登录的接口参数
 */
export interface LoginParams {
  username: string;
  password: string;
}

/**
 * 用户的角色信息
 */
export interface RoleInfo {
  id?: number
  roleName: string;
  value: string;
}

/**
 * @description: Login interface return value
 */
export interface LoginResultModel {
  user: string | number;
  token: string;
  role: RoleInfo;
}

export interface DubheLoginResultModel {
  user: any;
  token: string;
  permissions: string[];
}

/**
 * @description: Get user information return value
 */
export interface GetUserInfoModel {
  roles?: RoleInfo[];
  // 用户id
  userId: string | number;
  // 用户名
  username: string;
  // 真实名字
  realName?: string;
  // 头像
  avatar?: string;
  // 介绍
  desc?: string;
}
