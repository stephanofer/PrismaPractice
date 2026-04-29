package com.stephanofer.prismapractice.data;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcExecutor {

    private final DataSource dataSource;

    public JdbcExecutor(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public int update(String sql) {
        return update(sql, statement -> {
        });
    }

    public int update(String sql, SqlConsumer<PreparedStatement> binder) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(binder, "binder");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to execute update: " + sql, exception);
        }
    }

    public int[] batch(String sql, List<SqlConsumer<PreparedStatement>> binders) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(binders, "binders");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SqlConsumer<PreparedStatement> binder : binders) {
                binder.accept(statement);
                statement.addBatch();
            }
            return statement.executeBatch();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to execute batch: " + sql, exception);
        }
    }

    public <T> List<T> query(String sql, SqlFunction<ResultSet, T> mapper) {
        return query(sql, statement -> {
        }, mapper);
    }

    public <T> List<T> query(String sql, SqlConsumer<PreparedStatement> binder, SqlFunction<ResultSet, T> mapper) {
        Objects.requireNonNull(sql, "sql");
        Objects.requireNonNull(binder, "binder");
        Objects.requireNonNull(mapper, "mapper");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.accept(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> values = new ArrayList<>();
                while (resultSet.next()) {
                    values.add(mapper.apply(resultSet));
                }
                return List.copyOf(values);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to execute query: " + sql, exception);
        }
    }

    public <T> Optional<T> queryOne(String sql, SqlConsumer<PreparedStatement> binder, SqlFunction<ResultSet, T> mapper) {
        List<T> values = query(sql, binder, mapper);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.getFirst());
    }
}
