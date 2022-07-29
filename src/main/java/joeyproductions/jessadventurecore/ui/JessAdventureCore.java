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
import com.github.weisj.darklaf.theme.IntelliJTheme;
import com.github.weisj.darklaf.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import joeyproductions.jessadventurecore.world.World;

/**
 * The core interface for the adventure.
 * This should always be created first, and there should only be one instance.
 * @author Joseph Cramsey
 */
public class JessAdventureCore implements ActionListener, HabitualRefresher {
    
    //TODO: Screen reader mode, which has a specialized layout and controls
    //      for optimal screen reader presentation
    
    public static float FONT_SIZE_MULTIPLIER = 1f;
    public static final int FULL_SIZE_FONT_SIZE = 14;
    public static int STORY_FONT_SIZE = FULL_SIZE_FONT_SIZE;
    public static final int FULL_SIZE_H1_SIZE = 32;
    public static int STORY_H1_SIZE = FULL_SIZE_H1_SIZE;
    public static final int FULL_SIZE_H2_SIZE = 24;
    public static int STORY_H2_SIZE = FULL_SIZE_H2_SIZE;
    public static final int FULL_SIZE_STORY_LINE_SPACING = 4;
    public static int STORY_LINE_SPACING = FULL_SIZE_STORY_LINE_SPACING;
    public static final int FULL_SIZE_STORY_BODY_PADDING = 8;
    public static int STORY_BODY_PADDING = FULL_SIZE_STORY_BODY_PADDING;
    public static final int LIGHT_BACKGROUND_VALUE = 248;
    public static final int LIGHT_PARAGRAPH_VALUE = 0;
    public static final int LIGHT_HEADER_VALUE = 96;
    public static final int DARK_BACKGROUND_VALUE = 32;
    public static final int DARK_PARAGRAPH_VALUE = 204;
    public static final int DARK_HEADER_VALUE = 255;
    public static boolean DARK_MODE = true;
    public static int MAX_SUGGESTION_COUNT = 3;
    
    private static final String[] FONT_IDS = new String[] {
        "Button.font",
        "ToggleButton.font",
        "RadioButton.font",
        "CheckBox.font",
        "ColorChooser.font",
        "ComboBox.font",
        "Label.font",
        "List.font",
        "MenuBar.font",
        "MenuItem.font",
        "RadioButtonMenuItem.font",
        "CheckBoxMenuItem.font",
        "Menu.font",
        "PopupMenu.font",
        "OptionPane.font",
        "Panel.font",
        "ProgressBar.font",
        "ScrollPane.font",
        "Viewport.font",
        "TabbedPane.font",
        "Table.font",
        "TableHeader.font",
        "TextField.font",
        "PasswordField.font",
        "TextArea.font",
        "TextPane.font",
        "EditorPane.font",
        "TitledBorder.font",
        "ToolBar.font",
        "ToolTip.font",
        "Tree.font"
    };
    
    private static final float INVENTORY_COLUMN_FRACTION = 0.4f;
    private static final float MAP_COLUMN_FRACTION = 0.2f;
    private static final float STORY_COLUMN_FRACTION = 1f
            - (INVENTORY_COLUMN_FRACTION + MAP_COLUMN_FRACTION);
    private static final int MINIMUM_WINDOW_HEIGHT = 400;
    
    public static JessAdventureCore CORE;
    public static RefreshThread REFRESH_THREAD;
    String name;
    String author;
    public final World world;
    private JFrame frame;
    private JLabel roomLabel;
    private JPanel storyColumn;
    
    private JCheckBoxMenuItem darkModeItem;
    
    private final StoryPanelBuffer storyPanelBuffer;
    
    private JLayeredPane layers;
    
    private StoryPanel storyPanel;
    private JScrollPane storyPanelScroll;
    private boolean performedFirstScroll;
    private boolean somethingWasPosted;
    private boolean promptHasChanged;
    
    private PlayerPrompt playerPrompt;
    
    private JPanel mapColumn;
    
    private JessAdventureCore(String name, String author, World world) {
        this.name = name;
        this.author = author;
        this.world = world;
        this.storyPanelBuffer = new StoryPanelBuffer();
        this.performedFirstScroll = false;
        this.somethingWasPosted = false;
        this.promptHasChanged = false;
    }
    
    public static void initAdventure(String name, String author, World world) {
        System.out.println("Beginning adventure: " + name + " by " + author + "...");
        JessAdventureCore core = new JessAdventureCore(name, author, world);
        
        ArrayList<HabitualRefresher> refreshers = new ArrayList<>();
        REFRESH_THREAD = new RefreshThread(refreshers);
        
        core.frame = new JFrame(name);
        CORE = core;
        SwingUtilities.invokeLater(() -> {
            core.updateLookAndFeel();
            core.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            core.frame.setSize(screenSize.width - 128, screenSize.height - 256);
            core.frame.setResizable(true);
            
            core.layers = new JLayeredPane();
            core.layers.setLayout(new LayeredPaneLayout(core.layers));
            
            ScrollPair<StoryPanel> storyPanelPair = StoryPanel.createStoryPanel(
                    "<h1>" + name + "</h1><h2>by " + author + "</h2>",
                    core.layers
            );
            
            core.storyPanel = storyPanelPair.windowContent;
            core.storyPanelScroll = storyPanelPair.scrollPane;
            core.playerPrompt = new PlayerPrompt(core.storyPanelBuffer, core, core.layers);
            
            refreshers.add(core.storyPanel);
            refreshers.add(core.playerPrompt);
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
            mainColumns.add(core.storyColumn, BorderLayout.CENTER);
            
            core.storyColumn.add(core.layers, BorderLayout.CENTER); //TODO: Notes panel goes on the left side
            core.layers.add(storyPanelPair.scrollPane, JLayeredPane.DEFAULT_LAYER);
            core.layers.add(core.playerPrompt.autocompleteSuggestionPanel, JLayeredPane.POPUP_LAYER);
            
            JPanel roomLabelPanel = new JPanel();
            roomLabelPanel.setLayout(new BoxLayout(roomLabelPanel, BoxLayout.X_AXIS));
            roomLabelPanel.add(Box.createHorizontalGlue());
            
            core.roomLabel = new JLabel("Demo Room Name");
            core.roomLabel.setBorder(BorderFactory.createEmptyBorder(
                    8, 16, 4, 16
            ));
            core.roomLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
            roomLabelPanel.add(core.roomLabel);
            core.storyColumn.add(roomLabelPanel, BorderLayout.NORTH);
            
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
            
            JMenuBar menuBar = new JMenuBar();
            core.frame.setJMenuBar(menuBar);
            
            JMenu viewMenu = new JMenu("View");
            menuBar.add(viewMenu);
            
            core.darkModeItem = new JCheckBoxMenuItem("Dark mode", DARK_MODE);
            viewMenu.add(core.darkModeItem);
            core.darkModeItem.addActionListener(core);
            
            core.frame.setVisible(true);
            core.frame.setLocationRelativeTo(null);
            
            System.out.println("Starting refresh thread...");
            REFRESH_THREAD.initialize();
            REFRESH_THREAD.start();
            System.out.println("Batch refresh thread has started!");
            
            core.playerPrompt.textField.requestFocus();
            
            System.out.println("UI initialization done!");
            core.postUIInit();
        });
        
        System.out.println("Main init trigger has expired successfully!");
    }
    
    private void postUIInit() {
        System.out.println("Doing post-UI initialization...");
        
        if (world == null) {
            throw new RuntimeException("World cannot be null!");
        }
        world.prestartWorld();
        
        appendParagraph("Testing first paragraph.");
        appendParagraph("Testing second paragraph.");
        append("Testing extra content.");
        appendOther("<ul><li>Testing item one</li><li>Testing item two</li></ul>");
        append("Testing third paragraph.");
        /*pressToContinue();
        for (int i = 0; i < 20; i++) {
            append("<br>New line " + i + "!");
        }
        pressToContinue();
        clearScreen();
        append("The screen has cleared.");*/
        
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
    
    void updateLookAndFeel() {
        //TODO: Save preferred theme
        Theme preferredTheme = DARK_MODE ? new DarculaTheme() : new IntelliJTheme();
        LafManager.setTheme(preferredTheme);
        LafManager.install();
        
        // We need to rescale the fonts not only to the program's standard,
        // but also to the rescaled size according to the screen's size.
        // We will use the label font size as a standard.
        float standardizedRatio = ((float)FULL_SIZE_FONT_SIZE
                / (float)UIManager.getFont("Label.font").getSize())
                * FONT_SIZE_MULTIPLIER;
        
        for (String id : FONT_IDS) {
            Font font = UIManager.getFont(id);
            int size = Math.round(font.getSize() * standardizedRatio);
            UIManager.put(id, new Font(font.getName(), font.getStyle(), size));
        }
    }
    
    void updateStyle() {
        FONT_SIZE_MULTIPLIER = (float)Toolkit.getDefaultToolkit().getScreenSize().width / 1920f;
        
        STORY_FONT_SIZE = Math.round((float)FULL_SIZE_FONT_SIZE * FONT_SIZE_MULTIPLIER);
        
        STORY_H1_SIZE = Math.round((float)FULL_SIZE_H1_SIZE * FONT_SIZE_MULTIPLIER);
        
        STORY_H2_SIZE = Math.round((float)FULL_SIZE_H2_SIZE * FONT_SIZE_MULTIPLIER);
        
        STORY_BODY_PADDING = Math.round((float)FULL_SIZE_STORY_BODY_PADDING * FONT_SIZE_MULTIPLIER);
        if (STORY_BODY_PADDING < 1) STORY_BODY_PADDING = 1;
        
        STORY_LINE_SPACING = Math.round((float)FULL_SIZE_STORY_LINE_SPACING * FONT_SIZE_MULTIPLIER);
        if (STORY_LINE_SPACING < 1) STORY_LINE_SPACING = 1;
        
        updateLookAndFeel();
        
        SwingUtilities.invokeLater(() -> {
            storyPanel.updateStyle();
        });
    }
    
    void attemptToWriteStory() {
        RefreshThread.startPause(this);
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
                changePauseReason(instr);
            }
            else {
                thisWasAPost = false;
            }
            
            somethingWasPosted |= thisWasAPost;
        }
        RefreshThread.endPause(this);
    }
    
    private void changePauseReason(StoryPanelInstruction reason) {
        storyPanelBuffer.pauseReason = reason;
        promptHasChanged = true;
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
                changePauseReason(null);
                attemptToWriteStory();
            }
        }
        else if (e.getSource() == darkModeItem) {
            DARK_MODE = !DARK_MODE;
            darkModeItem.setState(DARK_MODE);
            //MenuSelectionManager.defaultManager().clearSelectedPath();
            updateStyle();
        }
    }
    
    public static String validateString(String str) {
        if (!isValidString(str)) {
            throw new RuntimeException("String \"" + str + "\" is not player-typable!");
        }
        return str;
    }
    
    public static boolean isValidString(String str) { //TODO: Make an iterator out of this, and reuse is PlayerPrompt.getSterileInput()
        boolean wasLastCharASpace = true; // Will invalidate leading spaces
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean isSpace = Character.isWhitespace(c);
            // spaceGate: Forbids repetition of spaces
            boolean spaceGate = (!wasLastCharASpace && isSpace) || !isSpace;
            if (!(isValidInputCharacter(c) && spaceGate)) {
                return false;
            }
            wasLastCharASpace = isSpace;
        }
        return true;
    }
    
    public static boolean isValidInputCharacter(char c) {
        switch (c) {
            default:
                return false;
            case ' ':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '0':
            case '\'':
            case '-':
                return true;
        }
    }
}
