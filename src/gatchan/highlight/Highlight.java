package gatchan.highlight;

import gnu.regexp.REException;
import org.gjt.sp.jedit.search.RESearchMatcher;
import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.Buffer;

import java.awt.*;
import java.util.ArrayList;

/**
 * A Highlight defines the string to highlight.
 *
 * @author Matthieu Casanova
 * @version $Id$
 */
public final class Highlight {
  /** This scope will be saved when you exit jEdit. */
  public static final int PERMANENT_SCOPE = 0;
  /** This scope is global but will not be saved. */
  public static final int SESSION_SCOPE = 1;
  /** This scope will not be saved and is for one buffer only. */
  public static final int BUFFER_SCOPE = 2;

  private String stringToHighlight;

  private boolean regexp;

  private boolean ignoreCase = true;

  private SearchMatcher searchMatcher;

  private static final int HIGHLIGHT_VERSION = 1;

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

  private int scope = PERMANENT_SCOPE;

  private Buffer buffer;

  private long duration = Long.MAX_VALUE;
  private long lastSeen = System.currentTimeMillis();

  public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase, int scope) throws REException {
    this.scope = scope;
    init(stringToHighlight, regexp, ignoreCase, getNextColor());
  }

  public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase) throws REException {
    this(stringToHighlight, regexp, ignoreCase, PERMANENT_SCOPE);
  }

  public Highlight(String s) throws REException {
    this(s, false, true, PERMANENT_SCOPE);
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
      Highlight highlight = (Highlight) obj;
      return highlight.getStringToHighlight().equals(stringToHighlight) &&
             highlight.isRegexp() == regexp &&
             highlight.isIgnoreCase() == ignoreCase;
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

  /**
   * Serialize the highlight like that : {@link HIGHLIGHT_VERSION};regexp ignorecase color;stringToHighlight (no space
   * between regexp, ignorecase and color
   *
   * @return
   */
  public String serialize() {
    StringBuffer buff = new StringBuffer(stringToHighlight.length() + 20);
    buff.append(HIGHLIGHT_VERSION).append(';');
    serializeBoolean(buff, regexp);
    serializeBoolean(buff, ignoreCase);
    buff.append(color.getRGB());
    buff.append(';');
    buff.append(stringToHighlight);
    return buff.toString();
  }

  private static void serializeBoolean(StringBuffer buff, boolean bool) {
    if (bool) {
      buff.append(1);
    } else {
      buff.append(0);
    }
  }

  /**
   * Unserialize the highlight.
   *
   * @param s the string to parse.
   *
   * @return the highlight unserialized
   *
   * @throws InvalidHighlightException exception if the highlight is invalid
   */
  public static Highlight unserialize(String s) throws InvalidHighlightException {
    int index = s.indexOf(';');
    boolean regexp = s.charAt(index + 1) == '1';
    boolean ignoreCase = s.charAt(index + 2) == '1';
    int i = s.indexOf(';', index + 3);
    Color color = Color.decode(s.substring(index + 3, i));
    // When using String.substring() the new String uses the same char[] so the new String is as big as the first one.
    // This is minor optimization
    String searchString = new String(s.substring(i + 1));
    Highlight highlight = new Highlight();
    try {
      highlight.init(searchString, regexp, ignoreCase, color);
    } catch (REException e) {
      throw new InvalidHighlightException("Invalid regexp " + e.getMessage());
    }
    return highlight;
  }

  /**
   * Get the scope of the highlight.
   *
   * @return the scope of the highlight
   */
  public int getScope() {
    return scope;
  }

  /**
   * Set the scope of the highlight.
   *
   * @param scope the new scope
   */
  public void setScope(int scope) {
    this.scope = scope;
  }

  /**
   * Returns the buffer associated to this highlight. It will be null if the scope is not {@link #BUFFER_SCOPE}
   *
   * @return the buffer associated
   */
  public Buffer getBuffer() {
    return buffer;
  }

  /**
   * Associate the highlight to a buffer. It must only be used for {@link #BUFFER_SCOPE}
   *
   * @param buffer the buffer
   */
  public void setBuffer(Buffer buffer) {
    if (this.buffer != null) {
      java.util.List highlights = (java.util.List) this.buffer.getProperty("highlights");
      highlights.remove(this);
      if (highlights.isEmpty()) {
        this.buffer.unsetProperty("highlights");
      }
    }
    this.buffer = buffer;
    if (buffer != null) {
      java.util.List highlights = (java.util.List) buffer.getProperty("highlights");
      if (highlights == null) {
        highlights = new ArrayList();
        buffer.setProperty("highlights", highlights);
      }
      highlights.add(this);
    }
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void updateLastSeen() {
    this.lastSeen = System.currentTimeMillis();
  }

  public boolean isExpired() {
    return System.currentTimeMillis() - lastSeen > duration;
  }
}
