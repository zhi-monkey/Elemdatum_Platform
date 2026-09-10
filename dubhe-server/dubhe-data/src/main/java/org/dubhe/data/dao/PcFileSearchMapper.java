package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.data.domain.dto.PcFileSearchDTO;
import org.dubhe.data.domain.vo.PcFileSearchResultVO;

import java.util.Set;

/**
 * @description 点云文件检索 Mapper
 * @date 2026-08-27
 */
public interface PcFileSearchMapper {

    /**
     * 跨数据集多条件检索点云文件
     *
     * @param page            分页参数
     * @param query           检索条件
     * @param resourceUserIds 可见资源用户ID集合（null 表示管理员，不过滤）
     * @param orderBy         排序（服务端白名单解析后传入）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT pf.id AS id, pf.name AS name, pf.url AS url, pf.dataset_id AS datasetId, " +
            "pd.name AS datasetName, pf.file_size AS fileSize, pf.point_count AS pointCount, " +
            "DATE_FORMAT(COALESCE(pf.update_time, pf.create_time), '%Y-%m-%d %H:%i:%s') AS updateTime " +
            "FROM pc_dataset_file pf " +
            "INNER JOIN pc_dataset pd ON pd.id = pf.dataset_id AND pd.deleted = 0 " +
            "<where>" +
            "  pf.deleted = 0 " +
            "  <if test='resourceUserIds != null and resourceUserIds.size() > 0'> AND pf.create_user_id IN <foreach collection='resourceUserIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach></if>" +
            "  <if test='query.datasetIds != null and query.datasetIds.size() > 0'> AND pf.dataset_id IN <foreach collection='query.datasetIds' item='did' open='(' separator=',' close=')'>#{did}</foreach></if>" +
            "  <if test='query.name != null and query.name != \"\"'> AND pf.name LIKE CONCAT(#{query.name}, '%')</if>" +
            "  <if test='query.updateTimeStart != null and query.updateTimeStart != \"\"'> AND pf.update_time &gt;= #{query.updateTimeStart}</if>" +
            "  <if test='query.updateTimeEnd != null and query.updateTimeEnd != \"\"'> AND pf.update_time &lt;= #{query.updateTimeEnd}</if>" +
            "</where>" +
            "ORDER BY ${orderBy}" +
            "</script>")
    IPage<PcFileSearchResultVO> search(Page<PcFileSearchResultVO> page,
                                       @Param("query") PcFileSearchDTO query,
                                       @Param("resourceUserIds") Set<Long> resourceUserIds,
                                       @Param("orderBy") String orderBy);
}
