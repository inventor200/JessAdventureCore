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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * A UI component for typing out the prompt, and offering auto-complete.
 * @author Joseph Cramsey
 */
class PlayerPrompt implements HabitualRefresher {
    
    private static final int SUGGESTION_PAD = 4;
    
    final JTextField textField;
    final JButton pressToContinueButton;
    final JPanel autocompleteSuggestionPanel;
    final JPanel buttonList;
    private final JPanel inputPanel;
    private final JPanel suggestionList;
    private final JLayeredPane layeredParent;
    private final ActionListener actionListener;
    private final StoryPanelBuffer storyPanelBuffer;
    private final JPanel pressToContinueButtonResizer;
    
    private boolean needsNewSuggestions = false;
    private int autocompleteLeftOffset = 32;
    
    private class FocusPair {
        
        public Component focusComponent;
        public Component mountedComponent;
        public Class<? extends StoryPanelInstruction> pauseReason;
        
        public FocusPair(Component focusComponent, Component mountedComponent, Class<? extends StoryPanelInstruction> pauseReason) {
            this.focusComponent = focusComponent;
            this.mountedComponent = mountedComponent;
            this.pauseReason = pauseReason;
        }
    }
    
    private final FocusPair[] focusComponents;
    
    PlayerPrompt(StoryPanelBuffer storyPanelBuffer, ActionListener actionListener, JLayeredPane layeredParent) {
        this.storyPanelBuffer = storyPanelBuffer;
        this.actionListener = actionListener;
        this.layeredParent = layeredParent;
        
        PlayerPrompt _this = this;
        
        textField = new JTextField();
        JLabel marker = new JLabel(" > ");
        inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(marker);
        inputPanel.add(textField);
        
        pressToContinueButton = new JButton("Click to continue");
        pressToContinueButton.addActionListener(this.actionListener);
        pressToContinueButtonResizer = new JPanel(new BorderLayout());
        pressToContinueButtonResizer.add(pressToContinueButton, BorderLayout.PAGE_START);
        
        buttonList = new JPanel();
        buttonList.setLayout(new BoxLayout(buttonList, BoxLayout.Y_AXIS));
        
        focusComponents = new FocusPair[] {
            new FocusPair(textField, inputPanel, null),
            new FocusPair(pressToContinueButton, pressToContinueButtonResizer, PressToContinueInstruction.class)
        };
        
        buttonList.add(focusComponents[0].mountedComponent);
        
        JPanel helpLines = new JPanel(new GridLayout(0, 1));
        int helpSeparation = 4;
        
        autocompleteSuggestionPanel = new JPanel() {
            @Override
            public Rectangle getBounds(Rectangle temp) {
                int height = suggestionList.getPreferredSize().height
                        + (SUGGESTION_PAD * 2)
                        + helpLines.getHeight()
                        + helpSeparation;
                int parentHeight = _this.layeredParent.getHeight();
                
                temp.x = _this.autocompleteLeftOffset; //TODO: offset according to input string
                temp.y = parentHeight - height;
                //temp.width = _this.layeredParent.getWidth();
                temp.width = getPreferredSize().width;
                temp.height = height;
                return temp;
            }
            
            @Override
            public Rectangle getBounds() {
                return getBounds(new Rectangle());
            }
        };
        autocompleteSuggestionPanel.setLayout(
                new BoxLayout(autocompleteSuggestionPanel, BoxLayout.Y_AXIS)
        );
        autocompleteSuggestionPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK),
                        BorderFactory.createEmptyBorder(
                                SUGGESTION_PAD, SUGGESTION_PAD,
                                SUGGESTION_PAD, SUGGESTION_PAD
                        )
                )
        );
        
        JLabel selectHelp1 = new JLabel("Up / Down arrows");
        JLabel selectHelp2 = new JLabel("Enter key to select");
        Font normalFont = selectHelp1.getFont();
        int helpSize = Math.round(10f * JessAdventureCore.FONT_SIZE_MULTIPLIER);
        if (helpSize < 4) helpSize = 4;
        Font tinyFont = new Font(normalFont.getName(), Font.PLAIN, helpSize);
        selectHelp1.setFont(tinyFont);
        selectHelp2.setFont(tinyFont);
        helpLines.add(selectHelp1);
        helpLines.add(selectHelp2);
        autocompleteSuggestionPanel.add(helpLines);
        autocompleteSuggestionPanel.add(Box.createVerticalStrut(helpSeparation));
        
        JPanel suggestionListResizer = new JPanel(new BorderLayout());
        suggestionList = new JPanel(new GridLayout(0, 1));
        suggestionListResizer.add(suggestionList, BorderLayout.PAGE_START);
        autocompleteSuggestionPanel.add(suggestionListResizer);
        
        //Test
        suggestionList.add(new JLabel("Suggestion 1"));
        suggestionList.add(new JLabel("Suggestion 2"));
        suggestionList.add(new JLabel("Suggestion 3"));
        suggestionList.add(new JLabel("Suggestion 4"));
        suggestionList.add(new JLabel("Suggestion 5"));
    }
    
    void rearrange() {
        // Check to see if any of our possible focus components actually
        // had focus. If so, we want to preserve this for players that like
        // to navigate with the keyboard.
        boolean hadFocus = false;
        for (FocusPair pair : focusComponents) {
            hadFocus |= pair.focusComponent.hasFocus();
        }
        
        // Clear the components that are mounted to the window
        buttonList.removeAll();
        
        // Go through the array of possible components until one is found
        // which matches the given pause reason
        StoryPanelInstruction pauseReason = storyPanelBuffer.pauseReason;
        FocusPair visibleComponent = null;
        boolean foundComponent = false;
        
        // While there are null dereference warnings, there are checks in place
        // for those here.
        for (FocusPair pair : focusComponents) {
            if (pair.pauseReason == null) {
                // Edge case for null reasons
                foundComponent = pauseReason == null;
            }
            else {
                // General case
                foundComponent = pair.pauseReason == pauseReason.getClass();
            }
            
            if (foundComponent) {
                visibleComponent = pair;
                break;
            }
        }
        
        if (foundComponent) {
            buttonList.add(visibleComponent.mountedComponent);
            if (hadFocus) {
                visibleComponent.focusComponent.requestFocusInWindow();
            }
        }
        else {
            throw new RuntimeException("PlayerPrompt has no component to show for pause reason "
                    + pauseReason.getClass().getName());
        }
        
        buttonList.invalidate();
        buttonList.revalidate();
        buttonList.repaint();
    }

    @Override
    public boolean needsRefresh() {
        return needsNewSuggestions;
    }

    @Override
    public void handleRefresh() {
        needsNewSuggestions = false;
        //TODO
    }
}
