

package org.dubhe.admin.dao.provider;

/**
 * @description  团队构建类
 * @date 2020-04-15
 */
public class TeamProvider {

  public String findByUserId(Long userId) {
    StringBuffer sql = new StringBuffer("select t.* from team t, teams_users_roles tur ");
    sql.append(" where tur.user_id=#{userId} ");
    sql.append(" and tur.team_id=t.id ");
    return sql.toString();
  }
}
