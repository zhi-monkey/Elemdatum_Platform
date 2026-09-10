package org.dlut.adv.mineai.model.service;


import net.lingala.zip4j.ZipFile;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.domain.dto.FileChunkDTO;
import org.dlut.adv.mineai.model.domain.dto.FileChunkResultDTO;
import org.dlut.adv.mineai.model.harbor.HarborController;
import org.dlut.adv.mineai.model.harbor.HarborService;
import org.dlut.adv.mineai.model.kubernetes.service.JobService;
import org.dlut.adv.mineai.model.repository.ModelConfigRepo;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.repository.ModelVersionRepo;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;


@Service
@SuppressWarnings("all")
public class ModelUploadService {

    private Logger logger = LoggerFactory.getLogger(ModelUploadService.class);

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private HarborController harborController;
    @Resource
    private ModelGenerationService modelGenerationService;

    @Resource
    private ModelGenerationRepo modelGenerationRepo;

    @Resource
    private ModelVersionService modelVersionService;
    @Resource
    private ModelConfigService modelConfigService;
    @Resource
    private ModelConfigRepo modelConfigRepo;
    @Resource
    private ModelVersionRepo modelVersionRepo;

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private JobService jobService;

    @Resource
    private HarborService harborService;

    @Value("${chunkFolderPath}")
    private String chunkFolderPath;

    @Value("${datasetRootPath}")
    private String datasetRootPath;

    @Value("${modelImageRootPath}")
    private String modelImageRootPath;

    @Value("${kubernetes.config}")
    private String k8sConfig;

    @Value("${kubernetes.namespace}")
    private String namespace;


    /**
     * 检查文件是否存在，如果存在则跳过该文件的上传，如果不存在，返回需要上传的分片集合
     *
     * @param chunkDTO
     * @return
     */
    public FileChunkResultDTO checkChunkExist(FileChunkDTO chunkDTO) {
        //1.检查文件是否已上传过
        //1.1)检查在磁盘中是否存在
        String chunkFileFolderPath = getChunkFileFolderPath(chunkDTO.getIdentifier());
        logger.info("chunkFileFolderPath-->{}", chunkFileFolderPath);
        String filePath = getFilePath(chunkDTO.getIdentifier(), chunkDTO.getFilename(), chunkDTO.getFilePath());
        File file = new File(filePath);
        boolean exists = file.exists();
        //1.2)检查Redis中是否存在,并且所有分片已经上传完成。
        Set<Integer> uploaded = (Set<Integer>) redisTemplate.opsForHash().get(chunkDTO.getIdentifier(), "uploaded");
        if (uploaded != null && uploaded.size() == chunkDTO.getTotalChunks() && exists) {
            return new FileChunkResultDTO(true);
        }
        File fileFolder = new File(chunkFileFolderPath);
        if (!fileFolder.exists()) {
            boolean mkdirs = fileFolder.mkdirs();
            logger.info("准备工作,创建文件夹,chunkFileFolderPath:{},mkdirs:{}", chunkFileFolderPath, mkdirs);
        }
        // 断点续传，返回已上传的分片
        return new FileChunkResultDTO(false, uploaded);
    }


    /**
     * 上传分片
     *
     * @param chunkDTO
     */
    public void uploadChunk(FileChunkDTO chunkDTO) {
        //分块的目录
        String chunkFileFolderPath = getChunkFileFolderPath(chunkDTO.getIdentifier());
        logger.info("分块的目录 -> {}", chunkFileFolderPath);
        File chunkFileFolder = new File(chunkFileFolderPath);
        if (!chunkFileFolder.exists()) {
            boolean mkdirs = chunkFileFolder.mkdirs();
            logger.info("创建分片文件夹:{}", mkdirs);
        }
        //写入分片
        try (
                InputStream inputStream = chunkDTO.getFile().getInputStream();
                FileOutputStream outputStream = new FileOutputStream(new File(chunkFileFolderPath + chunkDTO.getChunkNumber()))
        ) {
            IOUtils.copy(inputStream, outputStream);
            logger.info("文件标识:{},chunkNumber:{}", chunkDTO.getIdentifier(), chunkDTO.getChunkNumber());
            //将该分片写入redis
            long size = saveToRedis(chunkDTO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean mergeChunk(String identifier, String fileName, Integer totalChunks, String filePath, Boolean zip) throws IOException {
        return mergeChunks(identifier, fileName, totalChunks, filePath, zip);
    }

    /**
     * 合并分片
     *
     * @param identifier
     * @param filename
     */
    private boolean mergeChunks(String identifier, String filename, Integer totalChunks, String filePath1, Boolean zip) {
        //分片位置
        String chunkFileFolderPath = getChunkFileFolderPath(identifier);
        //没有目标文件夹创建先创建文件夹
        String directoryPath = modelImageRootPath + "/" + filePath1;
        new File(directoryPath).mkdirs();
        //合并文件位置
        String filePath = getFilePath(identifier, filename, filePath1);
        // 检查分片是否都存在
        if (checkChunks(chunkFileFolderPath, totalChunks)) {
            File chunkFileFolder = new File(chunkFileFolderPath);
            File mergeFile = new File(filePath);
            File[] chunks = chunkFileFolder.listFiles();
            //排序
            List fileList = Arrays.asList(chunks);
            Collections.sort(fileList, (Comparator<File>) (o1, o2) -> {
                return Integer.parseInt(o1.getName()) - (Integer.parseInt(o2.getName()));
            });
            try {
                RandomAccessFile randomAccessFileWriter = new RandomAccessFile(mergeFile, "rw");
                byte[] bytes = new byte[1024];
                for (File chunk : chunks) {
                    RandomAccessFile randomAccessFileReader = new RandomAccessFile(chunk, "r");
                    int len;
                    while ((len = randomAccessFileReader.read(bytes)) != -1) {
                        randomAccessFileWriter.write(bytes, 0, len);
                    }
                    randomAccessFileReader.close();
                }
                randomAccessFileWriter.close();
            } catch (Exception e) {
                logger.info("合并文件失败", e);
                return false;
            }
            //自动解压
            if (zip) {
                logger.info("开始自动解压");
                if (filename.endsWith("zip")) {
                    try {
                        ZipFile zipFile = new ZipFile(mergeFile);
                        zipFile.extractAll(directoryPath);
                        logger.info("解压完成");
                    } catch (Exception e) {
                        logger.info("解压失败");
                    }
                }
            }

            return true;
        }
        return false;
    }

    /**
     * 检查分片是否都存在
     *
     * @param chunkFileFolderPath
     * @param totalChunks
     * @return
     */
    private boolean checkChunks(String chunkFileFolderPath, Integer totalChunks) {
        try {
            for (int i = 1; i <= totalChunks + 1; i++) {
                File file = new File(chunkFileFolderPath + "/" + i);
                if (file.exists()) {
                    continue;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            logger.info("检查分片失败", e);
            return false;
        }
        return true;
    }

    /**
     * 分片写入Redis
     *
     * @param chunkDTO
     */
    private synchronized long saveToRedis(FileChunkDTO chunkDTO) {
        Set<Integer> uploaded = (Set<Integer>) redisTemplate.opsForHash().get(chunkDTO.getIdentifier(), "uploaded");
        if (uploaded == null) {
            uploaded = new HashSet<>(Arrays.asList(chunkDTO.getChunkNumber()));
            HashMap<String, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("uploaded", uploaded);
            objectObjectHashMap.put("totalChunks", chunkDTO.getTotalChunks());
            objectObjectHashMap.put("totalSize", chunkDTO.getTotalSize());
            objectObjectHashMap.put("path", chunkDTO.getFilename());
            redisTemplate.opsForHash().putAll(chunkDTO.getIdentifier(), objectObjectHashMap);
        } else {
            uploaded.add(chunkDTO.getChunkNumber());
            redisTemplate.opsForHash().put(chunkDTO.getIdentifier(), "uploaded", uploaded);
        }
        return uploaded.size();
    }

    /**
     * 得到文件的绝对路径
     *
     * @param identifier
     * @param filename
     * @return
     */
    private String getFilePath(String identifier, String filename, String filePath) {
        return modelImageRootPath + "/" + filePath + "/" + filename;
    }

    /**
     * 得到分块文件所属的目录
     *
     * @param identifier
     * @return
     */
    private String getChunkFileFolderPath(String identifier) {
        return chunkFolderPath + identifier.substring(0, 1) + "/" +
                identifier.substring(1, 2) + "/" +
                identifier + "/" + "chunks" + "/";
    }

    /**
     * 删除单个文件
     *
     * @param sPath 被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public boolean deleteFile(String filePath, String fileName) {
        boolean flag = false;
        String directoryPath = modelImageRootPath + "/" + filePath;
        String fileRealPath = directoryPath + "/" + fileName;
        File file = new File(fileRealPath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 调用接口，把url地址的镜像文件上传到Harbor仓库
     * 这里的url的值是镜像所处的docker仓库地址，fileName是镜像所属的生产任务的名称
     */
    public Msg<ModelVersion> uploadUrlToHarbor(String url, String generationName, String englishName, String description, String imageType) {
        System.out.println("开始上传到harbor");
        String newTag = null;
        Date createTime = new Date();
        newTag = "image" + createTime.getTime();
        Msg<Map<String, String>> mapMsg = harborController.urlUploadImage(url, englishName, newTag);
        if (mapMsg.getCode() == MsgCode.FAILED.getCode()) {
            return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
        }
        if (mapMsg.getCode() == MsgCode.SUCCEED.getCode()) {
            //根据所选镜像功能，新增镜像
            switch (imageType) {
                case "训练":
                    ModelVersion modelVersion = new ModelVersion();
                    ModelGeneration modelGeneration = modelGenerationService.findModelGenerationByName(generationName);
                    if (modelGeneration == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    String modelVersionName = englishName + '_' + newTag;
                    ModelVersion modelVersionInDatabase = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName, ModelVersion.NOT_DELETE);
                    if (modelVersionInDatabase != null) {
                        return new Msg<>(MsgCode.IMAGE_EXITED);
                    }
                    modelVersion.setName(modelVersionName);
                    modelVersion.setShowName(modelVersionName);
                    modelVersion.setCreateTime(createTime);
                    modelVersion.setArchitecture("arm64");
                    modelVersion.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                    modelVersion.setInferable(false);
                    modelVersion.setInspectable(false);
                    modelVersion.setTrainable(true);
                    modelVersion.setInspectNum("0-0-0-0-0");
                    modelVersion.setTrainNum("0-0-0-0-0");
                    modelVersion.setDescription(description);
                    modelVersion.setUrl(modelVersionName);
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
                            }
                    );
                    modelVersion.setModelConfigList(modelConfigList);
                    modelVersionService.saveModelVersion(modelVersion);
                    modelGeneration.setTrainModelVersion(modelVersion);
                    modelGenerationService.saveModelGeneration(modelGeneration);
                    return new Msg<>(MsgCode.SUCCEED, modelVersion);

                case "质检":
                    ModelVersion modelVersion2 = new ModelVersion();
                    ModelGeneration modelGeneration2 = modelGenerationService.findModelGenerationByName(generationName);
                    if (modelGeneration2 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    String modelVersionName2 = englishName + '_' + newTag;
                    ModelVersion modelVersionInDatabase2 = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName2, ModelVersion.NOT_DELETE);
                    if (modelVersionInDatabase2 != null) {
                        return new Msg<>(MsgCode.IMAGE_EXITED);
                    }
                    modelVersion2.setName(modelVersionName2);
                    modelVersion2.setShowName(modelVersionName2);
                    modelVersion2.setCreateTime(createTime);
                    modelVersion2.setArchitecture("arm64");
                    modelVersion2.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                    modelVersion2.setInferable(false);
                    modelVersion2.setInspectable(true);
                    modelVersion2.setTrainable(false);
                    modelVersion2.setInspectNum("0-0-0-0-0");
                    modelVersion2.setTrainNum("0-0-0-0-0");
                    modelVersion2.setDescription(description);
                    modelVersion2.setUrl(modelVersionName2);
                    modelVersion2.setSize(2048);
                    modelVersion2.setReuse(false);
                    List<ModelConfig> modelConfigList2 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                                if (k.contains("HP_")) {
                                    ModelConfig modelConfig = new ModelConfig();
                                    modelConfig.setField(k);
                                    modelConfig.setRequired(true);
                                    modelConfig.setMsg(v);
                                    modelConfigService.saveModelConfig(modelConfig);
                                    modelConfigList2.add(modelConfig);
                                }
                            }
                    );
                    modelVersion2.setModelConfigList(modelConfigList2);
                    modelVersionService.saveModelVersion(modelVersion2);
                    modelGeneration2.setTestModelVersion(modelVersion2);
                    modelGenerationService.saveModelGeneration(modelGeneration2);
                    return new Msg<>(MsgCode.SUCCEED, modelVersion2);

                case "推理":

                    ModelVersion modelVersion1 = new ModelVersion();
                    ModelGeneration imageModel1 = modelGenerationService.findModelGenerationByName(generationName);
                    if (imageModel1 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    String modelVersionName1 = englishName + '_' + newTag;
                    ModelVersion modelVersionInDatabase1 = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName1, ModelVersion.NOT_DELETE);
                    modelVersion1.setName(modelVersionName1);
                    modelVersion1.setShowName(modelVersionName1);
                    modelVersion1.setCreateTime(createTime);
                    modelVersion1.setArchitecture("arm64");
                    modelVersion1.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                    modelVersion1.setInferable(true);
                    modelVersion1.setInspectable(false);
                    modelVersion1.setTrainable(false);
                    modelVersion1.setInspectNum("0-0-0-0-0");
                    modelVersion1.setTrainNum("0-0-0-0-0");
                    modelVersion1.setDescription(description);
                    modelVersion1.setUrl(modelVersionName1);
                    modelVersion1.setSize(2048);
                    modelVersion1.setReuse(false);
                    List<ModelConfig> modelConfigList1 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                                if (k.contains("HP_")) {
                                    ModelConfig modelConfig = new ModelConfig();
                                    modelConfig.setField(k);
                                    modelConfig.setRequired(true);
                                    modelConfig.setMsg(v);
                                    modelConfigService.saveModelConfig(modelConfig);
                                    modelConfigList1.add(modelConfig);
                                }
                            }
                    );
                    modelVersion1.setModelConfigList(modelConfigList1);
                    if (modelVersionInDatabase1 != null) {
                        modelVersionInDatabase1.setName(modelVersionInDatabase1.getName() + "(已删除)");
                        modelVersionService.saveModelVersion(modelVersionInDatabase1);
                    }
                    modelVersionService.saveModelVersion(modelVersion1);
                    imageModel1.setDeployModelVersion(modelVersion1);
                    modelGenerationService.saveModelGeneration(imageModel1);
//                    if (modelVersionInDatabase1 != null) {
//                        if (modelVersionService.replaceMineServiceOfDeploymentModelVersion(modelVersion1, modelVersionInDatabase1) &&
//                                modelVersionService.replaceMonitorBind(modelVersion1, modelVersionInDatabase1) &&
//                                modelVersionService.replaceModelDeployment(modelVersion1, modelVersionInDatabase1)) {
//                            modelVersionService.deleteModelVersion(modelVersionInDatabase1);
//                            return new Msg<>(MsgCode.SUCCEED, modelVersion1);
//                        }
//                    }else {
                    return new Msg<>(MsgCode.SUCCEED, modelVersion1);
//                    }
                default:
                    break;
            }
        }

        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    /**
     * 调用接口，把本地镜像上传到Harbor仓库
     * generationName 是生产任务的name
     * 这里的filePath的值是算法英文名，fileName是上传到Harbor仓库的镜像文件名称
     */
    public Msg<ModelVersion> uploadImageToHarbor(String generationName, String filePath, String fileName, String imageType, String description) {
        String directoryPath = modelImageRootPath + "/" + filePath;
        String fileRealPath = directoryPath + "/" + fileName;
        File tempFile = new File(fileRealPath);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        MultipartFile file = getMultipartFile_new(tempFile);
        Date createTime = new Date();
        String newTag = "image" + createTime.getTime();
        if (file != null) {
            System.out.println("开始上传到harbor");
            Msg<Map<String, String>> mapMsg = harborController.uploadImage(file, filePath, newTag);
            if (mapMsg.getCode() == MsgCode.FAILED.getCode()) {
                return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
            }
            if (mapMsg.getCode() == MsgCode.SUCCEED.getCode()) {
                //判断镜像所选功能和环境变量是否一致
//                switch (imageType) {
//                    case "训练":
//                        if(!mapMsg.getPayload().get("MODEL_TRAINING").equals("true")){
//                            return new Msg<>(MsgCode.MODEL_FUNCTION_NOT_MATCHING_FAILED);
//                        }
//                        break;
//                    case "推理":
//                        if(!mapMsg.getPayload().get("MODEL_INFERENCE").equals("true")){
//                            return new Msg<>(MsgCode.MODEL_FUNCTION_NOT_MATCHING_FAILED);
//                        }
//                        break;
//                    default:
//                        break;
            }
            //根据所选镜像功能，新增镜像
            switch (imageType) {
                case "训练":
                    ModelVersion modelVersion = new ModelVersion();
                    ModelGeneration imageModel = modelGenerationService.findModelGenerationByName(generationName);
                    if (imageModel == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }

                    String modelVersionName = filePath + '_' + newTag;
                    String showModelVersionName = modelVersionName;
                    ModelVersion modelVersionInDatabase = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName, ModelVersion.NOT_DELETE);
                    if (modelVersionInDatabase != null) {
                        return new Msg<>(MsgCode.IMAGE_EXITED);
                    }
                    modelVersion.setName(modelVersionName);
                    modelVersion.setShowName(showModelVersionName);
                    modelVersion.setCreateTime(createTime);
                    modelVersion.setArchitecture("arm64");
                    modelVersion.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                    modelVersion.setInferable(false);
                    modelVersion.setInspectable(false);
                    modelVersion.setTrainable(true);
                    modelVersion.setInspectNum("0-0-0-0-0");
                    modelVersion.setTrainNum("0-0-0-0-0");
                    modelVersion.setDescription(description);
                    modelVersion.setUrl(modelVersionName);
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
                            }
                    );
                    modelVersion.setModelConfigList(modelConfigList);
                    modelVersionService.saveModelVersion(modelVersion);
                    imageModel.setTrainModelVersion(modelVersion);
                    modelGenerationService.saveModelGeneration(imageModel);
                    return new Msg<>(MsgCode.SUCCEED, modelVersion);

                case "质检":
                    ModelVersion modelVersion2 = new ModelVersion();
                    ModelGeneration imageModel2 = modelGenerationService.findModelGenerationByName(generationName);
                    if (imageModel2 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }

                    String modelVersionName2 = filePath + '_' + newTag;
                    String showModelVersionName2 = modelVersionName2;
                    ModelVersion modelVersionInDatabase2 = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName2, ModelVersion.NOT_DELETE);
                    if (modelVersionInDatabase2 != null) {
                        return new Msg<>(MsgCode.IMAGE_EXITED);
                    }
                    modelVersion2.setName(modelVersionName2);
                    modelVersion2.setShowName(showModelVersionName2);
                    modelVersion2.setCreateTime(createTime);
                    modelVersion2.setArchitecture("arm64");
                    modelVersion2.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                    modelVersion2.setInferable(false);
                    modelVersion2.setInspectable(true);
                    modelVersion2.setTrainable(false);
                    modelVersion2.setInspectNum("0-0-0-0-0");
                    modelVersion2.setTrainNum("0-0-0-0-0");
                    modelVersion2.setDescription(description);
                    modelVersion2.setUrl(modelVersionName2);
                    modelVersion2.setSize(2048);
                    modelVersion2.setReuse(false);
                    List<ModelConfig> modelConfigList2 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                                if (k.contains("HP_")) {
                                    ModelConfig modelConfig = new ModelConfig();
                                    modelConfig.setField(k);
                                    modelConfig.setRequired(true);
                                    modelConfig.setMsg(v);
                                    modelConfigService.saveModelConfig(modelConfig);
                                    modelConfigList2.add(modelConfig);
                                }
                            }
                    );
                    modelVersion2.setModelConfigList(modelConfigList2);
                    modelVersionService.saveModelVersion(modelVersion2);
                    imageModel2.setTestModelVersion(modelVersion2);
                    modelGenerationService.saveModelGeneration(imageModel2);
                    return new Msg<>(MsgCode.SUCCEED, modelVersion2);

                case "推理":

                    ModelVersion modelVersion1 = new ModelVersion();
                    ModelGeneration imageModel1 = modelGenerationService.findModelGenerationByName(generationName);
                    if (imageModel1 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    String modelVersionName1 = filePath + '_' + newTag;
                    String showModelVersionName1 = modelVersionName1;
                    ModelVersion modelVersionInDatabase1 = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName1, ModelVersion.NOT_DELETE);
                    modelVersion1.setName(modelVersionName1);
                    modelVersion1.setShowName(showModelVersionName1);
                    modelVersion1.setCreateTime(createTime);
                    modelVersion1.setArchitecture("arm64");
                    modelVersion1.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                    modelVersion1.setInferable(true);
                    modelVersion1.setInspectable(false);
                    modelVersion1.setTrainable(false);
                    modelVersion1.setInspectNum("0-0-0-0-0");
                    modelVersion1.setTrainNum("0-0-0-0-0");
                    modelVersion1.setDescription(description);
                    modelVersion1.setUrl(modelVersionName1);
                    modelVersion1.setSize(2048);
                    modelVersion1.setReuse(false);
                    List<ModelConfig> modelConfigList1 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                                if (k.contains("HP_")) {
                                    ModelConfig modelConfig = new ModelConfig();
                                    modelConfig.setField(k);
                                    modelConfig.setRequired(true);
                                    modelConfig.setMsg(v);
                                    modelConfigService.saveModelConfig(modelConfig);
                                    modelConfigList1.add(modelConfig);
                                }
                            }
                    );
                    modelVersion1.setModelConfigList(modelConfigList1);
                    if (modelVersionInDatabase1 != null) {
                        modelVersionInDatabase1.setName(modelVersionInDatabase1.getName() + "(已删除)");
                        modelVersionService.saveModelVersion(modelVersionInDatabase1);
                    }
                    modelVersionService.saveModelVersion(modelVersion1);
                    imageModel1.setDeployModelVersion(modelVersion1);
                    modelGenerationService.saveModelGeneration(imageModel1);
//                        if (modelVersionInDatabase1 != null) {
//                            if (modelVersionService.replaceMineServiceOfDeploymentModelVersion(modelVersion1, modelVersionInDatabase1) &&
//                                    modelVersionService.replaceMonitorBind(modelVersion1, modelVersionInDatabase1) &&
//                                    modelVersionService.replaceModelDeployment(modelVersion1, modelVersionInDatabase1)) {
//                                modelVersionService.deleteModelVersion(modelVersionInDatabase1);
                    return new Msg<>(MsgCode.SUCCEED, modelVersion1);

//                        }else {
//                            return new Msg<>(MsgCode.SUCCEED, modelVersion1);
//                        }
                default:
                    break;
            }
        }

        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    /**
     * 更新镜像对训练镜像和质检镜像的job进行删除
     *
     * @param modelGeneration
     * @param imageType
     */
    public void deleteOldJob(ModelGeneration modelGeneration, String imageType) {
        if (imageType.equals("训练")) {
            ModelVersion trainModelVersion = modelGeneration.getTrainModelVersion();
            List<ModelJob> trainModelJobs = modelJobService.findModelJobsByModelVersionIdAndAndStatus(trainModelVersion.getId(), ModelJobStateCodeConstant.TRAINING);
            for (ModelJob modelJob : trainModelJobs) {
                jobService.deleteJob(namespace, modelJob.getName());
                modelJob.setStatus(ModelJobStateCodeConstant.CANCELED);
                modelJobService.saveModelJob(modelJob);
            }
        }
        if (imageType.equals("质检")) {
            ModelVersion testModelVersion = modelGeneration.getTestModelVersion();
            List<ModelJob> testModelJobs = modelJobService.findModelJobsByModelVersionIdAndAndStatus(testModelVersion.getId(), ModelJob.INSPECTING);
            for (ModelJob modelJob : testModelJobs) {
                jobService.deleteJob(namespace, modelJob.getName());
                modelJob.setStatus(ModelJobStateCodeConstant.CANCELED);
                modelJobService.saveModelJob(modelJob);
            }
        }
    }

    public Msg<ModelVersion> updateUrlImage(String url, String generationName, String englishName, String description, String imageType) {
        System.out.println("开始上传到harbor");
        System.out.println("imageType:" + imageType);
        String newTag = null;
        Date createTime = new Date();
        newTag = "image" + createTime.getTime();
        Msg<Map<String, String>> mapMsg = harborController.urlUploadImage(url, englishName, newTag);
        if (mapMsg.getCode() == MsgCode.FAILED.getCode()) {
            return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
        }
        if (mapMsg.getCode() == MsgCode.SUCCEED.getCode()) {
            if (imageType.equals("训练")) {
                ModelGeneration modelGeneration1 = modelGenerationService.findModelGenerationByName(generationName);
                if (modelGeneration1 == null) {
                    return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                }
                if (modelGeneration1.getTrainModelVersion() != null) {
                    //删除旧作业
                    deleteOldJob(modelGeneration1, "训练");
                    //修改版本信息
                    ModelVersion newTrainModelVersion1 = modelGeneration1.getTrainModelVersion();
                    String modelVersionName1 = englishName + '_' + newTag;
                    String showModelVersionName1 = modelVersionName1;
                    newTrainModelVersion1.setName(modelVersionName1);
                    newTrainModelVersion1.setShowName(modelVersionName1);
                    newTrainModelVersion1.setUrl(modelVersionName1);
                    newTrainModelVersion1.setCreateTime(createTime);
                    newTrainModelVersion1.setDescription(description);
                    List<ModelConfig> modelConfigList1 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                        if (k.contains("HP_")) {
                            ModelConfig modelConfig = new ModelConfig();
                            modelConfig.setField(k);
                            modelConfig.setRequired(true);
                            modelConfig.setMsg(v);
                            modelConfigService.saveModelConfig(modelConfig);
                            modelConfigList1.add(modelConfig);
                        }
                    });
                    newTrainModelVersion1.setModelConfigList(modelConfigList1);
                    modelGenerationRepo.save(modelGeneration1);
                    modelVersionService.saveModelVersion(newTrainModelVersion1);
                    return new Msg<>(MsgCode.SUCCEED, newTrainModelVersion1);
                }
                return new Msg<>(MsgCode.TRAIN_IMAGE_NOT_EXIST);
            }
            if (imageType.equals("质检")) {
                ModelGeneration modelGeneration2 = modelGenerationService.findModelGenerationByName(generationName);
                if (modelGeneration2 == null) {
                    return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                }
                if (modelGeneration2.getTestModelVersion() != null) {
                    //删除旧作业
                    deleteOldJob(modelGeneration2, "质检");
                    //修改版本信息
                    ModelVersion newTestModelVersion = modelGeneration2.getTestModelVersion();
                    String modelVersionName2 = englishName + '_' + newTag;
                    String showModelVersionName2 = modelVersionName2;
                    newTestModelVersion.setName(modelVersionName2);
                    newTestModelVersion.setShowName(modelVersionName2);
                    newTestModelVersion.setUrl(modelVersionName2);
                    newTestModelVersion.setCreateTime(createTime);
                    newTestModelVersion.setDescription(description);
                    List<ModelConfig> modelConfigList2 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                        if (k.contains("HP_")) {
                            ModelConfig modelConfig = new ModelConfig();
                            modelConfig.setField(k);
                            modelConfig.setRequired(true);
                            modelConfig.setMsg(v);
                            modelConfigService.saveModelConfig(modelConfig);
                            modelConfigList2.add(modelConfig);
                        }
                    });
                    newTestModelVersion.setModelConfigList(modelConfigList2);
                    modelVersionService.saveModelVersion(newTestModelVersion);
//                    if (modelGeneration2.getStatus() == ModelGeneration.TESTING
//                            || modelGeneration2.getStatus() == ModelGeneration.TEST_FAIL
//                            || modelGeneration2.getStatus() == ModelGeneration.TEST_SUCCESS) {
//                        modelGeneration2.setStatus(ModelGeneration.TRAIN_SUCCESS);
//                        modelGenerationRepo.save(modelGeneration2);
//                    }
                    return new Msg<>(MsgCode.SUCCEED, newTestModelVersion);
                }
                return new Msg<>(MsgCode.TEST_IMAGE_NOT_EXIST);
            }
            if (imageType.equals("推理")) {
                ModelGeneration modelGeneration3 = modelGenerationService.findModelGenerationByName(generationName);
                if (modelGeneration3 == null) {
                    return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                }
                if (modelGeneration3.getDeployModelVersion() != null) {
                    //修改版本信息
                    ModelVersion oldDeployModelVersion3 = modelGeneration3.getDeployModelVersion();
                    String modelVersionName3 = englishName + '_' + newTag;
                    String showModelVersionName3 = modelVersionName3;
                    oldDeployModelVersion3.setName(modelVersionName3);
                    oldDeployModelVersion3.setShowName(modelVersionName3);
                    oldDeployModelVersion3.setUrl(modelVersionName3);
                    oldDeployModelVersion3.setCreateTime(createTime);
                    oldDeployModelVersion3.setDescription(description);
                    List<ModelConfig> modelConfigList3 = new ArrayList<>();
                    mapMsg.getPayload().forEach((k, v) -> {
                        if (k.contains("HP_")) {
                            ModelConfig modelConfig = new ModelConfig();
                            modelConfig.setField(k);
                            modelConfig.setRequired(true);
                            modelConfig.setMsg(v);
                            modelConfigService.saveModelConfig(modelConfig);
                            modelConfigList3.add(modelConfig);
                        }
                    });
                    oldDeployModelVersion3.setModelConfigList(modelConfigList3);
                    modelVersionService.saveModelVersion(oldDeployModelVersion3);
                    return new Msg<>(MsgCode.SUCCEED, oldDeployModelVersion3);
                }
                return new Msg<>(MsgCode.DEPLOY_IMAGE_NOT_EXIST);
            }
        }

        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    public Msg<ModelVersion> updateLocalImage(String generationName, String filePath, String fileName, String imageType, String description) {
        String directoryPath = modelImageRootPath + "/" + filePath;
        String fileRealPath = directoryPath + "/" + fileName;
        File tempFile = new File(fileRealPath);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        MultipartFile file = getMultipartFile_new(tempFile);
        Date createTime = new Date();
        String newTag = "image" + createTime.getTime();
        if (file != null) {
            System.out.println("开始上传到harbor");
            Msg<Map<String, String>> mapMsg = harborController.uploadImage(file, filePath, newTag);
            if (mapMsg.getCode() == MsgCode.FAILED.getCode()) {
                return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
            }
            if (mapMsg.getCode() == MsgCode.SUCCEED.getCode()) {
                if (imageType.equals("训练")) {

                    ModelGeneration modelGeneration1 = modelGenerationService.findModelGenerationByName(generationName);
                    if (modelGeneration1 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    if (modelGeneration1.getTrainModelVersion() != null) {
                        //删除旧作业
                        deleteOldJob(modelGeneration1, "训练");
                        //修改版本信息
                        ModelVersion newTrainModelVersion1 = modelGeneration1.getTrainModelVersion();
                        String modelVersionName1 = filePath + '_' + newTag;
                        String showModelVersionName1 = modelVersionName1;
                        newTrainModelVersion1.setName(modelVersionName1);
                        newTrainModelVersion1.setShowName(modelVersionName1);
                        newTrainModelVersion1.setUrl(modelVersionName1);
                        newTrainModelVersion1.setCreateTime(createTime);
                        newTrainModelVersion1.setDescription(description);
                        List<ModelConfig> modelConfigList1 = new ArrayList<>();
                        mapMsg.getPayload().forEach((k, v) -> {
                            if (k.contains("HP_")) {
                                ModelConfig modelConfig = new ModelConfig();
                                modelConfig.setField(k);
                                modelConfig.setRequired(true);
                                modelConfig.setMsg(v);
                                modelConfigService.saveModelConfig(modelConfig);
                                modelConfigList1.add(modelConfig);
                            }
                        });
                        newTrainModelVersion1.setModelConfigList(modelConfigList1);
                        //modelGeneration1.setStatus(ModelGeneration.CREATE_SUCCESS);
                        modelGenerationRepo.save(modelGeneration1);
                        modelVersionService.saveModelVersion(newTrainModelVersion1);
                        return new Msg<>(MsgCode.SUCCEED, newTrainModelVersion1);
                    }
                    return new Msg<>(MsgCode.TRAIN_IMAGE_NOT_EXIST);
                }
                if (imageType.equals("质检")) {
                    ModelGeneration modelGeneration2 = modelGenerationService.findModelGenerationByName(generationName);
                    if (modelGeneration2 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    if (modelGeneration2.getTestModelVersion() != null) {
                        //删除旧作业
                        deleteOldJob(modelGeneration2, "质检");
                        //修改版本信息
                        ModelVersion newTestModelVersion = modelGeneration2.getTestModelVersion();
                        String modelVersionName2 = filePath + '_' + newTag;
                        String showModelVersionName2 = modelVersionName2;
                        newTestModelVersion.setName(modelVersionName2);
                        newTestModelVersion.setShowName(modelVersionName2);
                        newTestModelVersion.setUrl(modelVersionName2);
                        newTestModelVersion.setCreateTime(createTime);
                        newTestModelVersion.setDescription(description);
                        List<ModelConfig> modelConfigList2 = new ArrayList<>();
                        mapMsg.getPayload().forEach((k, v) -> {
                            if (k.contains("HP_")) {
                                ModelConfig modelConfig = new ModelConfig();
                                modelConfig.setField(k);
                                modelConfig.setRequired(true);
                                modelConfig.setMsg(v);
                                modelConfigService.saveModelConfig(modelConfig);
                                modelConfigList2.add(modelConfig);
                            }
                        });
                        newTestModelVersion.setModelConfigList(modelConfigList2);
                        modelVersionService.saveModelVersion(newTestModelVersion);
//                        if (modelGeneration2.getStatus() == ModelGeneration.TESTING
//                                || modelGeneration2.getStatus() == ModelGeneration.TEST_FAIL
//                                || modelGeneration2.getStatus() == ModelGeneration.TEST_SUCCESS) {
//                            modelGeneration2.setStatus(ModelGeneration.TRAIN_SUCCESS);
//                            modelGenerationRepo.save(modelGeneration2);
//                        }
                        return new Msg<>(MsgCode.SUCCEED, newTestModelVersion);
                    }
                    return new Msg<>(MsgCode.TEST_IMAGE_NOT_EXIST);
                }

                if (imageType.equals("推理")) {
                    ModelGeneration modelGeneration3 = modelGenerationService.findModelGenerationByName(generationName);
                    if (modelGeneration3 == null) {
                        return new Msg<>(MsgCode.MODEL_GENERATION_NOT_EXITED);
                    }
                    if (modelGeneration3.getDeployModelVersion() != null) {
                        //修改版本信息
                        ModelVersion oldDeployModelVersion3 = modelGeneration3.getDeployModelVersion();
                        String modelVersionName3 = filePath + '_' + newTag;
                        String showModelVersionName3 = modelVersionName3;
                        oldDeployModelVersion3.setName(modelVersionName3);
                        oldDeployModelVersion3.setShowName(modelVersionName3);
                        oldDeployModelVersion3.setUrl(modelVersionName3);
                        oldDeployModelVersion3.setCreateTime(createTime);
                        oldDeployModelVersion3.setDescription(description);
                        List<ModelConfig> modelConfigList3 = new ArrayList<>();
                        mapMsg.getPayload().forEach((k, v) -> {
                            if (k.contains("HP_")) {
                                ModelConfig modelConfig = new ModelConfig();
                                modelConfig.setField(k);
                                modelConfig.setRequired(true);
                                modelConfig.setMsg(v);
                                modelConfigService.saveModelConfig(modelConfig);
                                modelConfigList3.add(modelConfig);
                            }
                        });
                        oldDeployModelVersion3.setModelConfigList(modelConfigList3);
                        modelVersionService.saveModelVersion(oldDeployModelVersion3);
                        return new Msg<>(MsgCode.SUCCEED, oldDeployModelVersion3);
                    }
                    return new Msg<>(MsgCode.DEPLOY_IMAGE_NOT_EXIST);
                }
            }
        }
        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    public static MultipartFile getMultipartFile_new(File file) {
        FileItem item = new DiskFileItemFactory().createItem("file"
                , MediaType.MULTIPART_FORM_DATA_VALUE
                , true
                , file.getName());
        try (InputStream input = new FileInputStream(file);
             OutputStream os = item.getOutputStream()) {
            // 流转移
            IOUtils.copy(input, os);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid file: " + e, e);
        }

        return new CommonsMultipartFile(item);
    }


    /**
     * 把file转化为multipartFile
     */
    public static MultipartFile getMultipartFile(File file) {
        DiskFileItem item = new DiskFileItem("file"
                , MediaType.MULTIPART_FORM_DATA_VALUE
                , true
                , file.getName()
                , (int) file.length()
                , file.getParentFile());
        try {
            OutputStream os = item.getOutputStream();
            os.write(FileUtils.readFileToByteArray(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CommonsMultipartFile(item);
    }

    /**
     * 把MultipartFile转化为File文件
     */
    public static MultipartFile getMultipartFile(InputStream inputStream, String fileName) {
        FileItem fileItem = createFileItem(inputStream, fileName);
        return new CommonsMultipartFile(fileItem);
    }

    /**
     * 创建FileItem文件
     */
    public static FileItem createFileItem(InputStream inputStream, String fileName) {
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        FileItem fileItem = factory.createItem("file", MediaType.MULTIPART_FORM_DATA_VALUE, true, fileName);
        int read = 0;
        OutputStream os = null;
        byte[] buffer = new byte[10 * 1024 * 1024];
        try {
            os = fileItem.getOutputStream();
            while ((read = inputStream.read(buffer, 0, 4096)) != -1) {
                os.write(buffer, 0, read);
            }
            inputStream.close();
        } catch (IOException e) {

            throw new IllegalArgumentException("文件流输出失败");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {

                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {

                }
            }
        }
        return fileItem;
    }

//    public Msg<String> isEnvExist(long modelId, String imageType, Map<String, String> envMap, String tag) throws IOException {
//        //上传镜像，model下同样modelVersion只准有一个训练和一个推理
//        Model model = modelService.findModelById(modelId);
//        String modelVersionShowName = model.getModelEnglishName() + "_" + tag;
//        String trainStatus = envMap.get("MODEL_INSPECT");
//        String inspectStatus = envMap.get("MODEL_TRAINING");
//        String deployStatus = envMap.get("MODEL_INFERENCE");
//        switch (imageType) {
//            case "训练":
//                if (trainStatus.equals("true")) {
//                    //根据modelVersionShowName找到所有的modelVersion
//                    List<ModelVersion> modelVersions = modelVersionRepo.findModelVersionsByShowNameAndIsDelete(modelVersionShowName, 0);
//                    if (modelVersions != null) {
//                        for (ModelVersion modelVersion : modelVersions) {
//                            if (modelVersion.isTrainable() == true) {
//                                if (modelVersionService.canDeleteModelVersion(modelVersion)) {
//                                    modelVersionService.deleteModelVersion(modelVersion);
//                                } else {
//                                    return new Msg<>(MsgCode.FAILED);
//                                }
//                            }
//                        }
//                    }
//                }
//                if (inspectStatus.equals("true")) {
//                    List<ModelVersion> modelVersions = modelVersionRepo.findModelVersionsByShowNameAndIsDelete(modelVersionShowName, 0);
//                    if (modelVersions != null) {
//                        for (ModelVersion modelVersion : modelVersions) {
//                            if (modelVersion.isInspectable() == true) {
//                                if (modelVersionService.canDeleteModelVersion(modelVersion)) {
//                                    modelVersionService.deleteModelVersion(modelVersion);
//                                } else {
//                                    return new Msg<>(MsgCode.FAILED);
//                                }
//                            }
//                        }
//                    }
//                }
//                break;
//            case "推理":
//                if (deployStatus.equals("true")) {
//                    List<ModelVersion> modelVersions = modelVersionRepo.findModelVersionsByShowNameAndIsDelete(modelVersionShowName, 0);
//                    if (modelVersions != null) {
//                        for (ModelVersion modelVersion : modelVersions) {
//                            if (modelVersion.isInferable() == true) {
//                                if (modelVersionService.canDeleteModelVersion(modelVersion)) {
//                                    modelVersionService.deleteModelVersion(modelVersion);
//                                } else {
//                                    return new Msg<>(MsgCode.FAILED);
//                                }
//                            }
//                        }
//                    }
//                }
//                break;
//            default:
//                break;
//        }
//
//        return new Msg<>(MsgCode.SUCCEED);
//    }

}
