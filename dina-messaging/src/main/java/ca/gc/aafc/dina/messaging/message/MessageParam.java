package ca.gc.aafc.dina.messaging.message;

/**
 * A record representing a parameter within a message.
 *
 * @param type  the type of the message parameter ({@link MessageParamType#TEXT} or {@link MessageParamType#URL})
 * @param value the string value of the parameter
 */
public record MessageParam(MessageParamType type, String value) {
  public enum MessageParamType {TEXT, URL}
}
