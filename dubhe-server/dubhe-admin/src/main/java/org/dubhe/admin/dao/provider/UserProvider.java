

package org.dubhe.admin.dao.provider;

/**
 * @description 用户sql构建类
 * @date 2020-04-02
 */
public class UserProvider {
    public String queryPermissionByUserId(Long userId) {
        StringBuffer sql = new StringBuffer("select m.permission from menu m, users_roles ur, roles_menus rm ");
        sql.append(" where ur.user_id = #{userId} and ur.role_id = rm.role_id and rm.menu_id = m.id and m.permission <> '' and m.deleted = 0 ");
        return sql.toString();
    }

    public String findPermissionByUserIdAndTeamId(Long userId, Long teamId) {
        StringBuffer sql = new StringBuffer("select m.permission from menu m, teams_users_roles tur ,roles_menus rm ");
        sql.append(" where tur.user_id=#{userId} ");
        sql.append(" and tur.role_id=rm.role_id ");
        sql.append(" and tur.team_id=#{team_id} ");
        sql.append(" and rm.menu_id=m.id");
        sql.append(" and  and m.deleted = 0 ");
        return sql.toString();
    }

    public String findByTeamId(Long teamId) {
        StringBuffer sql = new StringBuffer("select u.* from user u,teams_users_roles tur where tur.team_id=#{teamId} and tur.user_id=u.id  and u.deleted = 0");
        return sql.toString();
    }
}
