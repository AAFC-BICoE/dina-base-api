package ca.gc.aafc.dina.security;

import java.io.Serializable;
import java.util.Comparator;

public class PriorityComparator implements Comparator<DinaRole>, Serializable {

  @Override
  public int compare(DinaRole firstDinaRole, DinaRole secondDinaRole) {
    return Integer.compare(firstDinaRole.getPriority(), secondDinaRole.getPriority());
  }
  
}
