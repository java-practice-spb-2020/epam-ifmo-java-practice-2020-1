package com.ifmo.epampractice.dao;

import com.ifmo.epampractice.entity.Tests;
import com.ifmo.epampractice.service.DatabaseSource;
import com.ifmo.epampractice.service.IDAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestsDAO extends DatabaseSource implements IDAO<Tests> {
    private static final String INSERT_TESTS_QUERY = "INSERT INTO TESTS(title, description," +
            "subject_id, is_random, created_at, max_points, creator_id) VALUES(?,?,?,?,?,?,?) RETURNING id";
    private static final String INSERT_GROUPS_TESTS_QUERY = "INSERT INTO GROUPS_TESTS(test_id, group_id," +
            "is_necessary, max_attempts, deadline, time_limit) VALUES(?,?,?,?,?,?)";
    private static final String SELECT_ALL_QUERY = "SELECT id, title, description," +
            "subject_id, is_random, created_at, max_points, creator_id FROM TESTS";
    private static final String SELECT_BY_ID_QUERY = "SELECT title, description," +
            "subject_id, is_random, created_at, max_points, creator_id FROM TESTS WHERE id=?";
    private static final String SELECT_BY_TEST_ID_QUERY = "SELECT title, description," +
            "subject_id, is_random, created_at, max_points, creator_id FROM TESTS WHERE id=?";
    private static final String SELECT_BY_TEST_AND_GROUP_ID_QUERY = "SELECT is_necessary, max_attempts," +
            "deadline, time_limit FROM GROUPS_TESTS WHERE test_id=? AND group_id=?";
    private static final String UPDATE_TESTS_QUERY = "UPDATE TESTS SET title=?, description=?, " +
            "subject_id=?, is_random=?, created_at=?, max_points = ?, creator_id=? WHERE id=?";
    private static final String UPDATE_GROUPS_TESTS_QUERY = "UPDATE GROUPS_TESTS SET is_necessary=?, " +
            "max_attempts=?, deadline=?, time_limit = ? WHERE test_id=? AND group_id=?";
    private static final String REMOVE_QUERY = "DELETE FROM TESTS WHERE id=?";
    private static final String REMOVE_GROUPS_TESTS_QUERY = "DELETE FROM GROUPS_TESTS WHERE test_id=?";

    @Override
    public Tests addObject(Tests test) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement(INSERT_TESTS_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            fillTestsQueryFromObject(test, preparedStatement);
            executeStatementAndCheck(test, preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    public Tests addTestsWithGroupsTests(Tests test) {
        this.addObject(test);
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement =
                     connection.prepareStatement(INSERT_GROUPS_TESTS_QUERY, Statement.RETURN_GENERATED_KEYS)) {
            fillGroupsTestsQueryFromObject(test, preparedStatement);
            executeStatementAndCheck(test, preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    public Tests addGroupsTests(Tests test) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_GROUPS_TESTS_QUERY)) {
            fillGroupsTestsQueryFromObject(test, preparedStatement);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating record in GroupsTests failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    @Override
    public List<Tests> getAll() {
        List<Tests> testsList = new ArrayList<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(SELECT_ALL_QUERY);
            while (resultSet.next()) {
                Tests test = new Tests();
                test.setId(resultSet.getInt("id"));
                fillGeneralTestObjectFromResultSet(test, resultSet);
                testsList.add(test);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return testsList;
    }

    @Override
    public Tests getById(int id) {
        Tests test = new Tests();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_ID_QUERY)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            test.setId(id);
            fillGeneralTestObjectFromResultSet(test, resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    public Tests getObjectByTestAndGroupId(int testId, int groupId) {
        Tests test = new Tests();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatementTest = connection.prepareStatement(SELECT_BY_TEST_ID_QUERY);
             PreparedStatement preparedStatementGroup =
                     connection.prepareStatement(SELECT_BY_TEST_AND_GROUP_ID_QUERY)) {
            preparedStatementTest.setInt(1, testId);
            ResultSet resultSetTest = preparedStatementTest.executeQuery();
            resultSetTest.next();
            test.setId(testId);
            fillGeneralTestObjectFromResultSet(test, resultSetTest);
            preparedStatementGroup.setInt(1, testId);
            preparedStatementGroup.setInt(2, groupId);
            ResultSet resultSetGroup = preparedStatementGroup.executeQuery();
            resultSetGroup.next();
            test.setGroupId(groupId);
            fillTestForGroupObjectFromResultSet(test, resultSetGroup);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    public Tests fillObjectByGroupId(Tests test, int groupId) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_TEST_AND_GROUP_ID_QUERY)) {
            preparedStatement.setInt(1, test.getId());
            preparedStatement.setInt(2, groupId);
            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();
            fillTestForGroupObjectFromResultSet(test, resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return test;
    }

    @Override
    public void updateByObject(Tests test) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_TESTS_QUERY)) {
            fillTestsQueryFromObject(test, preparedStatement);
            preparedStatement.setInt(8, test.getId());
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Update test failed, no rows affected.");
            }
            if (test.getGroupId() != 0) {
                updateGroupsTests(test);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateGroupsTests(Tests test) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_GROUPS_TESTS_QUERY)) {
            fillTestsQueryFromObject(test, preparedStatement);
            preparedStatement.setInt(8, test.getId());
            if (test.getGroupId() != 0) {
                preparedStatement.setBoolean(1, test.getIsNecessary());
                preparedStatement.setInt(2, test.getMaxAttempts());
                preparedStatement.setDate(3, test.getDeadline());
                preparedStatement.setInt(4, test.getTimeLimit());
                preparedStatement.setInt(5, test.getId());
                preparedStatement.setInt(6, test.getGroupId());
            }
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Update groups_tests failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeById(int id) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(REMOVE_QUERY)) {
            preparedStatement.setInt(1, id);
            Tests test = getById(id);
            if (isGroupsTestsObjectExistsByTestId(test.getId())) {
                removeGroupsTestsById(id);
            }
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Remove test failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeGroupsTestsById(int id) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(REMOVE_GROUPS_TESTS_QUERY)) {
            preparedStatement.setInt(1, id);
            int affectedRows = preparedStatement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Remove groups_tests failed, no rows affected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeStatementAndCheck(Tests test, PreparedStatement preparedStatement) throws SQLException {
        int affectedRows = preparedStatement.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Creating test failed, no rows affected.");
        }
        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                test.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("Creating test failed, no ID obtained.");
            }
        }
    }

    private void fillGeneralTestObjectFromResultSet(Tests test, ResultSet resultSet) throws SQLException {
        test.setTitle(resultSet.getString("title"));
        test.setDescription(resultSet.getString("description"));
        test.setSubjectId(resultSet.getInt("subject_id"));
        test.setIsRandom(resultSet.getBoolean("is_random"));
        test.setCreatedAt(resultSet.getDate("created_at"));
        test.setMaxPoints(resultSet.getInt("max_points"));
        test.setCreatorId(resultSet.getInt("creator_id"));
    }

    private void fillTestForGroupObjectFromResultSet(Tests test, ResultSet resultSet) throws SQLException {
        test.setIsNecessary(resultSet.getBoolean("is_necessary"));
        test.setMaxAttempts(resultSet.getInt("max_attempts"));
        test.setDeadline(resultSet.getDate("deadline"));
        test.setTimeLimit(resultSet.getInt("time_limit"));
    }

    private void fillTestsQueryFromObject(Tests test, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, test.getTitle());
        preparedStatement.setString(2, test.getDescription());
        preparedStatement.setInt(3, test.getSubjectId());
        preparedStatement.setBoolean(4, test.getIsRandom());
        preparedStatement.setDate(5, test.getCreatedAt());
        preparedStatement.setInt(6, test.getMaxPoints());
        preparedStatement.setInt(7, test.getCreatorId());
    }

    private void fillGroupsTestsQueryFromObject(Tests test, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setInt(1, test.getId());
        preparedStatement.setInt(2, test.getGroupId());
        preparedStatement.setBoolean(3, test.getIsNecessary());
        preparedStatement.setInt(4, test.getMaxAttempts());
        preparedStatement.setDate(5, test.getDeadline());
        preparedStatement.setInt(6, test.getTimeLimit());
    }

    public Boolean isGroupsTestsObjectExistsByTestId(int id) {
        String query = "SELECT 1 FROM groups_tests WHERE test_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Boolean.FALSE;
    }
}
