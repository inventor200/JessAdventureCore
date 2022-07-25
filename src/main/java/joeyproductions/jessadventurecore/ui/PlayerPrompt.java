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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
    private final JPanel suggestionCorePanel;
    private final Component suggestionShifter;
    private final JLayeredPane layeredParent;
    private final ActionListener actionListener;
    private final StoryPanelBuffer storyPanelBuffer;
    private final JPanel pressToContinueButtonResizer;
    
    // Various stuff for thread-safe operation
    private String cachedInputString = "";
    private int cachedInputCaretPosition = 0;
    private String headerString = "";
    private int autocompleteLeftOffset;
    private boolean needsNewSuggestions = true;
    private boolean shouldBeVisible = false;
    private ArrayList<SortableSuggestion> cachedSuggestions = new ArrayList<>();
    
    private String[] testSuggestions = new String[] {
        "look at",
        "examine",
        "lamp",
        "get",
        "floor",
        "hang",
        "take",
        "drop",
        "ball",
        "shelf",
        "box",
        "window",
        "inventory"
    };
    
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
        
        textField = new JTextField();
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                prepareForSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                prepareForSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                prepareForSuggestions();
            }
        });
        textField.setFocusTraversalKeysEnabled(false);
        
        textField.addCaretListener((CaretEvent e) -> {
            if (!needsNewSuggestions) {
                prepareForSuggestions();
            }
        });
        
        InputMap tabInputMap = textField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap tabActionMap = textField.getActionMap();
        tabInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "tab");
        tabActionMap.put("tab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAutocomplete();
            }
        });
        
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
        
        suggestionCorePanel = new JPanel() {
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
            
            @Override
            public boolean isVisible() {
                return shouldBeVisible;
            }
        };
        suggestionCorePanel.setLayout(
                new BoxLayout(suggestionCorePanel, BoxLayout.Y_AXIS)
        );
        suggestionCorePanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK),
                        BorderFactory.createEmptyBorder(
                                SUGGESTION_PAD, SUGGESTION_PAD,
                                SUGGESTION_PAD, SUGGESTION_PAD
                        )
                )
        );
        
        JLabel selectHelp1 = new JLabel("Up / Down arrows");
        JLabel selectHelp2 = new JLabel("Tab to select");
        Font normalFont = selectHelp1.getFont();
        int helpSize = Math.round(10f * JessAdventureCore.FONT_SIZE_MULTIPLIER);
        if (helpSize < 4) helpSize = 4;
        Font tinyFont = new Font(normalFont.getName(), Font.PLAIN, helpSize);
        selectHelp1.setFont(tinyFont);
        selectHelp2.setFont(tinyFont);
        helpLines.add(selectHelp1);
        helpLines.add(selectHelp2);
        suggestionCorePanel.add(helpLines);
        suggestionCorePanel.add(Box.createVerticalStrut(helpSeparation));
        
        JPanel suggestionListResizer = new JPanel(new BorderLayout());
        suggestionList = new JPanel(new GridLayout(0, 1));
        suggestionListResizer.add(suggestionList, BorderLayout.PAGE_START);
        suggestionCorePanel.add(suggestionListResizer);
        
        autocompleteSuggestionPanel = new JPanel() {
            @Override
            public Rectangle getBounds(Rectangle temp) {
                temp.x = 0;
                temp.y = 0;
                temp.width = layeredParent.getWidth();
                temp.height = layeredParent.getHeight();
                return temp;
            }
            
            @Override
            public Rectangle getBounds() {
                return getBounds(new Rectangle());
            }
        };
        autocompleteSuggestionPanel.setOpaque(false);
        autocompleteSuggestionPanel.setLayout(new BoxLayout(autocompleteSuggestionPanel, BoxLayout.X_AXIS));
        suggestionShifter = new Component() {
            @Override
            public Dimension getPreferredSize() {
                int maxWidth = layeredParent.getWidth();
                int suggestionWidth = suggestionCorePanel.getPreferredSize().width;
                int maxOffset = maxWidth - suggestionWidth;
                int actualOffset = Math.min(maxOffset, autocompleteLeftOffset);
                return new Dimension(actualOffset, layeredParent.getHeight());
            }
            
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        autocompleteSuggestionPanel.add(suggestionShifter);
        
        JPanel suggestionAlignerY = new JPanel();
        suggestionAlignerY.setOpaque(false);
        suggestionAlignerY.setLayout(new BoxLayout(suggestionAlignerY, BoxLayout.Y_AXIS));
        suggestionAlignerY.add(Box.createVerticalGlue());
        suggestionAlignerY.add(suggestionCorePanel);
        
        autocompleteSuggestionPanel.add(suggestionAlignerY);
        autocompleteSuggestionPanel.add(Box.createHorizontalGlue());
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
    
    private void prepareForSuggestions() {
        RefreshThread.startPause(this);
        
        cachedInputString = textField.getText();
        cachedInputCaretPosition = textField.getCaretPosition();
        
        needsNewSuggestions = true;
        RefreshThread.endPause(this);
    }

    @Override
    public boolean needsRefresh() {
        return needsNewSuggestions;
    }

    @Override
    public void handleRefresh() {
        needsNewSuggestions = false;
        shouldBeVisible = false; //TODO: Make false if there's no suggestions
        cachedSuggestions.clear();
        
        //TODO: Evaluate suggestions
        StringCaretPair workingWord = getWorkingWord();
        shouldBeVisible = !(workingWord.str.equals(""));
        
        if (shouldBeVisible) {
            //String status = "Working word: |" + workingWord.str + "|";
            //System.out.println(Integer.toString(workingWord.caretPosition) + " of " + Integer.toString(cachedInputString.length()));
            
            headerString = cachedInputString.substring(0, workingWord.caretPosition);
            
            //TODO: Get context object to narrow down possible suggestions

            if (workingWord.str.equals(" ")) {
                getSuggestionsFromContext();
            }
            else {
                getSuggestionsFromContextAndInput(workingWord.str);
            }
            
            Collections.sort(cachedSuggestions, (x, y) -> Float.compare(x.score, y.score));
            while (cachedSuggestions.size() > 5) {
                cachedSuggestions.remove(0);
            }
            
            shouldBeVisible = cachedSuggestions.size() > 0;
        }
        
        SwingUtilities.invokeLater(() -> {
            suggestionList.removeAll();
            
            for (SortableSuggestion suggestion : cachedSuggestions) {
                suggestionList.add(new JLabel(suggestion.toString()));
            }
            
            FontMetrics metrics = textField.getFontMetrics(textField.getFont());
            int offsetWithinBox = metrics.stringWidth(headerString);
            int offsetWithinPanel = textField.getLocation().x;
            autocompleteLeftOffset = offsetWithinBox + offsetWithinPanel;
            
            autocompleteSuggestionPanel.invalidate();
            autocompleteSuggestionPanel.validate();
            autocompleteSuggestionPanel.repaint();
        });
    }
    
    private void getSuggestionsFromContext() {
        //TODO
    }
    
    private void getSuggestionsFromContextAndInput(String workingInput) {
        for (String testSuggestion : testSuggestions) {
            String matchingString = testSuggestion; //TODO: Suggestions can have multiple matching strings
            float matchingFactor = 1; //TODO: Suggestion matching strings can have individual biases
            
            if (workingInput.length() > matchingString.length()) {
                // If we're already typing past the word, then don't
                // suggest it
                continue;
            }

            float bestScore = -1;

            int smallerLength = workingInput.length();
            int lengthDiff = matchingString.length() - smallerLength;

            // Match partial words too
            for (int i = 0; i <= lengthDiff; i++) {
                String fragment = matchingString.substring(i, smallerLength + i);
                int charMatches = 0;
                for (int j = 0; j < smallerLength; j++) {
                    if (workingInput.charAt(j) == fragment.charAt(j)) {
                        charMatches++;
                    }
                }

                if (charMatches == 0) {
                    // Do not interact with no-match suggestions.
                    continue;
                }

                /*
                We want to prioritize more exact matches, as well as matches
                to the immediate start of the suggestion word.
                */
                float shiftPenalty = (float)(i * i * 100) / (float)(lengthDiff + 1);
                float score = (float)(charMatches * matchingFactor)
                        / (shiftPenalty + (float)lengthDiff);

                if (score > bestScore) bestScore = score;
            }

            if (bestScore > Float.MIN_NORMAL * 2) {
                // Do not accept irrelevant suggestions
                cachedSuggestions.add(
                        new SortableSuggestion(testSuggestion, bestScore)
                );
            }
        }
    }
    
    private void handleAutocomplete() {
        //TODO
    }
    
    private StringCaretPair getSterileInput() {
        String str = cachedInputString;
        int caretPosition = cachedInputCaretPosition;
        
        String buffer = "";
        boolean wasLastCharASpace = true; // Will crop leading spaces
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean isSpace = Character.isWhitespace(c);
            // spaceGate: Keeps repetition of spaces out of input
            boolean spaceGate = (!wasLastCharASpace && isSpace) || !isSpace;
            if (isValidInputCharacter(c) && spaceGate) {
                buffer += c;
            }
            else if (i < caretPosition) {
                // Move the caret back, as we cropped a char away behind it
                caretPosition--;
            }
            wasLastCharASpace = isSpace;
        }
        
        if (buffer.isBlank()) {
            return new StringCaretPair("", 0);
        }
        
        return new StringCaretPair(buffer, caretPosition);
    }
    
    private StringCaretPair getWorkingWord(StringCaretPair sterileInput) {
        String str = sterileInput.str;
        int originalCaret = cachedInputCaretPosition;
        
        if (str.isBlank()) {
            return new StringCaretPair("", 0); // Indicates no input
        }
        
        // Any cases after this will probably have a word...
        
        int caretIndex = sterileInput.caretPosition;
        
        if (str.endsWith(" ") && caretIndex == str.length()) {
            return new StringCaretPair(" ", originalCaret); // Indicates a new word, not yet entered
        }
        
        int firstIndex = caretIndex;
        int lastIndex = caretIndex;
        
        // Find the start of the word
        for (int i = caretIndex; i >= 0; i--) {
            firstIndex = i;
            if (i < str.length()) {
                char c = str.charAt(i);
                if (Character.isWhitespace(c) || i < 0) {
                    if (i < str.length() - 1) i++;
                    firstIndex = i;
                    break;
                }
            }
        }
        
        // Find the end of the word
        for (int i = caretIndex; i < str.length(); i++) {
            lastIndex = i;
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                if (i > 0 && i > firstIndex) i--;
                lastIndex = i;
                break;
            }
        }
        lastIndex++;
        
        int wordStart = originalCaret
                + (firstIndex - caretIndex);
        
        if (wordStart < 0) {
            wordStart = 0;
        }
        if (wordStart >= cachedInputString.length()) {
            wordStart = cachedInputString.length() - 1;
        }
        
        return new StringCaretPair(str.substring(firstIndex, lastIndex), wordStart);
    }
    
    private StringCaretPair getWorkingWord() {
        return getWorkingWord(getSterileInput());
    }
    
    private static boolean isValidInputCharacter(char c) {
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
                return true;
        }
    }
}
