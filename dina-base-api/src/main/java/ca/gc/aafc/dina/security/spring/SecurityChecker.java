package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.DinaAuthorizationService;
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

  private static final SpelExpressionParser parser;

  static {
    parser = new SpelExpressionParser();
  }

  public boolean check(
    DinaAuthorizationService as,
    String securityExpression,
    Object entity,
    Method triggerCheckMethod
  ) {
    EvaluationContext evaluationContext = config.createExpressionHandler().createEvaluationContext(
      SecurityContextHolder.getContext().getAuthentication(),
      new SimpleMethodInvocation(as, triggerCheckMethod, entity));

    return ExpressionUtils.evaluateAsBoolean(parser.parseExpression(securityExpression), evaluationContext);
  }

  public Set<String> getPermissionsForObject(Object target, DinaAuthorizationService as) {
    Set<String> permissions = new HashSet<>();
    Method authorizeCreate = getMethod("authorizeCreate", as.getClass().getSuperclass());
    Method authorizeUpdate = getMethod("authorizeUpdate", as.getClass().getSuperclass());
    Method authorizeDelete = getMethod("authorizeDelete", as.getClass().getSuperclass());
    if (this.check(as, getPreAuthorizeExpression(authorizeCreate), target, authorizeCreate)) {
      permissions.add("create");
    }
    if (this.check(as, getPreAuthorizeExpression(authorizeUpdate), target, authorizeUpdate)) {
      permissions.add("delete");
    }
    if (this.check(as, getPreAuthorizeExpression(authorizeDelete), target, authorizeCreate)) {
      permissions.add("update");
    }
    return permissions;
  }

  private String getPreAuthorizeExpression(Method matchingMethod) {
    PreAuthorize annotation = matchingMethod.getAnnotation(PreAuthorize.class);
    return annotation.value();
  }

  private Method getMethod(String methodName, Class<?> aClass) {
    return MethodUtils.getMatchingMethod(aClass, methodName, Object.class);
  }
}
