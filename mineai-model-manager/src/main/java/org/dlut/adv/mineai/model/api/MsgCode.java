package org.dlut.adv.mineai.model.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dlut.adv.mineai.core.api.MsgCodeInf;

@AllArgsConstructor
@Getter
public enum MsgCode implements MsgCodeInf {

    // modelConfigList为空
    MODEL_CONFIG_LIST_IS_NULL("30004", "算法配置为空"),

    //登录实例
    SUCCEED("MS_20000", "成功"),
    FAILED("SS_40002", "失败"),
    TIMEOUT("MS_40000", "超时"),
    ENCODE_FAILED("SS_40003", "编码过程中出现异常"),
    LOGIN_USER_NOT_EXIST("SS_40001", "登录失败，用户名或密码错误"),

    ADD_USER_FAILED("SS_00002", "添加新账户失败"),

    DELETE_USER_FAILED("SS_00003", "删除新账户失败"),

    GET_USER_FAIL("SS_00004", "获取用户失败"),

    USER_EXIST("SS_00005", "当前用户已存在"),

    USER_NOT_EXIST("SS_00006", "更新用户信息失败"),

    Update_USER_FAILED("SS_00007", "更新用户信息失败"),

    ADD_MODEL_FAILED("SS_00008", "新增新算法失败"),

    DEPT_EXIST("SS_00009", "当前部门已存在"),

    MODEL_NOT_EXIST("SS_00012", "算法不存在"),

    UPDATE_MODEL_FAILED("SS_00010", "算法更新失败"),

    DELETE_MODEL_FAILED("SS_00011", "算法删除失败"),
    MODEL_IS_BIND("SS_00013", "算法已经被绑定"),

    BIND_MODEL_FAILED("SS_00014", "该监控设备已被删除，绑定算法失败"),
    DELETE_MONITOR_FAILED("SS_00003", "删除监控设备失败"),
    MODEL_NAME_EXIST("SS_00015", "算法名称重复"),
    MODEL_DESCRIPTION_EXIST("SS_00016", "算法描述重复"),
    MODEL_ENGLISH_NAME_EXIST("SS_00017", "算法英文名称重复"),
    MODEL_NAME_EMPTY("SS_00018", "算法名称为空"),
    MODEL_DESCRIPTION_EMPTY("SS_00019", "算法描述为空"),
    DELETE_MODEL_ALERT_FAILED("SS_000020", "删除报警信息失败"),
    UPDATE_MODEL_ALERT_FAILED("SS_000021", "更新视频报警失败"),
    MODEL_VERSION_NOT_EXIST("SS_000022","模型版本不存在"),
    DELETE_MODEL_VERSION_FAILED("SS_000023","模型版本删除失败"),

    UPDATE_MODEL_VERSION_FAILED("SS_000024","模型版本更新失败"),
    CANCEL_EXECUTED_MODEL_JOB_FAILED("SS_000025","作业已执行成功，无法取消"),
    CANCEL_FAILED_MODEL_JOB_FAILED("SS_000026","作业已执行失败，无法取消"),
    CANCEL_FAILED_MODEL_JOB_CANCELED("SS_000027","作业已被取消"),
    JOB_NOT_EXIST("SS_000028","作业不存在"),
    UPDATE_JOB_FAILED("SS_000029","更新作业失败"),
    ADD_MODEL_JOB_FAILED("SS_000030","新增作业失败"),
    UPLOAD_IMAGE_TO_HARBOR_FAILED("SS_000031","上传镜像至镜像仓库失败"),
    DELETE_IMAGE_FILE_FAILED("SS_000032","删除镜像临时文件失败"),
    IMAGE_EXITED("SS_000033","该镜像已上传至平台，请勿重复上传"),
    FILE_NOT_EXITED("SS_000034","镜像文件不存在"),
    MODEL_GENERATION_NOT_EXITED("SS_000035","生产任务不存在于平台"),
    DEPLOYMENT_EXITED("SS_000036","该算法已存在相同部署配置"),
    CONVERT_EXITED("SS_000037","该模型已经转换完毕"),
    MODEL_VERSION_DELETE_FAILED("SS_000038","此算法的镜像已被绑定或部署，请解除绑定关系再删除"),
    MODEL_FUNCTION_NOT_MATCHING_FAILED("SS_000039","选择的镜像任务和镜像功能不匹配"),
    MODEL_GENERATION_NAME_EXIST("SS_00040", "生产任务名称重复"),
    ADD_MODEL_GENERATION_FAILED("SS_00041", "新增生产任务失败"),
    MODEL_GENERATION_EMPTY("SS_00043", "生产任务名称不能为空"),
    MODEL_GENERATION_UPDATE_FAILED("SS_00044","更新生产任务失败"),
    DONT_NEED_TEST("SS_00045","无需质检，请直接发布"),
    TRAIN_JOB_NOT_FOUND("SS_00046","训练任务不存在"),
    TEST_JOB_NOT_FOUND("SS_00047","质检任务不存在"),
    TRAIN_IMAGE_NOT_EXIST("SS_0048","该生产任务未上传过训练镜像"),
    TEST_IMAGE_NOT_EXIST("SS_0048","该生产任务未上传过质检镜像"),
    DEPLOY_IMAGE_NOT_EXIST("SS_0048","该生产任务未上传过推理镜像"),
    CAN_NOT_PUBLISH("SS_0049","该生产任务模型效果未优于原模型，无法发布"),
    HAVE_NOT_TEST("SS_0052","该生产任务未经过质检，无法发布"),
    FAILED_TO_GET_WEIGHT_FILE("SS_0050","权重文件不存在"),
    FAILED_TO_START_AUTO_LABEL("SS_0051","自动标注任务启动失败"),
    IMAGE_ENV_NOT_EXIST("SS_0053","镜像环境变量为空"),
    FAILED_TO_GET_IMAGE_ENV("SS_0054","镜像不符合规范: 镜像中没读取到任何环境变量"),
    IMAGE_NOT_FOUND("SS_0055","镜像不存在: 请检查镜像仓库地址是否正确"),
    CONNECT_TO_HARBOR_TIMEOUT("SS_0050","网络错误: 连接镜像仓库超时"),
    UN_FILED_ERROR("SS_0055","未归档错误"),
    TRAIN_ENV_MISSING_OR_NOT_EXIST("SS_0056","镜像不符合规范: 训练镜像未传入以MODE开头的环境变量"),
    CONVERT_ENV_MISSING_OR_NOT_EXIST("SS_0057","镜像不符合规范: 转换镜像未传入以CONVERT开头的环境变量"),
    DEFAULT_LABELS_MISSING_OR_NOT_EXIST("SS_0058","镜像不符合规范: 自动标注镜像应有DEFAULT_LABELS环境变量, 用于描述自动标注镜像支持的标签种类"),
    DEFAULT_LABELS_WRONG_FORMAT("ss_0116", "镜像不符合规范: 自动标注镜像传入的DEFAULT_LABELS格式错误"),


    NOT_CHANGE("MM_0050","未修改"),
    HARDWARE_PARAMS_EXIST("SS_0051","该配置已存在"),
    MODEL_GENERATION_PUBLISHED("SS_0053","该生产任务已经发布，不允许打回"),

    MODEL_VERSION_EXIST("SS_0054","镜像已存在，请修改名称或者版本号"),
    UPDATE_IMAGE_FAILED("SS_0055","镜像更新失败"),
    SUCCESS_IMAGE_FAILED("SS_0056","镜像更新成功"),
    IMAGE_EXIST("SS_0057", "镜像已经存在"),
    IMAGE_NAME_EXIST("SS_0058", "镜像名称已存在"),
    NO_MODEL_EXPLORE("SS_0058", "算法为空，请先添加算法！！！"),
    AUTO_LABEL_START("SS_0059", "自动标注任务启动失败"),

    ADD_MODEL_CLASSIFICATION_SUCCESS("SS_0060", "新增算法分类成功"),
    ADD_MODEL_CLASSIFICATION_FAIL("SS_0061", "新增算法分类失败"),
    MODEL_CLASSIFICATION_EXIST("SS_0062", "算法分类名称已经存在"),
    DELETE_MODEL_CLASSIFICATION_SUCCESS("SS_0063", "删除算法分类成功"),
    UPDATE_MODEL_CLASSIFICATION_SUCCESS("SS_0064", "更新算法分类成功"),
    UPDATE_MODEL_CLASSIFICATION_FAIL("SS_0065", "更新算法分类失败"),
    GET_MODEL_CLASSIFICATION_SUCCESS("SS_0066", "查询算法分类成功"),
    GET_MODEL_CLASSIFICATION_FAIL("SS_0067", "更新算法分类失败"),
    MODEL_CLASSIFICATION_ID_NOT_EXISTS("SS_0068", "算法分类Id不存在"),
    CLASSES_NOT_EXISTS("SS_0069", "标签文件为空或不存在"),

    MODEL_EXPLORE_NAME_EXISTS("SS_0070", "算法名称已存在"),
    MODEL_GENERATION_DELETE_GUIDED_DATASET_ERROR("SS_0071", "生产任务删除失败-对应引导式数据集删除失败,请检查数据集状态"),

    USER_CPU_MEMORY_OVER_LIMIT("ss_0101", "当前部门的CPU和内存使用超过限制，请提升部门限额或等待其他任务结束释放资源"),
    USER_MEMORY_USED_OVER_LIMIT("ss_0102", "当前部门的内存使用超过限制，请提升部门限额或等待其他任务结束释放资源"),
    USER_CPU_USED_OVER_LIMIT("ss_0102", "当前部门的CPU使用超过限制，请提升部门限额或等待其他任务结束释放资源"),
    USER_CPU_MEMORY_DISTRIBUTED_FAILED("ss_0103", "CPU和内存分配失败"),
    DISTRIBUTED_FAILED_UNKNOWN_REASON("ss_0104", "未知原因，分配失败"),
    USER_GPU_USED_OVER_LIMIT("ss_0105", "当前部门的GPU使用超过限制，请提升部门限额或等待其他任务结束释放资源" ),
    USER_VGPU_CORES_USED_OVER_LIMIT("ss_0106", "当前部门的VGPU cores使用超过限制，请提升部门限额或等待其他任务结束释放资源"),
    FAILED_TO_GET_CHIP("ss_0107", "无效的芯片类型"),
    FAILED_GET_DATA_FROM_PROMETHEUS("ss_0108", "从Prometheus获取数据失败"),
    APPLICATION_IS_BINDED("ss_0109", "该应用已被绑定"),
    CHIP_IS_BINDED("ss_0110", "该算力芯片已被绑定，无法删除"),
    DEVICE_IS_BINDED("ss_0111", "该设备固件已被绑定，无法删除"),
    SCENE_IS_BINDED("ss_0112", "该场景已被绑定"),
    CURRENT_DATASET_HAS_NO_TAG_ERROR("ss_0113", "当前数据集没有标签"),
//    APPLICATION_NAME_EXIST("ss_0114", "应用名称重复"),
    SCENE_NAME_EXIST("ss_0115", "场景名称重复"),
    CHIP_TYPE_EXIST("ss_0114", "算力芯片类型已存在"),
    CHIP_NOT_EXIST("ss_0116", "算力芯片不存在"),
    CHIP_TYPE_NOT_PRESENT("ss_0117", "算力芯片类型参数获取失败"),
    DEVICE_NOT_EXIST("ss_0118", "设备固件不存在"),
    ;



    private final String code;
    private final String text;

}
