package com.aivle13.fin_audit_ai.global.entity;

import com.pgvector.PGvector;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * pgvector의 vector 타입을 float[]로 매핑하는 Hibernate 커스텀 타입.
 * PGobject 서브클래스(PGvector)는 addDataType 등록 없이도 setObject/getObject로 값을 주고받을 수 있다.
 */
public class VectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        Object value = rs.getObject(position);
        return value == null ? null : new PGvector(value.toString()).toArray();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, WrapperOptions options) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, new PGvector(value));
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return deepCopy((float[]) cached);
    }

    @Override
    public float[] replace(float[] original, float[] target, Object owner) {
        return deepCopy(original);
    }
}
