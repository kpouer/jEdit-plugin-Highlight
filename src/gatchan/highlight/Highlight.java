package gatchan.highlight;

import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.search.RESearchMatcher;
import gnu.regexp.REException;

/**
 * A Highlight defines the string to highlight.
 * @author Matthieu Casanova
 */
public class Highlight {

  private String stringToHighlight;

  private boolean regexp;

  private boolean ignoreCase;

  private SearchMatcher searchMatcher;

  public Highlight(String s, boolean regexp) throws REException {
    if (regexp) {
      searchMatcher = new RESearchMatcher(s,false);
    }
    this.stringToHighlight = s;
    this.regexp = regexp;
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

  public boolean equals(Object obj) {
    if (obj instanceof Highlight) {
      Highlight highlight = (Highlight) obj;
      return highlight.getStringToHighlight().equals(stringToHighlight) && highlight.isRegexp() == regexp;
    }
    return false;
  }
}
