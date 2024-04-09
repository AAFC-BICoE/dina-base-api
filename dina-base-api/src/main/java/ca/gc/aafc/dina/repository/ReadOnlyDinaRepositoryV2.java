package ca.gc.aafc.dina.repository;

import java.util.List;
import java.util.function.Predicate;

import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.FilterExpression;
import ca.gc.aafc.dina.filter.FilterGroup;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.filter.SimpleObjectFilterHandlerV2;
import ca.gc.aafc.dina.service.ReadOnlyDinaService;

/**
 * Based repository for accessing read-only data.
 * @param <D>
 */
public class ReadOnlyDinaRepositoryV2<D> {

  private final ReadOnlyDinaService<D> service;

  public ReadOnlyDinaRepositoryV2(ReadOnlyDinaService<D> service) {
    this.service = service;
  }

  public List<D> findAll(String queryString) {
    QueryComponent queryComponents = QueryStringParser.parse(queryString);

    FilterComponent fc = queryComponents.getFilters();

    Predicate<D> predicate = handlePredicate(fc);
    return service.findAll(predicate, queryComponents.getPageOffset(), queryComponents.getPageLimit());
  }

  private Predicate<D> handlePredicate(FilterComponent fc) {
    Predicate<D> predicate = null;
    if (fc instanceof FilterExpression fex) {
      predicate = and(predicate, SimpleObjectFilterHandlerV2.buildPredicate(fex));
    } else if( fc instanceof FilterGroup fgrp) {
      // multiple values can be submitted with en EQUALS to create an OR.
      if (fgrp.getConjunction() == FilterGroup.Conjunction.OR) {
        predicate = handleOr(fgrp.getComponents());
      } else {
        predicate = handleAnd(fgrp.getComponents());
      }
    }
    return predicate;
  }

  private Predicate<D> handleOr(List<FilterComponent> orList) {
    Predicate<D> predicate = null;
    for (FilterComponent fc : orList) {
      if (fc instanceof FilterExpression fex) {
        predicate = or(predicate, SimpleObjectFilterHandlerV2.buildPredicate(fex));
      } else if (fc instanceof FilterGroup fg){
        predicate = or(predicate, handlePredicate(fg));
      }
    }
    return predicate;
  }

  private Predicate<D> handleAnd(List<FilterComponent> andList) {
    Predicate<D> predicate = null;
    for (FilterComponent fc : andList) {
      if (fc instanceof FilterExpression fex) {
        predicate = and(predicate, SimpleObjectFilterHandlerV2.buildPredicate(fex));
      } else if (fc instanceof FilterGroup fg){
        predicate = and(predicate, handlePredicate(fg));
      }
    }
    return predicate;
  }

  private Predicate<D> and(Predicate<D> current, Predicate<D> toAdd) {
    if( current == null) {
      return toAdd;
    }
    return current.and(toAdd);
  }

  private Predicate<D> or(Predicate<D> current, Predicate<D> toAdd) {
    if( current == null) {
      return toAdd;
    }
    return current.or(toAdd);
  }
}
