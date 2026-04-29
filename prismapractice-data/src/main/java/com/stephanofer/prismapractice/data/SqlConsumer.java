package com.stephanofer.prismapractice.data;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlConsumer<T> {

    void accept(T value) throws SQLException;
}
