import { MockMethod } from 'vite-plugin-mock';
import { resultError, resultSuccess, getRequestToken, requestParams } from '../_util';

export function createFakeUserList() {
  return [
    {
      userId: '1',
      username: 'user1',
      realName: '测试用户',
      avatar: 'https://q1.qlogo.cn/g?b=qq&nk=190848757&s=640',
      desc: 'manager',
      password: '123456',
      token: 'fakeToken1',
      homePath: '/maSystem/info',
      roles: [
        {
          roleName: 'Super Admin',
          value: 'super',
        },
      ],
    },
    {
      userId: '2',
      username: 'test',
      password: '123456',
      realName: 'test user',
      avatar: 'https://q1.qlogo.cn/g?b=qq&nk=339449197&s=640',
      desc: 'tester',
      token: 'fakeToken2',
      homePath: '/maSystem/info',
      roles: [
        {
          roleName: 'Super Admin',
          value: 'super',
        },
      ],
    },
    {
      userId: '3',
      username: 'system',
      realName: '测试-系统服务',
      avatar: 'https://q1.qlogo.cn/g?b=qq&nk=190848757&s=640',
      desc: 'manager',
      password: '123456',
      token: 'fakeToken3',
      homePath: '/maSystem/info',
      roles: [
        {
          roleName: 'SystemManger',
          value: 'system:manager',
        },
        {
          roleName: 'SystemInfo',
          value: 'system:info',
        },
        {
          roleName: 'SystemUser',
          value: 'system:user',
        },
        {
          roleName: 'SystemRole',
          value: 'system:role',
        },
        {
          roleName: 'SystemMenu',
          value: 'system:menu',
        },
        {
          roleName: 'SystemDepartment',
          value: 'system:department',
        },
      ],
    },
    {
      userId: '4',
      username: 'guest',
      realName: '访客用户',
      avatar: 'https://q1.qlogo.cn/g?b=qq&nk=190848757&s=640',
      desc: 'manager',
      password: '123456',
      token: 'fakeToken4',
      homePath: '/maSystem/info',
      roles: [
        {
          roleName: 'SystemManger',
          value: 'system:manager',
        },
        {
          roleName: 'SystemInfo',
          value: 'system:info',
        },
        {
          roleName: 'ControllerManager',
          value: 'controller:manager',
        },
        {
          roleName: 'ControllerInfo',
          value: 'controller:controller-info',
        },
        {
          roleName: 'DataManager',
          value: 'data:manager',
        },
        {
          roleName: 'DataInfo',
          value: 'data:info',
        },
        {
          roleName: 'DatasetExplorer',
          value: 'data:DataDatasetExplorer',
        },
        {
          roleName: 'DataUpload',
          value: 'data:upload',
        },
        {
          roleName: 'DataLabeling',
          value: 'data:labeling',
        },
        {
          roleName: 'MonitorManager',
          value: 'monitor:manager',
        },
        {
          roleName: 'MonitorHome',
          value: 'monitor:monitorHome',
        },
        {
          roleName: 'MonitorInfo',
          value: 'monitor:monitorInfo',
        },
        {
          roleName: 'ElectricFence',
          value: 'monitor:ElectricFence',
        },
        {
          roleName: 'WorkerManager',
          value: 'worker:manager',
        },
        {
          roleName: 'WorkerIndex',
          value: 'worker:index',
        },
      ],
    },
  ];
}

const fakeCodeList: any = {
  '1': ['1000', '3000', '5000'],

  '2': ['2000', '4000', '6000'],
};
export default [
  // mock user login
  {
    url: '/basic-api/login',
    timeout: 200,
    method: 'post',
    response: ({ body }) => {
      const { username, password } = body;
      const checkUser = createFakeUserList().find(
        (item) => item.username === username && password === item.password,
      );
      if (!checkUser) {
        return resultError('Incorrect account or password！');
      }
      const { userId, username: _username, token, realName, desc, roles } = checkUser;
      return resultSuccess({
        roles,
        userId,
        username: _username,
        token,
        realName,
        desc,
      });
    },
  },
  {
    url: '/basic-api/getUserInfo',
    method: 'get',
    response: (request: requestParams) => {
      const token = getRequestToken(request);
      if (!token) return resultError('Invalid token');
      const checkUser = createFakeUserList().find((item) => item.token === token);
      if (!checkUser) {
        return resultError('The corresponding user information was not obtained!');
      }
      return resultSuccess(checkUser);
    },
  },
  {
    url: '/basic-api/getPermCode',
    timeout: 200,
    method: 'get',
    response: (request: requestParams) => {
      const token = getRequestToken(request);
      if (!token) return resultError('Invalid token');
      const checkUser = createFakeUserList().find((item) => item.token === token);
      if (!checkUser) {
        return resultError('Invalid token!');
      }
      const codeList = fakeCodeList[checkUser.userId];

      return resultSuccess(codeList);
    },
  },
  {
    url: '/basic-api/logout',
    timeout: 200,
    method: 'get',
    response: (request: requestParams) => {
      const token = getRequestToken(request);
      if (!token) return resultError('Invalid token');
      const checkUser = createFakeUserList().find((item) => item.token === token);
      if (!checkUser) {
        return resultError('Invalid token!');
      }
      return resultSuccess(undefined, { message: 'Token has been destroyed' });
    },
  },
] as MockMethod[];
