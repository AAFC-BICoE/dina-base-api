package ca.gc.aafc.dina.filter;

import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.jpa.JPACriteriaQueryVisitor;

/**
 * Extends {@link JPACriteriaQueryVisitor} to fix enum handling in Hibernate 6.
 *
 * <p>The parent class calls {@code path.as(clazz)} unconditionally, which in
 * Hibernate 6 creates a {@code SelfRenderingSqmFunction} (SQL CAST) using the
 * global type registry. For enums, that registry defaults to ordinal/SMALLINT,
 * regardless of {@code @Enumerated(EnumType.STRING)} or
 * {@code @Type(PostgreSQLEnumType.class)} on the field.
 *
 * <p>The fix: for enum types, use the {@code Path} directly (no {@code .as()}),
 * so Hibernate infers the correct JDBC type from the field's declared mapping.
 */
public class EnumSafeJPACriteriaQueryVisitor<T, E> extends JPACriteriaQueryVisitor<T, E> {

    public EnumSafeJPACriteriaQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           Class<E> queryClass) {
        super(em, tClass, queryClass);
    }

    public EnumSafeJPACriteriaQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           Class<E> queryClass,
                                           Map<String, String> fieldMap) {
        super(em, tClass, queryClass, fieldMap);
    }

    public EnumSafeJPACriteriaQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           Class<E> queryClass,
                                           List<String> joinProps) {
        super(em, tClass, queryClass, joinProps);
    }

    public EnumSafeJPACriteriaQueryVisitor(EntityManager em,
                                           Class<T> tClass,
                                           Class<E> queryClass,
                                           Map<String, String> fieldMap,
                                           List<String> joinProps) {
        super(em, tClass, queryClass, fieldMap, joinProps);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Predicate doBuildPredicate(ConditionType ct, Path<?> path,
                                         Class<?> valueClazz, Object value) {

        if (valueClazz != null && valueClazz.isEnum()) {
            return buildEnumPredicate(ct, path, value);
        }

        // Non-enum: delegate to parent (original behavior preserved)
        return super.doBuildPredicate(ct, path, valueClazz, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate buildEnumPredicate(ConditionType ct, Path<?> path, Object value) {
        CriteriaBuilder cb = getCriteriaBuilder();

        // Use path directly — no .as() — correct type inferred from field mapping
        Expression exp = path;

        return switch (ct) {
            case EQUALS -> cb.equal(exp, value);
            case NOT_EQUALS -> cb.notEqual(exp, value);
            case GREATER_THAN -> cb.greaterThan(exp, (Comparable) value);
            case GREATER_OR_EQUALS -> cb.greaterThanOrEqualTo(exp, (Comparable) value);
            case LESS_THAN -> cb.lessThan(exp, (Comparable) value);
            case LESS_OR_EQUALS -> cb.lessThanOrEqualTo(exp, (Comparable) value);
            default -> throw new IllegalArgumentException(
                "Unsupported condition type for enum: " + ct);
        };
    }
}