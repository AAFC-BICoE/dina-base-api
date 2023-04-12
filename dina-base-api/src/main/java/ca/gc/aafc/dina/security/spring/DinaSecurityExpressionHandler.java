package ca.gc.aafc.dina.security.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;

/**
 * Custom security expression handler using a custom SecurityExpressionRoot
 */
public class DinaSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {
  private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

  @Override
  protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
      Authentication authentication, MethodInvocation invocation) {
    DinaPermissionEvaluator root = new DinaPermissionEvaluator(authentication);
    root.setPermissionEvaluator(getPermissionEvaluator());
    root.setTrustResolver(this.trustResolver);
    root.setRoleHierarchy(getRoleHierarchy());
    return root;
  }

}
