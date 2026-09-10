package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.data.constant.ErrorEnum;
import org.dubhe.data.dao.DataRepoMapper;
import org.dubhe.data.domain.dto.DataRepoCreateDTO;
import org.dubhe.data.domain.dto.DataRepoQueryDTO;
import org.dubhe.data.domain.dto.DataRepoUpdateDTO;
import org.dubhe.data.domain.dto.DatasetCreateDTO;
import org.dubhe.data.domain.entity.DataRepo;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.vo.DataRepoVO;
import org.dubhe.data.service.DataRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.dubhe.data.constant.Constant.SORT_ASC;

@Service
public class DataRepoServiceImpl extends ServiceImpl<DataRepoMapper, DataRepo> implements DataRepoService {

    @Autowired
    private DataRepoMapper dataRepoMapper;

    /**
     * 用户内容服务
     */
    @Autowired
    private UserContextService userContextService;

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Page<DataRepoVO> listDataRepos(Page<DataRepo> page, DataRepoQueryDTO queryDTO) {
        // 获取当前用户信息
        UserContext currentUser = userContextService.getCurUser();
        Long currentUserId = currentUser.getId();
        // 检查是否是管理员（直接写逻辑，不单独封装函数）
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> "管理员".equals(role.getName()) || "管理人员".equals(role.getName()));
        queryDTO.timeConvert();
        QueryWrapper<DataRepo> dataRepoQueryWrapper = WrapperHelp.getWrapper(queryDTO);
        dataRepoQueryWrapper.eq("deleted", MagicNumConstant.ZERO);
        // 如果不是管理员，则只允许查询当前用户创建的记录
        if (!isAdmin) {
            dataRepoQueryWrapper.eq("create_user_id", currentUserId);
        }
        if (StringUtils.isNotEmpty(queryDTO.getSort())&&StringUtils.isNotEmpty(queryDTO.getOrder())){
            dataRepoQueryWrapper.orderBy(
                    true,
                    SORT_ASC.equals(queryDTO.getOrder().toLowerCase()),
                    StringUtils.humpToLine(queryDTO.getSort())
            );
        }
        // 查询数据仓库列表，根据queryDTO中的条件进行筛选
        Page<DataRepo> dataRepoPage = dataRepoMapper.selectPage(page, dataRepoQueryWrapper);
        Page<DataRepoVO> dataRepoVOPage = new Page<>();
        List<DataRepoVO> dataRepoVOList = dataRepoPage.getRecords().stream()
                .map(this::convertToDataRepoVO)
                .collect(Collectors.toList());
        dataRepoVOPage.setRecords(dataRepoVOList);
        dataRepoVOPage.setTotal(dataRepoPage.getTotal());
        return dataRepoVOPage;
    }

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public DataRepoVO getDataRepoById(Long id) {
        // 根据ID获取数据仓库详情
        DataRepo dataRepo = dataRepoMapper.selectById(id);
        return convertToDataRepoVO(dataRepo);
    }

    @Override
    @Transactional
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public boolean createDataRepo(DataRepoCreateDTO createDTO) {
        // 创建数据仓库
        DataRepo dataRepo = createDTO.toDataRepo();
        dataRepo.setOriginUserId(userContextService.getCurUserId());
        dataRepo.setCreateUserId(userContextService.getCurUserId());
        dataRepo.setDeleted(false);
        try {
            save(dataRepo);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorEnum.DATASET_NAME_DUPLICATED_ERROR);
        }
        dataRepo.setUri("datarepo/" + dataRepo.getId());
        updateById(dataRepo);
        return true;
    }

    @Override
    @Transactional
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public boolean updateDataRepo(DataRepoUpdateDTO updateDTO, Long id) {
        // 更新数据仓库信息
        DataRepo dataRepo = updateDTO.toDataRepo();
        dataRepo.setId(id);
        return dataRepoMapper.updateById(dataRepo) > 0;
    }

    @Override
    @Transactional
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public void deleteDataRepo(Long id) {
        // 删除数据仓库
        DataRepo dataRepo = new DataRepo();
        dataRepo.setId(id);
        dataRepo.setDeleted(true);
        dataRepoMapper.updateById(dataRepo);
    }

    @Override
    public List<DataRepoVO> queryAllDataRepos() {
        List<DataRepo> dataRepos = dataRepoMapper.selectList(new QueryWrapper<DataRepo>().eq("deleted", 0));
        return dataRepos.stream().map(this::convertToDataRepoVO).collect(Collectors.toList());
    }

    @Override
    public List<DataRepoVO> queryAllDataReposNotNull() {
        List<DataRepo> dataRepos = dataRepoMapper.selectList(new QueryWrapper<DataRepo>().eq("deleted", 0).exists("SELECT 1 FROM data_repo_file WHERE dataset_id = data_repo.id AND deleted = 0"));
        return dataRepos.stream().map(this::convertToDataRepoVO).collect(Collectors.toList());
    }

    // 检查名称是否存在
    @Override
    public boolean isNameExist(String repoName) {
        Long departmentId = dataRepoMapper.findIdByName(repoName);
        return departmentId != null; // 如果返回的ID不为null，表示该部门存在
    }

    private DataRepoVO convertToDataRepoVO(DataRepo dataRepo) {
        // 将DataRepo实体转换为DataRepoVO
        DataRepoVO dataRepoVO = new DataRepoVO();
        // 设置属性值
        dataRepoVO.setId(dataRepo.getId());
        dataRepoVO.setName(dataRepo.getName());
        dataRepoVO.setRemark(dataRepo.getRemark());
        dataRepoVO.setUri(dataRepo.getUri());
        dataRepoVO.setCreateTime(dataRepo.getCreateTime());
        dataRepoVO.setUserId(dataRepo.getCreateUserId());
        // 根据需要设置其他属性
        return dataRepoVO;
    }
}
