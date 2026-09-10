package org.dlut.adv.mineai.model.service;

import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.HardwareParams;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.model.exception.BusinessException;
import org.dlut.adv.mineai.model.repository.HardwareParamsRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class HardwareParamsService {

    @Resource
    HardwareParamsRepo hardwareParamsRepo;

    /**
     * 找寻所有的硬件的参数配置项目
     *
     * @return 配置项集合
     */
    public List<HardwareParams> findAllHardwareParams(Long userId){
        return hardwareParamsRepo.findByCreateUserIdOrAdminRole(userId);
    }

    /**
     * 分页找寻所有的硬件的参数配置项目
     *
     * @return 配置项分页对象
     */
    public Page<HardwareParams> findHardwareParamsPage(Long userId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;
        int limit = size;

        // 获取分页数据
        List<HardwareParams> content = hardwareParamsRepo.findByCreateUserIdOrAdminRolePage(userId, offset, limit);

        // 获取总记录数（需要实现这个方法）
        long total = hardwareParamsRepo.countByCreateUserIdOrAdminRole(userId);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 根据is_default字段分页查询硬件参数
     * @param userId 用户ID
     * @param isDefault 是否为默认配置 (1: 默认配置, 0: 自定义配置)
     * @param pageable 分页参数
     * @return 硬件参数分页对象
     */
    public Page<HardwareParams> findHardwareParamsPageByIsDefault(Long userId, int isDefault, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int offset = page * size;
        int limit = size;

        // 获取分页数据
        List<HardwareParams> content = hardwareParamsRepo.findByCreateUserIdOrAdminRolePageByIsDefault(userId, isDefault, offset, limit);

        // 获取总记录数
        long total = hardwareParamsRepo.countByCreateUserIdOrAdminRoleByIsDefault(userId, isDefault);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 批量删除硬件参数配置
     *
     * @param ids 硬件参数ID列表
     * @return 删除的记录数
     */
    @Transactional
    public Map<String, Object> deleteHardwareParamsBatch(List<Long> ids) {
        Map<String, Object> result = new HashMap<>();

        // 空值检查
        if (ids == null || ids.isEmpty()) {
            result.put("deletedCount", 0);
            result.put("referencedNames", Collections.emptyList());
            return result;
        }

        try {
            // 查询被引用的记录
            List<Object[]> referencedRecords = hardwareParamsRepo.findReferencedIds(ids);

            // 处理被引用记录
            List<String> referencedNames = new ArrayList<>();
            Set<Long> referencedIds = new HashSet<>();

            for (Object[] record : referencedRecords) {
                // 安全处理名称
                referencedNames.add(String.valueOf(record[0]));

                // 安全处理ID类型转换
                Object idObj = record[1];
                Long id = null;

                if (idObj instanceof BigInteger) {
                    id = ((BigInteger) idObj).longValue();
                } else if (idObj instanceof Long) {
                    id = (Long) idObj;
                } else if (idObj instanceof Number) {
                    id = ((Number) idObj).longValue();
                }

                if (id != null) {
                    referencedIds.add(id);
                }
            }

            // 筛选出未被引用的ID
            List<Long> deletableIds = ids.stream()
                    .filter(id -> !referencedIds.contains(id))
                    .collect(Collectors.toList());

            // 执行批量删除
            int deletedCount = 0;
            if (!deletableIds.isEmpty()) {
                hardwareParamsRepo.deleteAllByIdInBatch(deletableIds);
                deletedCount = deletableIds.size();
            }

            result.put("deletedCount", deletedCount);
            result.put("referencedNames", referencedNames);
            return result;

        } catch (Exception e) {
            throw new BusinessException("删除硬件参数时发生错误: " + e.getMessage());
        }
    }

    /**
     * 按照 id 查找硬件的参数
     *
     * @param id 硬件 id
     * @return
     */
    public HardwareParams findHardwareParamsById(long id){
        return hardwareParamsRepo.findHardwareParamsById(id);
    }

    /**
     * 保存硬件参数
     *
     * @param hardwareParams
     */
    public void saveHardwareParams(HardwareParams hardwareParams) {
        // 获取当前用户上下文（确保userId转为Long）
        Long currentUserId = Long.valueOf(UserContextHolder.getUserContext().getId());
        hardwareParams.setCreateUserId(currentUserId);
        hardwareParamsRepo.save(hardwareParams);
    }

    /**
     * 判断硬件配置名称是否存在
     *
     * @param name
     * @return
     */
    public boolean isHardwareParamsExist(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        return hardwareParamsRepo.findHardwareParamsByName(name) != null;
    }

    /**
     * 获取默认硬件配置（用于自迭代训练等场景）
     * @return 默认硬件配置
     */
    public HardwareParams getDefaultHardwareParams() {
        List<HardwareParams> allParams = hardwareParamsRepo.findAll();
        if (allParams != null && !allParams.isEmpty()) {
            return allParams.get(0);
        }
        throw new IllegalStateException("系统中没有可用的硬件配置");
    }
}
