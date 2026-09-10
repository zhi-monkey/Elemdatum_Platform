
package org.dubhe.cloud.authconfig.service;


import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.vo.DataResponseBody;

/**
 * @description Demo服务接口
 * @date 2020-11-26
 */
public interface AdminUserService {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名称
     * @return 用户信息
     */
    DataResponseBody<UserContext> findUserByUsername(String username);



}
