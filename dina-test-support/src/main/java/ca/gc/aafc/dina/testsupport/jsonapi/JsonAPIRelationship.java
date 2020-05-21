package ca.gc.aafc.dina.testsupport.jsonapi;

/**
 * Immutable class (for testing) representing a Relationship.
 *
 */
public final class JsonAPIRelationship {

  private final String name;
  private final String type;
  private final String id;
  
  public static JsonAPIRelationship of(String name, String type, String id) {
    return new JsonAPIRelationship(name, type, id);
  }
  
  private JsonAPIRelationship(String name, String type, String id) {
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
