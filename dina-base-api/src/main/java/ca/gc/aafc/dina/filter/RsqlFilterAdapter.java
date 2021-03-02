package ca.gc.aafc.dina.filter;

import cz.jirutka.rsql.parser.ast.Node;

/**
 * Functional Interface to Process a Rsql Node.
 */
public interface RsqlFilterAdapter {

  Node process(Node node);

}
