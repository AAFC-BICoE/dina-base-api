package ca.gc.aafc.dina;

import ca.gc.aafc.dina.filter.RsqlFilterAdapter;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

import java.util.stream.Collectors;

/**
 * Example usage of a RSQLVisitor and RsqlFilterAdapter.
 */
public class PersonRsqlAdapter implements RSQLVisitor<Node, String>, RsqlFilterAdapter {

  // This method comes from RsqlFilterAdapter
  @Override
  public Node process(Node node) {
    return node.accept(this);
  }

  // This method comes from RSQLVisitor
  @Override
  public Node visit(AndNode andNode, String field) {
    return resolveLogicalNode(andNode, field);
  }

  // This method comes from RSQLVisitor
  @Override
  public Node visit(OrNode orNode, String field) {
    return resolveLogicalNode(orNode, field);
  }

  // This method comes from RSQLVisitor
  @Override
  public Node visit(ComparisonNode comparisonNode, String field) {
    if (comparisonNode.getSelector().equalsIgnoreCase("customSearch")) {
      return doForCustomSearchField(comparisonNode);
    } else {
      return comparisonNode;
    }
  }

  private Node resolveLogicalNode(LogicalNode logicalNode, String field) {
    return logicalNode.withChildren(logicalNode.getChildren()
      .stream()
      .map(node -> node.accept(this, field))
      .collect(Collectors.toList()));
  }

  private static ComparisonNode doForCustomSearchField(ComparisonNode comparisonNode) {
    return new ComparisonNode(RSQLOperators.EQUAL, "uuid", comparisonNode.getArguments());
  }

}
