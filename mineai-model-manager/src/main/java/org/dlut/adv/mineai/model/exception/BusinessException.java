package org.dlut.adv.mineai.model.exception;


import lombok.Getter;
import lombok.Setter;

/**
 * 可知的业务异常 等价于 BizException的含义，但是这里继承RuntimeException用于避免方法上加throws
 *
 * @author LYW
 * @create 2022-01-19
 */
@Setter
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 详细的错误信息，用于给程序开发人员进行错误分析用
     */
    private String detailMsg;
    /**
     * 用户可看的信息，避免一大串的报错信息反馈给用户，userMsg要通俗易懂，言简意赅，一些用户不必要知道的信息统统用“系统异常，请联系管理员”代替
     */
    private String userMsg;

    public BusinessException(String msg) {
        super(msg);
        userMsg = msg;
    }

    public BusinessException(String msg, String detailMsg) {
        super("userMsg:" + msg + ",detailMsg:" + detailMsg);
        this.userMsg = msg;
        this.detailMsg = detailMsg;
    }
}

