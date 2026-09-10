package org.dlut.adv.mineai.model.service;

import org.apache.commons.lang3.StringUtils;
import org.dlut.adv.mineai.core.entity.ApplicationName;
import org.dlut.adv.mineai.core.entity.Chip;
import org.dlut.adv.mineai.core.entity.ExternalModelConverter;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.model.repository.ExternalModelConverterRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mayn
 */
@Service
public class ExternalModelConverterService {

    @Resource
    private ExternalModelConverterRepo externalModelConverterRepo;
    public List<ExternalModelConverter> findAllDevices() {
        return externalModelConverterRepo.findAll();
    }

    //动态分页查询
    public Page<ExternalModelConverter> dynamicFindExternalModelConverterPage(Pageable pageable, ExternalModelConverter externalModelConverter) {
        return externalModelConverterRepo.findAll(new Specification<ExternalModelConverter>() {
            @Override
            public Predicate toPredicate(Root<ExternalModelConverter> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();

                // 条件1: 根据 id 查询
                if (externalModelConverter.getId() != null) {
                    predicates.add(cb.equal(root.get("id"), externalModelConverter.getId()));
                }
                // 条件2: 根据 ip 模糊查询
                if (StringUtils.isNotBlank(externalModelConverter.getIp())) {
                    predicates.add(cb.like(root.get("ip"), "%" + externalModelConverter.getIp() + "%"));
                }
                // 条件2: 根据 description 模糊查询
                if (StringUtils.isNotBlank(externalModelConverter.getDescription())) {
                    predicates.add(cb.like(root.get("description"), "%" + externalModelConverter.getDescription() + "%"));
                }
                if ((externalModelConverter.getPort())!=0) {
                    predicates.add(cb.equal(root.get("port"), externalModelConverter.getPort()));
                }
                if (externalModelConverter.getOnlineStatus()) {
                    predicates.add(cb.equal(root.get("onlineStatus"), externalModelConverter.getOnlineStatus()));
                }

                query.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    //模糊查询
    public Page<ExternalModelConverter> getVagueExternalModelConverterPage(Pageable pageable, String vagueInfo) {
        return externalModelConverterRepo.findAll(new Specification<ExternalModelConverter>() {
            @Override
            public Predicate toPredicate(Root<ExternalModelConverter> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.or(cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    //根据id进行查找
    public ExternalModelConverter findDeviceById(Long id) {
        return externalModelConverterRepo.findDeviceById(id);
    }

    //根据ip进行查找
    public ExternalModelConverter findDeviceByIp(String ip) {
        return externalModelConverterRepo.findDeviceByIp(ip);
    }

    //根据描述进行查找
    public List<ExternalModelConverter> findDevicesByDescription(String description) {
        return externalModelConverterRepo.findDevicesByDescriptionContaining(description);
    }

    //编辑和新增
    public ExternalModelConverter saveDevice(ExternalModelConverter device) {
        return externalModelConverterRepo.save(device);
    }


    public boolean isIpExist(String ip) {
        Long departmentId = externalModelConverterRepo.findIdByIp(ip);
        return departmentId != null; // 如果返回的ID不为null，表示该ip存在
    }

    //根据id进行删除
    public void deleteDevice(Long id) {
        externalModelConverterRepo.deleteById(id);
    }

    public boolean pingDevice(String ipAddress) {

        String os = System.getProperty("os.name").toLowerCase();
        String command;

        if (os.contains("win")) {
            command = "ping -n 1 " + ipAddress; // Windows 命令
        } else if (os.contains("nix") || os.contains("nux")) {
            command = "ping -c 1 " + ipAddress; // Linux 命令
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }

        try {
            Process process = Runtime.getRuntime().exec(command);
            int returnVal = process.waitFor();
//            System.out.println("Ping command executed with return value: " + returnVal + os + command);
            return returnVal == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
