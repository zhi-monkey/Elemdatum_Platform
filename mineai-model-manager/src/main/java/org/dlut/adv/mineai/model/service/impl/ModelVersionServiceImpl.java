package org.dlut.adv.mineai.model.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.Chip;
import org.dlut.adv.mineai.core.entity.ModelConfig;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dao.ModelVersionMapper;
import org.dlut.adv.mineai.model.domain.dto.ModelVersionCreateDTO;
import org.dlut.adv.mineai.model.dto.ModelVersionDTO;
import org.dlut.adv.mineai.model.harbor.HarborController;
import org.dlut.adv.mineai.model.harbor.HarborService;
import org.dlut.adv.mineai.model.repository.ModelVersionRepo;
import org.dlut.adv.mineai.model.service.ChipService;
import org.dlut.adv.mineai.model.service.ModelConfigService;
import org.dlut.adv.mineai.model.service.ModelUploadService;
import org.dlut.adv.mineai.model.service.ModelVersionService;
import org.dlut.adv.mineai.model.service.inter.ModelVersionInterService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModelVersionServiceImpl extends ServiceImpl<ModelVersionMapper, ModelVersion> implements ModelVersionInterService {

    private final ModelVersionMapper modelVersionMapper;

    private final ModelVersionService modelVersionService;

    private final HarborController harborController;

    private final HarborService harborService;

    private final ModelUploadService modelUploadService;

    private final ModelVersionRepo modelVerisonRepo;

    private final ModelConfigService modelConfigService;

    @Autowired
    private ChipService chipService;

    @Value("${modelImageRootPath}")
    private String modelImageRootPath;
    @Autowired
    private ModelVersionDefaultLabelsServiceImpl modelVersionDefaultLabelsServiceImpl;

    /**
     * 用于镜像中心页面的镜像上传，上传方式为url上传
     *
     * @param userId
     * @param url
     * @param showName
     * @param level
     * @param description
     * @param use
     * @return
     */
    @Override
    public Msg<ModelVersionDTO> create(Long userId, String url, String showName, String level, String description, String use, String chipType) {
        // use 现在可能多选, 例如 "训练,自动标注“
        List<String> useList = Arrays.asList(use.split(","));
        //第一步要调用k8s接口，也就是harborController.urlUploadImage将镜像上传至镜像仓库
        //需要的参数是showName和newTag，内容就在下面一看就会
        Msg<ModelVersionDTO> checkResult = checkImageEnv(url, useList);
        if (Objects.equals(checkResult.getCode(), MsgCode.FAILED.getCode())) {
            // 如果返回的是错误码则构造成成功再返回
            return new Msg<>(MsgCode.SUCCEED, checkResult.getPayload());
        }
        String newTag = null;
        Date createTime = new Date();
        newTag = "image" + createTime.getTime();
        log.info("开始上传到Harbor");
        Msg<Map<String, String>> mapMsg = harborController.urlUploadImage(url, showName, newTag);
        ModelVersionDTO modelVersionDTO = new ModelVersionDTO();

        if (Objects.equals(mapMsg.getCode(), MsgCode.FAILED.getCode())) {
            // 上传镜像到镜像仓库失败
            if (mapMsg.getPayload().get("ERROR_MESSAGE") != null) {
                modelVersionDTO.setIsSucceed(false);
                if (MsgCode.FAILED_TO_GET_IMAGE_ENV.getText().equals(mapMsg.getPayload().get("ERROR_MESSAGE"))) {
                    modelVersionDTO.setErrMsg(MsgCode.FAILED_TO_GET_IMAGE_ENV.getText());
                    // 环境变量获取出错
                    return new Msg<>(MsgCode.SUCCEED, modelVersionDTO);
                } else if (MsgCode.CONNECT_TO_HARBOR_TIMEOUT.getText().equals(mapMsg.getPayload().get("ERROR_MESSAGE"))) {
                    modelVersionDTO.setErrMsg(MsgCode.CONNECT_TO_HARBOR_TIMEOUT.getText());
                    // 连接超时
                    return new Msg<>(MsgCode.SUCCEED, modelVersionDTO);
                } else {
                    // 还没触发过的错
                    modelVersionDTO.setErrMsg(MsgCode.UN_FILED_ERROR.getText());
                    return new Msg<>(MsgCode.SUCCEED, modelVersionDTO);
                }
            }
        }
        // 下一步就是在数据库中存储镜像信息，也就是modelVersion
        ModelVersion modelVersion = new ModelVersion();
        if (useList.contains("转换")) {
            // 如果没有查询到芯片信息且为转换镜像，则返回错误消息. 事实上如果contains转换则里面只有转换
            Chip chip = chipService.findByChipType(chipType);
            if (chip == null) {
                return new Msg<>(MsgCode.FAILED_TO_GET_CHIP);
            }
            modelVersion.setChip(chip);
        }

        Map<String, String> envMap = checkResult.getPayload().getEnvMap();
        String name = showName + '_' + level;
        ModelVersion modelVersionInDatabase = modelVerisonRepo.findModelVersionByNameAndIsDelete(name, ModelVersion.NOT_DELETE);
        if (modelVersionInDatabase != null) {
            return new Msg<>(MsgCode.IMAGE_EXITED);
        }
        modelVersion.setName(name);
        modelVersion.setShowName(showName);
        modelVersion.setCreateTime(createTime);
        modelVersion.setArchitecture("arm64");
        modelVersion.setUseGpu("true".equals(envMap.getOrDefault("USE_GPU", "false")));
        modelVersion.setAnnotationType(envMap.getOrDefault("ANNOTATION_TYPE", "Detection"));
        modelVersion.setAnnotationFormat(envMap.getOrDefault("ANNOTATION_FORMAT", "YOLO"));
        modelVersion.setRoleId(Math.toIntExact(userId));
        modelVersion.setUserId(userId);
        modelVersion.setPublic(false);
        modelVersion.setDescription(description);
        modelVersion.setUrl(showName + '_' + newTag);
        modelVersion.setLevel(level);
        modelVersion.setSize(2048);
        modelVersion.setStatus(ModelVersion.uploaded);
        modelVersion.setReuse(false);
        List<ModelConfig> modelConfigList = new ArrayList<>();

        // 用来存放默认标签的JSON字符串
        final AtomicReference<String> defaultLabelsRef = new AtomicReference<>("");

        envMap.forEach((k, v) -> {
                    if (k.contains("HP_") || k.contains("CONVERT_")) {
                        ModelConfig modelConfig = new ModelConfig();
                        modelConfig.setField(k);
                        modelConfig.setRequired(true);
                        // 正则表达式提取信息，使用分隔符处理冒号
                        Pattern pattern = Pattern.compile("(\\w+):([^:]*):([^:]*):(.+)");
                        Matcher matcher = pattern.matcher(v);
                        if (matcher.find()) {
                            String variableType = matcher.group(1);       // 变量类型
                            String range = matcher.group(2);               // 取值范围（可能为空字符串）
                            String defaultValue = matcher.group(3);        // 默认值
                            String setInputDescription = matcher.group(4);    // 输入规范要求
                            modelConfig.setDefaultNum(defaultValue);
                            modelConfig.setMsg(range);
                            modelConfig.setInputDescription(setInputDescription);
                            modelConfig.setType(variableType);
                        }
//                            modelConfig.setMsg(v);
                        modelConfigService.saveModelConfig(modelConfig);
                        modelConfigList.add(modelConfig);
                    }
                    // 这里是镜像中默认模型对应的标签信息, 格式是JSON数组字符串 '["cat", "dog", "person"]'
                    if ("DEFAULT_LABELS".equals(k)) {
                        // 确保没有单引号
                        String cleanedValue = v.trim().replaceAll("^'|'$", "");
                        // 拿出去处理，这里modelVersionId还不存在
                        defaultLabelsRef.set(cleanedValue);
                    }
                }
        );
        modelVersion.setModelConfigList(modelConfigList);
        // 根据所选镜像功能，新增镜像
        if (useList.contains("训练")) {
            modelVersion.setTrainable(true);
        }
        if (useList.contains("自动标注")) {
            modelVersion.setAutoLabel(true);
        }
        if (useList.contains("转换")) {
            modelVersion.setInspectable(true);
        }
        modelVersionService.saveModelVersion(modelVersion);
        if (!Objects.toString(defaultLabelsRef.get(), "").isEmpty()) {
            try {
                String defaultLabelsStr = defaultLabelsRef.get();

                Gson gson = new Gson();
                String[] labelNames = gson.fromJson(defaultLabelsStr, String[].class);
                // 插入关联信息
                modelVersionDefaultLabelsServiceImpl.insertRelations(modelVersion.getId(), labelNames);
            } catch (Exception e) {
                // 解析失败帆返回错误
                modelVersionDTO.setIsSucceed(false);
                modelVersionDTO.setErrMsg(MsgCode.DEFAULT_LABELS_WRONG_FORMAT.getText());
                return new Msg<>(MsgCode.SUCCEED, modelVersionDTO);
            }
        }
        BeanUtils.copyProperties(modelVersion, modelVersionDTO);
        modelVersionDTO.setIsSucceed(true);
        modelVersionDTO.setErrMsg("");
        return new Msg<>(MsgCode.SUCCEED, modelVersionDTO);
    }

    public Msg<ModelVersionDTO> checkImageEnv(String srcImageName, List<String> useList) {
        ModelVersionDTO modelVersionDTO = new ModelVersionDTO();
        Map<String, String> envMap = harborService.getImageEnvFromSource(srcImageName);
        if (envMap.isEmpty() || envMap.containsKey("ERROR_MESSAGE")) {
            modelVersionDTO.setIsSucceed(false);
            modelVersionDTO.setErrMsg(envMap.get("ERROR_MESSAGE"));
            return new Msg<>(MsgCode.FAILED, modelVersionDTO);
        }
        // 抽取镜像的判断(HP_, CONVERT_等)
        // 训练转换得有MODE参数
        if (!containsKeyStartsWith(envMap, "MODE_")) {
            modelVersionDTO.setIsSucceed( false);
            modelVersionDTO.setErrMsg(MsgCode.TRAIN_ENV_MISSING_OR_NOT_EXIST.getText());
            return new Msg<>(MsgCode.FAILED, modelVersionDTO);
        }
        //下一步就是在数据库中存储镜像信息，也就是modelVersion
        ModelVersion modelVersion = new ModelVersion();
        if (useList.contains("转换")) {
            // 转换不能缺少convert相关参数
            if (!containsKeyStartsWith(envMap, "CONVERT_")) {
                modelVersionDTO.setIsSucceed(false);
                modelVersionDTO.setErrMsg(MsgCode.CONVERT_ENV_MISSING_OR_NOT_EXIST.getText());
                return new Msg<>(MsgCode.FAILED, modelVersionDTO);
            }
        }
        if (useList.contains("自动标注")) {
            // 自动标注不能缺少default_labels
            if (!containsKeyStartsWith(envMap, "DEFAULT_LABELS")) {
                modelVersionDTO.setIsSucceed(false);
                modelVersionDTO.setErrMsg(MsgCode.DEFAULT_LABELS_MISSING_OR_NOT_EXIST.getText());
                return new Msg<>(MsgCode.FAILED, modelVersionDTO);
            }
            String defaultLabels = envMap.get("DEFAULT_LABELS").trim().replaceAll("^'|'$", "");
            if (Objects.toString(defaultLabels, "").isEmpty()) {
                modelVersionDTO.setIsSucceed(false);
                modelVersionDTO.setErrMsg(MsgCode.DEFAULT_LABELS_MISSING_OR_NOT_EXIST.getText());
                return new Msg<>(MsgCode.FAILED, modelVersionDTO);
            }
            // 检查格式规范
            try {
                Gson gson = new Gson();
                gson.fromJson(defaultLabels, String[].class);
            } catch (Exception e) {
                // 解析失败帆返回错误
                modelVersionDTO.setIsSucceed(false);
                modelVersionDTO.setErrMsg(MsgCode.DEFAULT_LABELS_WRONG_FORMAT.getText());
                return new Msg<>(MsgCode.FAILED, modelVersionDTO);
            }
        }
        // 检查成功
        modelVersionDTO.setEnvMap(envMap);
        return new Msg<>(MsgCode.SUCCEED, modelVersionDTO);
    }

    public static boolean containsKeyStartsWith(Map<String, String> map, String prefix) {
        if (map == null || prefix == null) {
            return false;
        }
        return map.keySet()
                .stream()
                .anyMatch(key -> key != null && key.startsWith(prefix));
    }

    @Override
    public Msg<ModelVersion> createLocal(Long userId, String showName, String level, String fileName, String description, String use, String chipType) {
        // use 现在可能多选, 例如 "训练,自动标注“
        List<String> useList = Arrays.asList(use.split(","));

        String directoryPath = modelImageRootPath + "/" + showName + '_' + level;
        String fileRealPath = directoryPath + "/" + showName + '_' + level;
        File tempFile = new File(fileRealPath);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        MultipartFile file = ModelUploadService.getMultipartFile_new(tempFile);
        Date createTime = new Date();
        String newTag = "image" + createTime.getTime();
        if (file != null) {
            System.out.println("开始上传到harbor");
            Msg<Map<String, String>> mapMsg = harborController.uploadImage(file, showName, newTag);
            if (Objects.equals(mapMsg.getCode(), MsgCode.FAILED.getCode())) {
                return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
            }
            if (Objects.equals(mapMsg.getCode(), MsgCode.SUCCEED.getCode())) {
                //根据所选镜像功能，新增镜像
                ModelVersion modelVersion = new ModelVersion();

                // 通过 chipType 查询对应的 Chip 实体，并将其设置到 modelVersion 中
                Chip chip = chipService.findByChipType(chipType);
                // 如果没有查询到芯片信息且为转换镜像，则返回错误消息. 事实上如果contains转换则里面只有转换
                if (chip == null && useList.contains("转换")) {
                    return new Msg<>(MsgCode.FAILED_TO_GET_CHIP);
                }
                modelVersion.setChip(chip);

                // 用来存放默认标签的JSON字符串
                final AtomicReference<String> defaultLabelsRef = new AtomicReference<>("");

                String modelVersionName = showName + '_' + level;
                ModelVersion modelVersionInDatabase = modelVerisonRepo.findModelVersionByNameAndIsDelete(modelVersionName, ModelVersion.NOT_DELETE);
                if (modelVersionInDatabase != null) {
                    return new Msg<>(MsgCode.IMAGE_EXITED);
                }
                modelVersion.setName(modelVersionName);
                modelVersion.setShowName(showName);
                modelVersion.setCreateTime(createTime);
                modelVersion.setArchitecture("arm64");
                modelVersion.setRoleId(Math.toIntExact(userId));
                modelVersion.setUserId(userId);
                modelVersion.setPublic(false);
                modelVersion.setUseGpu(mapMsg.getPayload().get("USE_GPU") != null && ("true".equals(mapMsg.getPayload().get("USE_GPU"))));
                modelVersion.setDescription(description);
                modelVersion.setUrl(showName + '_' + newTag);
                modelVersion.setLevel(level);
                modelVersion.setStatus(ModelVersion.uploaded);
                modelVersion.setSize(2048);
                modelVersion.setReuse(false);
                List<ModelConfig> modelConfigList = new ArrayList<>();
                mapMsg.getPayload().forEach((k, v) -> {
                            if (k.contains("HP_")) {
                                ModelConfig modelConfig = new ModelConfig();
                                modelConfig.setField(k);
                                modelConfig.setRequired(true);
                                modelConfig.setMsg(v);
                                modelConfigService.saveModelConfig(modelConfig);
                                modelConfigList.add(modelConfig);
                            }
                            // 这里是镜像中默认模型对应的标签信息, 格式是JSON数组字符串 '["cat", "dog", "person"]'
                            if ("DEFAULT_LABELS".equals(k)) {
                                // 确保没有单引号
                                String cleanedValue = v.trim().replaceAll("^'|'$", "");
                                // 拿出去处理，这里modelVersionId还不存在
                                defaultLabelsRef.set(cleanedValue);
                            }
                                }
                );
                modelVersion.setModelConfigList(modelConfigList);
                if (useList.contains("训练")) {
                    modelVersion.setTrainable(true);
                }
                if (useList.contains("自动标注")) {
                    modelVersion.setAutoLabel(true);
                }
                if (useList.contains("转换")) {
                    modelVersion.setInspectable(true);
                }
                modelVersionService.saveModelVersion(modelVersion);
                if (!Objects.toString(defaultLabelsRef.get(), "").isEmpty()) {
                    try {
                        String defaultLabelsStr = defaultLabelsRef.get();

                        Gson gson = new Gson();
                        String[] labelNames = gson.fromJson(defaultLabelsStr, String[].class);
                        // 插入关联信息
                        modelVersionDefaultLabelsServiceImpl.insertRelations(modelVersion.getId(), labelNames);
                    } catch (Exception e) {
                        System.err.println("解析失败: " + e.getMessage());
                    }
                }
                return new Msg<>(MsgCode.SUCCEED, modelVersion);
            }
        }
        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    @Override
    public Boolean checkModelVersion(ModelVersionCreateDTO modelVersionCreateDTO) {
        ModelVersion modelVersion = modelVersionMapper.getModelVersionByShowNameAndLevel(modelVersionCreateDTO.getShowName(), modelVersionCreateDTO.getLevel());
        return !Objects.isNull(modelVersion);
    }


    @Override
    public Msg<ModelVersion> updateModelVersion(Long mdId, String url, String showName, String level, String description, String use, String chipType) {
        ModelVersion oldModelVersion = modelVerisonRepo.findModelVersionById(mdId);

        // 重名检查
        String name = showName + '_' + level;
        ModelVersion sameShowNameModelVersion = modelVerisonRepo.findModelVersionByNameAndIsDelete(name, ModelVersion.NOT_DELETE);
        if (sameShowNameModelVersion != null) {
            if (sameShowNameModelVersion.getId() != mdId) {
                return new Msg<>(MsgCode.IMAGE_NAME_EXIST);
            }
        }

        if (chipType.isEmpty()) {
            return new Msg<>(MsgCode.CHIP_TYPE_NOT_PRESENT);
        }
        Chip chip = chipService.findByChipType(chipType);
        if (chip == null) {
            return new Msg<>(MsgCode.CHIP_NOT_EXIST);
        }
        oldModelVersion.setChip(chip);

        // 更新ModelVersion的信息
        oldModelVersion.setShowName(showName);
        oldModelVersion.setLevel(level);
        oldModelVersion.setDescription(description);
        modelVerisonRepo.save(oldModelVersion);
        return new Msg<>(MsgCode.SUCCEED, oldModelVersion);
    }


    /**
     * 判断镜像的用途
     *
     * @param id
     * @return
     */
    @Override
    public String ModelVersionUse(Long id) {
        ModelVersion md = modelVerisonRepo.findModelVersionById(id);
        if (md.isTrainable()) {
            return "训练";
        }
        if (md.isInferable()) {
            return "推理";
        }
        return null;
    }

}
