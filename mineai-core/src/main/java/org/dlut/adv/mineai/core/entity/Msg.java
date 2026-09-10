package org.dlut.adv.mineai.core.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.dlut.adv.mineai.core.api.MsgCodeInf;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class Msg<T> implements Serializable {
    private String code;
    private String text;
    private T payload;

    public Msg(MsgCodeInf msgCode) {
        this.code = msgCode.getCode();
        this.text = msgCode.getText();
    }

    public Msg(MsgCodeInf msgCode, T payload) {
        this(msgCode);
        this.payload = payload;
    }
}
