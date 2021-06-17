package ca.gc.aafc.dina.security.spring;

import ca.gc.aafc.dina.security.DinaAuthorizationService;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.lang.reflect.Method;

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
}
