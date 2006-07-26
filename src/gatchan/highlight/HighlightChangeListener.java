package gatchan.highlight;

import java.util.EventListener;


/**
 * @author Matthieu Casanova
 */
public interface HighlightChangeListener extends EventListener {

  void highlightUpdated(boolean highlightEnabled);
}
