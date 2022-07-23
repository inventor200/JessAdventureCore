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

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.DarculaTheme;
import com.github.weisj.darklaf.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * The core interface for the adventure.
 * This should always be created first, and there should only be one instance.
 * @author Joseph Cramsey
 */
public class JessAdventureCore implements ActionListener, HabitualRefresher {
    
    public static int STORY_FONT_SIZE = 14;
    public static int STORY_LINE_SPACING = 4;
    public static int STORY_BODY_PADDING = 8;
    
    private static final float INVENTORY_COLUMN_FRACTION = 0.4f;
    private static final float MAP_COLUMN_FRACTION = 0.2f;
    private static final float STORY_COLUMN_FRACTION = 1f
            - (INVENTORY_COLUMN_FRACTION + MAP_COLUMN_FRACTION);
    private static final int MINIMUM_WINDOW_HEIGHT = 400;
    
    public static JessAdventureCore CORE;
    public static RefreshThread REFRESH_THREAD;
    String name;
    String author;
    private JFrame frame;
    private JLabel roomLabel;
    private JPanel storyColumn;
    //private JButton takeActionButton;
    
    private final StoryPanelBuffer storyPanelBuffer;
    
    private JLayeredPane layers;
    
    private StoryPanel storyPanel;
    private JScrollPane storyPanelScroll;
    private boolean performedFirstScroll;
    private boolean somethingWasPosted;
    private boolean promptHasChanged;
    
    private PlayerPrompt playerPrompt;
    
    private JPanel mapColumn;
    //private JPanel actionPanel;
    //private JScrollPane actionScroll;
    
    private JessAdventureCore(String name, String author) {
        this.name = name;
        this.author = author;
        this.storyPanelBuffer = new StoryPanelBuffer();
        this.performedFirstScroll = false;
        this.somethingWasPosted = false;
        this.promptHasChanged = false;
    }
    
    public static void initAdventure(String name, String author) {
        System.out.println("Beginning adventure: " + name + " by " + author + "...");
        JessAdventureCore core = new JessAdventureCore(name, author);
        
        //Theme preferredTheme = LafManager.themeForPreferredStyle(getPreferredThemeStyle());
        Theme preferredTheme = new DarculaTheme();
        LafManager.setTheme(preferredTheme);
        LafManager.install();
        
        ArrayList<HabitualRefresher> refreshers = new ArrayList<>();
        REFRESH_THREAD = new RefreshThread(refreshers);
        
        core.frame = new JFrame(name);
        CORE = core;
        SwingUtilities.invokeLater(() -> {
            core.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            core.frame.setSize(1600, 960);
            core.frame.setResizable(true);
            
            core.layers = new JLayeredPane();
            core.layers.setLayout(new LayeredPaneLayout(core.layers));
            
            ScrollPair<StoryPanel> storyPanelPair = StoryPanel.createStoryPanel(
                    "<h1>" + name + "</h1><h2>by " + author + "</h2>",
                    core.layers
            );
            core.storyPanel = storyPanelPair.windowContent;
            core.storyPanelScroll = storyPanelPair.scrollPane;
            refreshers.add(core.storyPanel);
            refreshers.add(core);
            
            JPanel mainColumns = new JPanel(new BorderLayout());
            core.frame.setContentPane(mainColumns);
            
            core.storyColumn = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(
                            core.getFractionWidth(STORY_COLUMN_FRACTION),
                            MINIMUM_WINDOW_HEIGHT
                    );
                }
                
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(
                            core.getFractionWidth(STORY_COLUMN_FRACTION),
                            core.getHeight()
                    );
                }
                
                @Override
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            //JPanel storyFramer = new JPanel();
            //storyFramer.setLayout(new BoxLayout(storyFramer, BoxLayout.X_AXIS));
            //storyFramer.add(Box.createHorizontalGlue());
            //storyFramer.add(core.storyColumn);
            mainColumns.add(core.storyColumn, BorderLayout.CENTER);
            
            core.storyColumn.add(core.layers, BorderLayout.CENTER);
            /*storyPanelPair.scrollPane.setBounds(
                    0, 0, layers.getWidth(), layers.getHeight()
            );*/
            core.layers.add(storyPanelPair.scrollPane, JLayeredPane.DEFAULT_LAYER);
            core.playerPrompt = new PlayerPrompt(core.storyPanelBuffer, core, core.layers);
            /*core.playerPrompt.autocompleteSuggestionPanel.setBounds(
                    0, 0, layers.getWidth(), layers.getHeight()
            );*/
            core.layers.add(core.playerPrompt.autocompleteSuggestionPanel, JLayeredPane.POPUP_LAYER);
            
            core.roomLabel = new JLabel("Demo Room Name");
            core.roomLabel.setBorder(BorderFactory.createEmptyBorder(
                    8, 16, 4, 16
            ));
            core.roomLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
            core.storyColumn.add(core.roomLabel, BorderLayout.NORTH);
            core.storyColumn.add(core.playerPrompt.buttonList, BorderLayout.SOUTH);
            
            core.mapColumn = new JPanel() {
                @Override
                public Dimension getMinimumSize() {
                    return new Dimension(
                            core.getFractionWidth(MAP_COLUMN_FRACTION),
                            MINIMUM_WINDOW_HEIGHT
                    );
                }
                
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(
                            core.getFractionWidth(MAP_COLUMN_FRACTION),
                            core.getHeight()
                    );
                }
                
                @Override
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            core.mapColumn.setLayout(new BoxLayout(core.mapColumn, BoxLayout.Y_AXIS));
            mainColumns.add(core.mapColumn, BorderLayout.EAST);
            
            JPanel mapFramer = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getPreferredSize() {
                    int width = core.mapColumn.getWidth();
                    return new Dimension(width, width);
                }
                
                @Override
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            JPanel mapPlaceHolder = new JPanel() { //TODO
                @Override
                public Dimension getPreferredSize() {
                    int width = this.getWidth();
                    return new Dimension(width, width);
                }
                
                @Override
                public boolean isOpaque() {
                    return true;
                }
                
                @Override
                public Color getBackground() {
                    return Color.BLACK;
                }
            };
            mapFramer.add(mapPlaceHolder, BorderLayout.PAGE_START);
            mapFramer.setBorder(BorderFactory.createTitledBorder("Map"));
            core.mapColumn.add(mapFramer);
            
            /*core.actionPanel = new JPanel();
            core.actionPanel.setLayout(new BoxLayout(core.actionPanel, BoxLayout.Y_AXIS));
            JPanel buttonListResizer = new JPanel(new BorderLayout());
            JPanel buttonList = new JPanel(new GridLayout(0, 1));
            buttonListResizer.add(buttonList, BorderLayout.PAGE_START);
            core.actionPanel.add(buttonListResizer);
            core.actionPanel.add(Box.createVerticalGlue());
            core.actionScroll = new JScrollPane(core.actionPanel);
            core.mapColumn.add(core.actionScroll);
            core.actionScroll.setBorder(BorderFactory.createTitledBorder("Actions"));
            
            core.takeActionButton = new JButton(TAKE_ACTION_MSG);
            core.takeActionButton.addActionListener(core);
            buttonList.add(core.takeActionButton);*/
            
            core.frame.setVisible(true);
            core.frame.setLocationRelativeTo(null);
            
            System.out.println("Starting refresh thread...");
            REFRESH_THREAD.start();
            System.out.println("Batch refresh thread has started!");
            
            System.out.println("UI initialization done!");
            core.postUIInit();
        });
        
        System.out.println("Main init trigger has expired successfully!");
    }
    
    private void postUIInit() {
        System.out.println("Doing post-UI initialization...");
        
        //TODO: Set up the world state
        
        System.out.println("storyColumn: " + storyColumn.getBounds() + " | " + storyColumn.isShowing());
        System.out.println("layers: " + layers.getBounds() + " | " + layers.isShowing());
        System.out.println("storyPanelScroll: " + storyPanelScroll.getBounds() + " | " + storyPanelScroll.isShowing());
        System.out.println("storyPanel: " + storyPanel.getBounds() + " | " + storyPanel.isShowing());
        
        appendParagraph("Testing first paragraph.");
        appendParagraph("Testing second paragraph.");
        append("Testing extra content.");
        appendOther("<ul><li>Testing item one</li><li>Testing item two</li></ul>");
        append("Testing third paragraph.");
        pressToContinue();
        for (int i = 0; i < 20; i++) {
            append("<br>New line " + i + "!");
        }
        pressToContinue();
        clearScreen();
        append("The screen has cleared.");
        
        System.out.println("Adventure has begun!");
        
        attemptToWriteStory();
    }
    
    public int getWidth() {
        return frame.getWidth();
    }
    
    public int getHeight() {
        return frame.getHeight();
    }
    
    public int getFractionWidth(float fraction) {
        if (fraction > 1f) fraction = 1f;
        if (fraction < 0) fraction = 0;
        return Math.round(getWidth() * fraction);
    }
    
    void updateStyle() {
        //TODO: Font sizes should change according to screen resolution
        //TODO: Also account for look and feel theme
        storyPanel.updateStyle();
    }
    
    void attemptToWriteStory() {
        while (storyPanelBuffer.pauseReason == null && !storyPanelBuffer.buffer.isEmpty()) {
            StoryPanelInstruction instr = storyPanelBuffer.buffer.poll();
            boolean thisWasAPost = true;
            
            if (instr instanceof PostLineInstruction) {
                PostLineInstruction post = (PostLineInstruction)instr;
                switch (post.method) {
                    case PostLineInstruction.METHOD_APPEND:
                        storyPanel.append(post.content);
                        break;
                    case PostLineInstruction.METHOD_APPEND_DIRECTLY:
                        storyPanel.appendDirectly(post.content);
                        break;
                    case PostLineInstruction.METHOD_APPEND_PARAGRAPH:
                        storyPanel.appendParagraph(post.content);
                        break;
                    case PostLineInstruction.METHOD_APPEND_OTHER:
                        storyPanel.appendOther(post.content);
                        break;
                    default:
                        throw new RuntimeException("Unrecognized post line method ID: " + post.method);
                }
            }
            else if (instr instanceof ClearScreenInstruction) {
                storyPanel.clearScreen();
            }
            else if (instr instanceof PressToContinueInstruction) {
                storyPanelBuffer.pauseReason = instr;
                promptHasChanged = true;
            }
            else {
                thisWasAPost = false;
            }
            
            somethingWasPosted |= thisWasAPost;
        }
    }
    
    private boolean storyNeedsScrolling() {
        return somethingWasPosted && storyPanel.somethingWasPosted;
    }
    
    @Override
    public boolean needsRefresh() {
        return storyNeedsScrolling() || promptHasChanged;
    }
    
    @Override
    public void handleRefresh() {
        if (promptHasChanged) {
            promptHasChanged = false;
            SwingUtilities.invokeLater(() -> {
                playerPrompt.rearrange();
            });
        }
        if (storyNeedsScrolling()) {
            somethingWasPosted = false;
            storyPanel.somethingWasPosted = false;
            SwingUtilities.invokeLater(() -> {
                storyColumn.invalidate();
                storyColumn.validate();
                if (performedFirstScroll) {
                    JScrollBar vertical = storyPanelScroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                else {
                    performedFirstScroll = true;
                }
            });
        }
    }
    
    public void append(String content) {
        storyPanelBuffer.buffer.add(new PostLineInstruction(content, PostLineInstruction.METHOD_APPEND));
    }
    
    public void appendDirectly(String content) {
        storyPanelBuffer.buffer.add(new PostLineInstruction(content, PostLineInstruction.METHOD_APPEND_DIRECTLY));
    }
    
    public void appendParagraph(String content) {
        storyPanelBuffer.buffer.add(new PostLineInstruction(content, PostLineInstruction.METHOD_APPEND_PARAGRAPH));
    }
    
    public void appendOther(String content) {
        storyPanelBuffer.buffer.add(new PostLineInstruction(content, PostLineInstruction.METHOD_APPEND_OTHER));
    }
    
    public void clearScreen() {
        storyPanelBuffer.buffer.add(new ClearScreenInstruction());
    }
    
    public void pressToContinue() {
        storyPanelBuffer.buffer.add(new PressToContinueInstruction());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == playerPrompt.pressToContinueButton) {
            if (storyPanelBuffer.pauseReason != null) {
                storyPanelBuffer.pauseReason = null;
                promptHasChanged = true;
                attemptToWriteStory();
            }
        }
    }
}
