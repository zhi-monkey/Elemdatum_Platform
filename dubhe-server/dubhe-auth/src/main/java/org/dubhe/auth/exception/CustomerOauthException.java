

package org.dubhe.auth.exception;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

/**
 * @description 定义自已的异常处理类
 * @date 2020-12-21
 */
@JsonSerialize(using = CustomerOauthExceptionSerializer.class)
public class CustomerOauthException extends OAuth2Exception {
    public CustomerOauthException(String msg, Throwable t) {
        super(msg, t);
    }

    public CustomerOauthException(String msg) {
        super(msg);
    }

}
