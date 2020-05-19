package ca.gc.aafc.dina.testsupport;

/**
 * Immutable class (for testing) representing a Relationship.
 *
 */
public class Relationship {

  private final String name;
  private final String type;
  private final String id;
  
  public static Relationship of(String name, String type, String id) {
    return new Relationship(name, type, id);
  }
  
  private Relationship(String name, String type, String id) {
    this.name = name;
    this.type = type;
    this.id = id;
  }
  
  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getId() {
    return id;
  }


}
