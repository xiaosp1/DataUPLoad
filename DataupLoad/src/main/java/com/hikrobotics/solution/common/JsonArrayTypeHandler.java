package com.hikrobotics.solution.common;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PG JSON 数组 TypeHandler（PSM 1:1）。
 * 将 JSON 数组字符串转逗号分隔的普通字符串。
 */
public class JsonArrayTypeHandler implements TypeHandler<String> {

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter);
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        String val = rs.getString(columnName);
        if (val == null) return null;
        return val.replaceAll("[\\[\\]\"]", "");
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        String val = rs.getString(columnIndex);
        if (val == null) return null;
        return val.replaceAll("[\\[\\]\"]", "");
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String val = cs.getString(columnIndex);
        if (val == null) return null;
        return val.replaceAll("[\\[\\]\"]", "");
    }
}
