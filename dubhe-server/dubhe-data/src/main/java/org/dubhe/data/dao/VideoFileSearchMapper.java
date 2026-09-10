package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.data.domain.dto.VideoFileSearchDTO;
import org.dubhe.data.domain.vo.VideoFileSearchResultVO;

import java.util.Set;

/**
 * @description 视频文件检索 Mapper
 * @date 2026-08-27
 */
public interface VideoFileSearchMapper {

    /**
     * 跨数据集多条件检索视频文件
     *
     * @param page            分页参数
     * @param query           检索条件
     * @param resourceUserIds 可见资源用户ID集合（null 表示管理员，不过滤）
     * @param orderBy         排序（服务端白名单解析后传入）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT vf.id AS id, vf.name AS name, vf.url AS url, vf.dataset_id AS datasetId, " +
            "vd.name AS datasetName, vf.file_size AS fileSize, " +
            "DATE_FORMAT(COALESCE(vf.update_time, vf.create_time), '%Y-%m-%d %H:%i:%s') AS updateTime " +
            "FROM video_dataset_file vf " +
            "INNER JOIN video_dataset vd ON vd.id = vf.dataset_id AND vd.deleted = 0 " +
            "<where>" +
            "  vf.deleted = 0 " +
            "  <if test='resourceUserIds != null and resourceUserIds.size() > 0'> AND vf.create_user_id IN <foreach collection='resourceUserIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach></if>" +
            "  <if test='query.datasetIds != null and query.datasetIds.size() > 0'> AND vf.dataset_id IN <foreach collection='query.datasetIds' item='did' open='(' separator=',' close=')'>#{did}</foreach></if>" +
            "  <if test='query.name != null and query.name != \"\"'> AND vf.name LIKE CONCAT(#{query.name}, '%')</if>" +
            "  <if test='query.updateTimeStart != null and query.updateTimeStart != \"\"'> AND vf.update_time &gt;= #{query.updateTimeStart}</if>" +
            "  <if test='query.updateTimeEnd != null and query.updateTimeEnd != \"\"'> AND vf.update_time &lt;= #{query.updateTimeEnd}</if>" +
            "</where>" +
            "ORDER BY ${orderBy}" +
            "</script>")
    IPage<VideoFileSearchResultVO> search(Page<VideoFileSearchResultVO> page,
                                          @Param("query") VideoFileSearchDTO query,
                                          @Param("resourceUserIds") Set<Long> resourceUserIds,
                                          @Param("orderBy") String orderBy);
}
