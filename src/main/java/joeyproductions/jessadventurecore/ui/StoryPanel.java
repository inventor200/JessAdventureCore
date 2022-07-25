/*
 * The MIT License
 *
 * Copyright 2022 Joseph Cramsey.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package joeyproductions.jessadventurecore.ui;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.ListIterator;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * A panel for displaying the story output of the game.
 * @author Joseph Cramsey
 */
class StoryPanel extends JTextPane implements HabitualRefresher {
    
    private static final String INDENT = "&nbsp;&nbsp;&nbsp;";
    
    private final HTMLEditorKit htmlEditorKit;
    private final ArrayList<LineContent> lines;
    private boolean needsRefresh = false;
    boolean somethingWasPosted = false;
    
    private final JLayeredPane layeredParent;
    
    private StoryPanel(String firstMessage, JLayeredPane layeredParent) {
        super();
        this.layeredParent = layeredParent;
        htmlEditorKit = new HTMLEditorKit();
        lines = new ArrayList<>();
        lines.add(new LineContent(firstMessage, false));
    }
    
    static ScrollPair<StoryPanel> createStoryPanel(String firstMessage, JLayeredPane layeredParent) {
        StoryPanel storyPanel = new StoryPanel(firstMessage, layeredParent);
        storyPanel.setEditable(false);
        storyPanel.setContentType("text/html");
        storyPanel.setOpaque(true);
        
        storyPanel.updateStyle();
        storyPanel.setEditorKit(storyPanel.htmlEditorKit);
        
        JPanel storyFramer = new JPanel();
        storyFramer.setLayout(new BoxLayout(storyFramer, BoxLayout.X_AXIS));
        storyFramer.add(Box.createHorizontalGlue());
        storyFramer.add(storyPanel);
        
        JScrollPane scroll = new JScrollPane(storyFramer) {
            /*@Override
            public Dimension getMinimumSize() {
                int min = storyPanel.getMinimumWidth();
                return new Dimension(min, min);
            }
            
            @Override
            public Dimension getPreferredSize() {
                int pref = storyPanel.getPreferredWidth();
                return new Dimension(pref, pref);
            }*/
            
            @Override
            public Rectangle getBounds(Rectangle temp) {
                temp.x = 0;
                temp.y = 0;
                temp.width = storyPanel.layeredParent.getWidth();
                temp.height = storyPanel.layeredParent.getHeight();
                return temp;
            }
            
            @Override
            public Rectangle getBounds() {
                return getBounds(new Rectangle());
            }
        };
        
        storyPanel.needsRefresh = true;
        
        SwingUtilities.invokeLater(() -> {
            scroll.getVerticalScrollBar().setValue(0);
        });
        
        return new ScrollPair<>(storyPanel, scroll);
    }
    
    void updateStyle() {
        StyleSheet styleSheet = new StyleSheet();
        
        int backgroundValue = JessAdventureCore.DARK_MODE
                ? JessAdventureCore.DARK_BACKGROUND_VALUE
                : JessAdventureCore.LIGHT_BACKGROUND_VALUE;
        int paragraphValue = JessAdventureCore.DARK_MODE
                ? JessAdventureCore.DARK_PARAGRAPH_VALUE
                : JessAdventureCore.LIGHT_PARAGRAPH_VALUE;
        int headerValue = JessAdventureCore.DARK_MODE
                ? JessAdventureCore.DARK_HEADER_VALUE
                : JessAdventureCore.LIGHT_HEADER_VALUE;
        
        styleSheet.addRule("body { background-color: "
                + String.format("#%02x%02x%02x",
                        backgroundValue, backgroundValue, backgroundValue)
                + "; padding: "
                + Integer.toString(JessAdventureCore.STORY_BODY_PADDING)
                + "px; }");
        styleSheet.addRule("p, li { color: "
                + String.format("#%02x%02x%02x",
                        paragraphValue, paragraphValue, paragraphValue)
                + "; font-size: "
                + Integer.toString(JessAdventureCore.STORY_FONT_SIZE)
                + "pt; font-family: sans-serif; }");
        styleSheet.addRule("p { margin: "
                + Integer.toString(JessAdventureCore.STORY_LINE_SPACING / 2)
                + "px 0px "
                + Integer.toString(
                        (JessAdventureCore.STORY_LINE_SPACING / 2)
                        + (JessAdventureCore.STORY_LINE_SPACING % 2)
                )
                + "px 16px; }");
        styleSheet.addRule("ul, ol { margin: 0px 0px 0px 48px; }");
        styleSheet.addRule("h1, h2 { color: "
                + String.format("#%02x%02x%02x",
                        headerValue, headerValue, headerValue)
                + "; font-family: monospace; }");
        styleSheet.addRule("h1 { font-size: "
                + Integer.toString(JessAdventureCore.STORY_H1_SIZE)
                + "pt; }");
        styleSheet.addRule("h2 { font-size: "
                + Integer.toString(JessAdventureCore.STORY_H2_SIZE)
                + "pt; margin: 16px 0px 0px 12px; }");
        
        htmlEditorKit.setStyleSheet(styleSheet);
        HTMLDocument htmlDocument = (HTMLDocument)htmlEditorKit.createDefaultDocument();
        setDocument(htmlDocument);
        updateHTML();
    }
    
    void updateHTML() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<html><body>");
        
        ListIterator<LineContent> iter = lines.listIterator();
        while (iter.hasNext()) {
            LineContent line = iter.next();
            if (line.isParagraph) {
                sb.append("<p>" + INDENT).append(line.content).append("</p>");
            }
            else {
                sb.append(line.content);
            }
        }
        
        sb.append("</body></html>");
        
        setText(sb.toString());
        somethingWasPosted = true;
    }
    
    @Override
    public boolean needsRefresh() {
        return needsRefresh;
    }
    
    @Override
    public void handleRefresh() {
        needsRefresh = false;
        updateHTML();
    }
    
    private LineContent getLastContent(boolean mustBeParagraph) {
        if (!lines.isEmpty()) {
            LineContent lastLine = lines.get(lines.size() - 1);
            if (lastLine.isParagraph == mustBeParagraph) return lastLine;
        }
        
        // If the last line's type doesn't match the requirement,
        // then add a new line which does
        LineContent newContent = new LineContent("", mustBeParagraph);
        lines.add(newContent);
        return newContent;
    }
    
    private void startPause() {
        // This method is mostly here to maintain consistency between the
        // starts and ends of refresh pauses.
        RefreshThread.startPause(this);
    }
    
    private void endPause() {
        needsRefresh = true;
        RefreshThread.endPause(this);
    }
    
    void append(String content) {
        if (content.isBlank()) return; // Do not indirectly append blank content
        
        startPause();
        
        getLastContent(true).append(content, false);
        
        endPause();
    }
    
    void appendDirectly(String content) {
        if (content.isEmpty()) return; // Do not append empty content
        
        startPause();
        
        getLastContent(true).append(content, true);
        
        endPause();
    }
    
    void appendParagraph(String content) {
        startPause();
        
        lines.add(new LineContent(content, true));
        
        endPause();
    }
    
    void appendOther(String content) {
        if (content.isEmpty()) return; // Do not append empty content
        
        startPause();
        
        getLastContent(false).append(content, true);
        
        endPause();
    }
    
    void clearScreen() {
        startPause();
        
        lines.clear();
        
        endPause();
    }
    
    private int getMinimumWidth() {
        return Math.round((float)Toolkit.getDefaultToolkit()
                .getScreenSize().height * (2f / 5f));
    }
    
    private int getPreferredWidth() {
        int screenRatio = Math.round((float)Toolkit.getDefaultToolkit()
                .getScreenSize().height * (3f / 5f));
        return Math.min(layeredParent.getWidth(), screenRatio);
    }
    
    @Override
    public Dimension getMinimumSize() {
        int min = getMinimumWidth();
        return new Dimension(min, min);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getPreferredWidth(), super.getPreferredSize().height);
    }
    
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(getPreferredWidth(), super.getMaximumSize().height);
    }
}
