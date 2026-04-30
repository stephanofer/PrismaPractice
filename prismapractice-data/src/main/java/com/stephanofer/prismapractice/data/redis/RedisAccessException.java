package com.stephanofer.prismapractice.data.redis;

import com.stephanofer.prismapractice.data.DataAccessException;

public final class RedisAccessException extends DataAccessException {

    public RedisAccessException(String message) {
        super(message);
    }

    public RedisAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
