package gatchan.highlight;

import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.search.RESearchMatcher;
import gnu.regexp.REException;

import java.awt.*;

/**
 * A Highlight defines the string to highlight.
 *
 * @author Matthieu Casanova
 */
public final class Highlight {

  private String stringToHighlight;

  private boolean regexp;

  private boolean ignoreCase;

  private SearchMatcher searchMatcher;

  public static final Color DEFAULT_COLOR = new Color(153, 255, 204);

  private Color color;

  private boolean enabled = true;

  public Highlight(String s) throws REException {
    init(s, false, DEFAULT_COLOR);
  }

  public Highlight() throws REException {
    this(null);
  }

  public void init(String s, boolean regexp, Color color) throws REException {
    if (regexp) {
      if (!s.equals(stringToHighlight) || !this.regexp) {
        searchMatcher = new RESearchMatcher(s, false);
      }
    } else {
      searchMatcher = null;
    }
    stringToHighlight = s;
    this.regexp = regexp;
    this.color = color;
  }

  public String getStringToHighlight() {
    return stringToHighlight;
  }

  public boolean isRegexp() {
    return regexp;
  }

  public boolean isIgnoreCase() {
    return ignoreCase;
  }

  public SearchMatcher getSearchMatcher() {
    return searchMatcher;
  }

  /**
   * Returns the color of the highlight.
   *
   * @return the color
   */
  public Color getColor() {
    return color;
  }

  public boolean equals(Object obj) {
    if (obj instanceof Highlight) {
      final Highlight highlight = (Highlight) obj;
      return highlight.getStringToHighlight().equals(stringToHighlight) && highlight.isRegexp() == regexp;
    }
    return false;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
