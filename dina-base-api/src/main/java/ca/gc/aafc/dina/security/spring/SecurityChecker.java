package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.DinaAuthorizationService;
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

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Inspired by
 *
 * @see <a href="https://gist.github.com/matteocedroni/b0e5a935127316603dfb">Security Checker</a>
 */
@Service
public final class SecurityChecker {

  @Inject
  private MethodSecurityConfig config;

  private static final SpelExpressionParser parser = new SpelExpressionParser();

  public Set<String> getPermissionsForObject(@NonNull Object target, @NonNull DinaAuthorizationService as) {
    Set<String> permissions = new HashSet<>();
    if (this.checkObjectPreAuthorized(as, target, "authorizeCreate")) {
      permissions.add("create");
    }
    if (this.checkObjectPreAuthorized(as, target, "authorizeUpdate")) {
      permissions.add("delete");
    }
    if (this.checkObjectPreAuthorized(as, target, "authorizeDelete")) {
      permissions.add("update");
    }
    return permissions;
  }

  private boolean checkObjectPreAuthorized(
    @NonNull DinaAuthorizationService as,
    @NonNull Object entity,
    @NonNull String methodName
  ) {
    Method preAuthorizeMethod = MethodUtils.getMatchingMethod(
      as.getClass().getSuperclass(), methodName, Object.class);

    EvaluationContext evaluationContext = config.createExpressionHandler().createEvaluationContext(
      SecurityContextHolder.getContext().getAuthentication(),
      new SimpleMethodInvocation(as, preAuthorizeMethod, entity));

    return ExpressionUtils.evaluateAsBoolean(
      parser.parseExpression(getPreAuthorizeExpression(preAuthorizeMethod)), evaluationContext);
  }

  private String getPreAuthorizeExpression(Method matchingMethod) {
    PreAuthorize annotation = matchingMethod.getAnnotation(PreAuthorize.class);
    if (annotation == null || StringUtils.isBlank(annotation.value())) {
      throw new IllegalArgumentException("the given method does contain a valid PreAuthorizeAnnotation");
    }
    return annotation.value();
  }

}
