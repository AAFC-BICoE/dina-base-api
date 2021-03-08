package ca.gc.aafc.dina.filter;

import cz.jirutka.rsql.parser.ast.Node;

/**
 * Functional Interface to Process a Rsql Node.
 */
public interface RsqlFilterAdapter {

  /**
   * Entry point to process the rsql filters. This method will receive the root node of the rsql filters and
   * will return the final rsql node to be used in the filtering process.
   *
   * @param node root node of the rsql filters
   * @return final node to be used in the filtering process cannot be null.
   */
  Node process(Node node);

}
