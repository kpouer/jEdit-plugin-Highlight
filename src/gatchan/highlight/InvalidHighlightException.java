package gatchan.highlight;

/**
 * Exception launched when creating an highlight if there is an error.
 *
 * @author Matthieu Casanova
 */
public class InvalidHighlightException extends Exception {

  public InvalidHighlightException(String message) {
    super(message);
  }
}
