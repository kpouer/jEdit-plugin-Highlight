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

  private boolean ignoreCase = true;

  private SearchMatcher searchMatcher;

  private static final Color[] COLORS = {new Color(153, 255, 204),
                                         new Color(0x66, 0x66, 0xff),
                                         new Color(0xff, 0x66, 0x66),
                                         new Color(0xff, 0xcc, 0x66),
                                         new Color(0xcc, 0xff, 0x66),
                                         new Color(0xff, 0x33, 0x99),
                                         new Color(0xff, 0x33, 0x00),
                                         new Color(0x66, 0xff, 0x00),
                                         new Color(0x99, 0x00, 0x99),
                                         new Color(0x99, 0x99, 0x00),
                                         new Color(0x00, 0x99, 0x66)};

  private static int colorIndex;

  private Color color;

  private boolean enabled = true;

  public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase) throws REException {
    init(stringToHighlight, regexp, ignoreCase, getNextColor());
  }

  public Highlight(String s) throws REException {
    this(s,false,true);
  }

  public Highlight() {
  }

  public void init(String s, boolean regexp, boolean ignoreCase, Color color) throws REException {
    if (s.length() == 0) {
      throw new IllegalArgumentException("The search string cannot be empty");
    }
    if (regexp) {
      if (!s.equals(stringToHighlight) || !this.regexp || ignoreCase != this.ignoreCase) {
        searchMatcher = new RESearchMatcher(s, ignoreCase);
      }
    } else {
      searchMatcher = null;
    }
    stringToHighlight = s;
    this.regexp = regexp;
    this.ignoreCase = ignoreCase;
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
      return highlight.getStringToHighlight().equals(stringToHighlight) && highlight.isRegexp() == regexp && highlight.isIgnoreCase() == ignoreCase;
    }
    return false;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public static Color getNextColor() {
    colorIndex = ++colorIndex % COLORS.length;
    return COLORS[colorIndex];
  }
}
