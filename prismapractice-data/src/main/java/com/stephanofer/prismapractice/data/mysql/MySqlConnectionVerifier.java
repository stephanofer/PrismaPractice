package com.stephanofer.prismapractice.data.mysql;

import com.stephanofer.prismapractice.data.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public final class MySqlConnectionVerifier {

    public void verify(DataSource dataSource, String testQuery) {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(testQuery, "testQuery");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(testQuery);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new DataAccessException("Database connectivity test query returned no rows");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to verify MySQL connectivity", exception);
        }
    }
}
