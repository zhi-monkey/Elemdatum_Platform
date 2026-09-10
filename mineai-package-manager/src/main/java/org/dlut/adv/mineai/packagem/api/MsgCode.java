package org.dlut.adv.mineai.packagem.api;

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

    NOT_CHANGE("MM_0050","未修改"),
    HARDWARE_PARAMS_EXIST("SS_0051","该配置已存在"),
    MODEL_GENERATION_PUBLISHED("SS_0053","该生产任务已经发布，不允许打回"),

    MODEL_VERSION_EXIST("SS_0054","镜像已存在，请修改名称或者版本号"),
    UPDATE_IMAGE_FAILED("SS_0055","镜像更新失败"),
    SUCCESS_IMAGE_FAILED("SS_0056","镜像更新成功"),
    IMAGE_EXIST("SS_0057", "镜像已经存在"),
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

    FILEPATH_IS_NOT_DIRECTORY("SS_6601", "文件路径不是文件夹")
    ;

    private final String code;
    private final String text;

}
