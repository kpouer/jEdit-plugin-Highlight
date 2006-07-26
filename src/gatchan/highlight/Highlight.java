package gatchan.highlight;

import org.gjt.sp.jedit.search.SearchMatcher;
import org.gjt.sp.jedit.search.PatternSearchMatcher;
import org.gjt.sp.jedit.search.BoyerMooreSearchMatcher;
import org.gjt.sp.jedit.buffer.JEditBuffer;
import org.gjt.sp.jedit.jEdit;

import java.awt.*;
import java.util.ArrayList;

/**
 * A Highlight defines the string to highlight.
 *
 * @author Matthieu Casanova
 * @version $Id: Highlight.java,v 1.23 2006/06/21 09:40:32 kpouer Exp $
 */
public class Highlight
{
    /**
     * This scope will be saved when you exit jEdit.
     */
    public static final int PERMANENT_SCOPE = 0;
    /**
     * This scope is global but will not be saved.
     */
    public static final int SESSION_SCOPE = 1;
    /**
     * This scope will not be saved and is for one buffer only.
     */
    public static final int BUFFER_SCOPE = 2;

    private String stringToHighlight;

    private boolean regexp;

    private boolean ignoreCase = true;

    private boolean valid;

    private SearchMatcher searchMatcher;

    private static final int HIGHLIGHT_VERSION = 1;

    private boolean highlightSubsequence = jEdit.getBooleanProperty(HighlightOptionPane.PROP_HIGHLIGHT_SUBSEQUENCE);

    /**
     * The default color. If null we will cycle the colors.
     */
    private static Color defaultColor = jEdit.getColorProperty(HighlightOptionPane.PROP_DEFAULT_COLOR);

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

    private JEditBuffer buffer;

    /**
     * The time to live in ms.
     */
    private long duration = Long.MAX_VALUE;

    /**
     * The date where the highlight was last seen.
     */
    private long lastSeen = System.currentTimeMillis();

    public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase, int scope)
    {
        this.scope = scope;
        init(stringToHighlight, regexp, ignoreCase, getNextColor());
    }

    public Highlight(String stringToHighlight, boolean regexp, boolean ignoreCase)
    {
        this(stringToHighlight, regexp, ignoreCase, PERMANENT_SCOPE);
    }

    public Highlight(String s)
    {
        this(s, false, true, PERMANENT_SCOPE);
    }

    public Highlight()
    {
    }

    public void init(String s, boolean regexp, boolean ignoreCase, Color color)
    {
        if (s.length() == 0)
        {
            valid = false;
            throw new IllegalArgumentException("The search string cannot be empty");
        }
        valid = true;
        if (regexp)
        {
            if (searchMatcher == null ||
                !s.equals(stringToHighlight) ||
                !this.regexp ||
                ignoreCase != this.ignoreCase)
            {
                searchMatcher = new PatternSearchMatcher(s, ignoreCase);
            }
        }
        else if (searchMatcher == null ||
                 !s.equals(stringToHighlight) ||
                 this.regexp ||
                 ignoreCase != this.ignoreCase)
        {
            searchMatcher = new BoyerMooreSearchMatcher(s, ignoreCase);
        }
        stringToHighlight = s;
        this.regexp = regexp;
        this.ignoreCase = ignoreCase;
        this.color = color;
    }

    public String getStringToHighlight()
    {
        return stringToHighlight;
    }

    public boolean isRegexp()
    {
        return regexp;
    }

    public boolean isIgnoreCase()
    {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase)
    {
        this.ignoreCase = ignoreCase;
    }


    public boolean isValid()
    {
        return valid;
    }

    public void setValid(boolean valid)
    {
        this.valid = valid;
    }

    public SearchMatcher getSearchMatcher()
    {
        return searchMatcher;
    }

    /**
     * Returns the color of the highlight.
     *
     * @return the color
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Set the color of the highlight.
     *
     * @param color the new color
     */
    public void setColor(Color color)
    {
        this.color = color;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof Highlight)
        {
            Highlight highlight = (Highlight) obj;
            return highlight.stringToHighlight.equals(stringToHighlight) &&
                   highlight.regexp == regexp &&
                   highlight.ignoreCase == ignoreCase;
        }
        return false;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Set the default color.
     *
     * @param defaultColor the new default color. If null we will cycle the colors
     */
    public static void setDefaultColor(Color defaultColor)
    {
        Highlight.defaultColor = defaultColor;
    }

    public static Color getNextColor()
    {
        if (defaultColor != null)
        {
            return defaultColor;
        }
        colorIndex = ++colorIndex % COLORS.length;
        return COLORS[colorIndex];
    }

    /**
     * Serialize the highlight like that : {@link #HIGHLIGHT_VERSION};regexp ignorecase color;stringToHighlight (no space
     * between regexp, ignorecase and color
     *
     * @return the serialized string
     */
    public String serialize()
    {
        StringBuffer buff = new StringBuffer(stringToHighlight.length() + 20);
        buff.append(HIGHLIGHT_VERSION).append(';');
        serializeBoolean(buff, regexp);
        serializeBoolean(buff, ignoreCase);
        serializeBoolean(buff, enabled);
        buff.append(color.getRGB());
        buff.append(';');
        buff.append(stringToHighlight);
        return buff.toString();
    }

    private static void serializeBoolean(StringBuffer buff, boolean bool)
    {
        if (bool)
        {
            buff.append(1);
        }
        else
        {
            buff.append(0);
        }
    }

    /**
     * Unserialize the highlight.
     *
     * @param s the string to parse.
     * @return the highlight unserialized
     * @throws InvalidHighlightException exception if the highlight is invalid
     */
    public static Highlight unserialize(String s, boolean getStatus) throws InvalidHighlightException
    {
        int index = s.indexOf(';');
        boolean regexp = s.charAt(index + 1) == '1';
        boolean ignoreCase = s.charAt(index + 2) == '1';
        boolean enabled = !getStatus || s.charAt((index + 3)) == '1';
        int i = s.indexOf(';', index + 4);
        Color color = Color.decode(s.substring(index + 4, i));

        // When using String.substring() the new String uses the same char[] so the new String is as big as the first one.
        // This is minor optimization
        String searchString = new String(s.substring(i + 1));
        Highlight highlight = new Highlight();
        highlight.setEnabled(enabled);
        highlight.init(searchString, regexp, ignoreCase, color);
        return highlight;
    }

    /**
     * Get the scope of the highlight.
     *
     * @return the scope of the highlight
     */
    public int getScope()
    {
        return scope;
    }

    /**
     * Set the scope of the highlight.
     *
     * @param scope the new scope
     */
    public void setScope(int scope)
    {
        this.scope = scope;
    }

    /**
     * Returns the buffer associated to this highlight. It will be null if the scope is not {@link #BUFFER_SCOPE}
     *
     * @return the buffer associated
     */
    public JEditBuffer getBuffer()
    {
        return buffer;
    }

    /**
     * Associate the highlight to a buffer. It must only be used for {@link #BUFFER_SCOPE}
     *
     * @param buffer the buffer
     */
    public void setBuffer(JEditBuffer buffer)
    {
        if (this.buffer != null)
        {
            java.util.List highlights = (java.util.List) this.buffer.getProperty("highlights");
            highlights.remove(this);
            if (highlights.isEmpty())
            {
                this.buffer.unsetProperty("highlights");
            }
        }
        this.buffer = buffer;
        if (buffer != null)
        {
            java.util.List highlights = (java.util.List) buffer.getProperty("highlights");
            if (highlights == null)
            {
                highlights = new ArrayList();
                buffer.setProperty("highlights", highlights);
            }
            highlights.add(this);
        }
    }

    /**
     * Set the time to live of the highlight.
     * This time to live is the time until the highlight expires if it wasn't be seen
     *
     * @param duration the duration in ms.
     */
    public void setDuration(long duration)
    {
        this.duration = duration;
    }

    /**
     * This method is called each time the highlight is seen.
     */
    public void updateLastSeen()
    {
        lastSeen = System.currentTimeMillis();
    }

    /**
     * Check if the highlight is expired.
     * The highlight is expired if it hasn't been seen for more that {@link #duration} field
     *
     * @return true if the highlight is expired
     */
    public boolean isExpired()
    {
        return System.currentTimeMillis() - lastSeen > duration;
    }

    public void setStringToHighlight(String stringToHighlight)
    {
        init(stringToHighlight, regexp, ignoreCase, color);
    }

    public boolean isHighlightSubsequence()
    {
        return highlightSubsequence;
    }

    /**
     * Activate or deactivate the feature "highlight subsequence"
     * Highlight subsequences (if checked, the same highlight can be found several times inside the same word)
     *
     * @param highlightSubsequence true to activate the feature, false otherwise
     * @return <code>true</code> if the value was changed
     */
    public boolean setHighlightSubsequence(boolean highlightSubsequence)
    {
        if (this.highlightSubsequence != highlightSubsequence)
        {
            this.highlightSubsequence = highlightSubsequence;
            return true;
        }
        return false;
    }
}
