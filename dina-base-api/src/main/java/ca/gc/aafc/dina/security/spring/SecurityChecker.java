package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Service used to return the permissions of an object authorized from a {@link DinaAuthorizationService}.
 * <p>
 * Inspired by
 *
 * @see <a href="https://gist.github.com/matteocedroni/b0e5a935127316603dfb">Security Checker</a>
 */
@Service
public final class SecurityChecker {

  @AllArgsConstructor
  @Getter
  public enum Operations {
    CREATE("create"),
    READ("read"),
    UPDATE("update"),
    DELETE("delete");
    private final String value;
  }

  private final MethodSecurityConfig config;
  private static final SpelExpressionParser PARSER = new SpelExpressionParser();

  public SecurityChecker(Optional<MethodSecurityConfig> config) {
    this.config = config.orElse(null);
  }

  /**
   * Returns a set of permissions for a given object using a given {@link DinaAuthorizationService}
   *
   * @param target permissions of object to check
   * @param as     authorization service used in evaluation.
   * @return Returns a set of permissions for a given object
   */
  public Set<String> getPermissionsForObject(@NonNull Object target, @NonNull DinaAuthorizationService as) {
    Set<String> permissions = new HashSet<>();

    if (config == null) {
      return permissions;
    }

    if (this.checkObjectPreAuthorized(as, target, "authorizeCreate")) {
      permissions.add(Operations.CREATE.getValue());
    }
    if (this.checkObjectPreAuthorized(as, target, "authorizeDelete")) {
      permissions.add(Operations.DELETE.getValue());
    }
    if (this.checkObjectPreAuthorized(as, target, "authorizeUpdate")) {
      permissions.add(Operations.UPDATE.getValue());
    }
    return permissions;
  }

  private boolean checkObjectPreAuthorized(
    @NonNull DinaAuthorizationService as,
    @NonNull Object entity,
    @NonNull String methodName
  ) {
    Method preAuthorizeMethod = MethodUtils.getMatchingMethod(as.getClass(), methodName, Object.class);

    EvaluationContext evaluationContext = config.createExpressionHandler().createEvaluationContext(
      SecurityContextHolder.getContext().getAuthentication(),
      new SimpleMethodInvocation(as, preAuthorizeMethod, entity));

    return ExpressionUtils.evaluateAsBoolean(
      PARSER.parseExpression(getPreAuthorizeExpression(preAuthorizeMethod)), evaluationContext);
  }

  private String getPreAuthorizeExpression(Method matchingMethod) {
    PreAuthorize annotation = matchingMethod.getAnnotation(PreAuthorize.class);
    if (annotation == null || StringUtils.isBlank(annotation.value())) {
      throw new IllegalArgumentException("the given method does contain a valid PreAuthorizeAnnotation");
    }
    return annotation.value();
  }

}
