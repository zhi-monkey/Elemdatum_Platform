package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.data.domain.dto.FileSearchDTO;
import org.dubhe.data.domain.entity.LabelTemplate;
import org.dubhe.data.domain.vo.FileSearchResultVO;

import java.util.List;
import java.util.Set;

/**
 * @description 图片多条件检索 Mapper
 * @date 2026-08-26
 */
public interface FileSearchMapper {

    /**
     * 跨数据集多条件检索图片
     *
     * @param page            分页参数
     * @param query           检索条件
     * @param resourceUserIds 可见资源用户ID集合（null 表示管理员，不过滤）
     * @param orderBy         排序（服务端白名单解析后传入）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT f.id AS id, f.name AS name, f.url AS url, f.width AS width, f.height AS height, " +
            "f.file_type AS fileType, f.dataset_id AS datasetId, " +
            "DATE_FORMAT(COALESCE(f.update_time, f.create_time), '%Y-%m-%d %H:%i:%s') AS updateTime, " +
            "d.name AS datasetName, " +
            "(SELECT dvf.annotation_status FROM data_dataset_version_file dvf " +
            "  WHERE dvf.dataset_id = f.dataset_id AND dvf.file_id = f.id " +
            "  AND dvf.status IN (0,2) " +
            "  AND COALESCE(NULLIF(dvf.version_name, ''), '__none__') = COALESCE(NULLIF(d.current_version_name, ''), '__none__') " +
            "  ORDER BY dvf.id DESC LIMIT 1) AS annotationStatus, " +
            "(SELECT GROUP_CONCAT(DISTINCT l.name SEPARATOR ',') FROM data_file_annotation a INNER JOIN data_dataset_version_file vf ON vf.id = a.version_file_id INNER JOIN data_label l ON l.id = a.label_id WHERE vf.file_id = f.id AND vf.dataset_id = f.dataset_id AND vf.status IN (0,2) AND COALESCE(NULLIF(vf.version_name, ''), '__none__') = COALESCE(NULLIF(d.current_version_name, ''), '__none__') AND a.deleted = 0) AS labels, " +
            "m.source_type AS sourceType, DATE_FORMAT(m.capture_time, '%Y-%m-%d %H:%i:%s') AS captureTime, " +
            "m.device AS device, m.device_sn AS deviceSn, m.location AS location, " +
            "m.scenario AS scenario, m.lighting AS lighting, m.quality AS quality " +
            "FROM data_file f " +
            "INNER JOIN data_dataset d ON d.id = f.dataset_id AND d.deleted = 0 " +
            "LEFT JOIN data_file_metadata m ON m.file_id = f.id " +
            "<if test='query.datasetGroupId != null'>" +
            "  INNER JOIN dataset_dataset_group ddg ON ddg.dataset_id = f.dataset_id AND ddg.dataset_group_id = #{query.datasetGroupId} " +
            "</if>" +
            "<where>" +
            "  f.deleted = 0 AND (f.file_type = 0 OR f.file_type IS NULL) " +
            "  AND EXISTS (SELECT 1 FROM data_dataset_version_file dvf0 WHERE dvf0.file_id = f.id AND dvf0.dataset_id = f.dataset_id AND dvf0.status IN (0,2) AND COALESCE(NULLIF(dvf0.version_name, ''), '__none__') = COALESCE(NULLIF(d.current_version_name, ''), '__none__')) " +
            "  <if test='resourceUserIds != null and resourceUserIds.size() > 0'> AND f.create_user_id IN <foreach collection='resourceUserIds' item='uid' open='(' separator=',' close=')'>#{uid}</foreach></if>" +
            "  <if test='query.datasetIds != null and query.datasetIds.size() > 0'> AND f.dataset_id IN <foreach collection='query.datasetIds' item='did' open='(' separator=',' close=')'>#{did}</foreach></if>" +
            "  <if test='query.fileIds != null and query.fileIds.size() > 0'> AND f.id IN <foreach collection='query.fileIds' item='fid' open='(' separator=',' close=')'>#{fid}</foreach></if>" +
            "  <if test='query.name != null and query.name != \"\"'> AND f.name LIKE CONCAT(#{query.name}, '%')</if>" +
            "  <if test='query.minWidth != null'> AND f.width &gt;= #{query.minWidth}</if>" +
            "  <if test='query.maxWidth != null'> AND f.width &lt;= #{query.maxWidth}</if>" +
            "  <if test='query.minHeight != null'> AND f.height &gt;= #{query.minHeight}</if>" +
            "  <if test='query.maxHeight != null'> AND f.height &lt;= #{query.maxHeight}</if>" +
            "  <if test='query.updateTimeStart != null and query.updateTimeStart != \"\"'> AND f.update_time &gt;= #{query.updateTimeStart}</if>" +
            "  <if test='query.updateTimeEnd != null and query.updateTimeEnd != \"\"'> AND f.update_time &lt;= #{query.updateTimeEnd}</if>" +
            "  <if test='query.annotationStatus != null and query.annotationStatus.size() > 0'> AND (SELECT dvf.annotation_status FROM data_dataset_version_file dvf WHERE dvf.dataset_id = f.dataset_id AND dvf.file_id = f.id AND dvf.status IN (0,2) AND COALESCE(NULLIF(dvf.version_name, ''), '__none__') = COALESCE(NULLIF(d.current_version_name, ''), '__none__') ORDER BY dvf.id DESC LIMIT 1) IN <foreach collection='query.annotationStatus' item='as' open='(' separator=',' close=')'>#{as}</foreach></if>" +
            "  <if test='query.labelNames != null and query.labelNames.size() > 0'>" +
            "    <foreach collection='query.labelNames' item='labelName'> AND EXISTS (SELECT 1 FROM data_file_annotation a INNER JOIN data_dataset_version_file vf ON vf.id = a.version_file_id INNER JOIN data_label dl ON dl.id = a.label_id WHERE vf.file_id = f.id AND vf.dataset_id = f.dataset_id AND vf.status IN (0,2) AND COALESCE(NULLIF(vf.version_name, ''), '__none__') = COALESCE(NULLIF(d.current_version_name, ''), '__none__') AND a.deleted = 0 AND dl.name = #{labelName})</foreach>" +
            "  </if>" +
            "  <if test='query.sourceType != null and query.sourceType.size() > 0'> AND m.source_type IN <foreach collection='query.sourceType' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "  <if test='query.captureTimeStart != null and query.captureTimeStart != \"\"'> AND m.capture_time &gt;= #{query.captureTimeStart}</if>" +
            "  <if test='query.captureTimeEnd != null and query.captureTimeEnd != \"\"'> AND m.capture_time &lt;= #{query.captureTimeEnd}</if>" +
            "  <if test='query.device != null and query.device.size() > 0'> AND m.device IN <foreach collection='query.device' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "  <if test='query.deviceSn != null and query.deviceSn != \"\"'> AND m.device_sn = #{query.deviceSn}</if>" +
            "  <if test='query.location != null and query.location.size() > 0'> AND m.location IN <foreach collection='query.location' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "  <if test='query.scenario != null and query.scenario.size() > 0'> AND m.scenario IN <foreach collection='query.scenario' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "  <if test='query.lighting != null and query.lighting.size() > 0'> AND m.lighting IN <foreach collection='query.lighting' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "  <if test='query.quality != null and query.quality.size() > 0'> AND m.quality IN <foreach collection='query.quality' item='item' open='(' separator=',' close=')'>#{item}</foreach></if>" +
            "</where>" +
            "ORDER BY ${orderBy}" +
            "</script>")
    IPage<FileSearchResultVO> search(Page<FileSearchResultVO> page,
                                     @Param("query") FileSearchDTO query,
                                     @Param("resourceUserIds") Set<Long> resourceUserIds,
                                     @Param("orderBy") String orderBy);

    /**
     * 查询全部现存标签（供检索标签筛选下拉使用）
     *
     * @return 标签列表
     */
    @Select("SELECT id, name FROM label_template ORDER BY id")
    List<LabelTemplate> listAllLabels();
}
