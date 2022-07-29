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
import java.awt.event.KeyAdapter;
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
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import joeyproductions.jessadventurecore.world.VocabularyWord;

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
    private final int[] workingIndices = new int[] { 0, 0 };
    private int autocompleteLeftOffset;
    private boolean needsNewSuggestions = true;
    private boolean doSuggestions = false;
    private final ArrayList<SortableSuggestion> cachedSuggestions = new ArrayList<>();
    
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
                //System.out.println("Insert: " + textField.getText());
                prepareForSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                //System.out.println("Remove: " + textField.getText());
                prepareForSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Irrelevant
            }
        });
        AbstractDocument doc = (AbstractDocument)textField.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            private String getFilteredString(String string) {
                String buffer = "";
                for (int i = 0; i < string.length(); i++) {
                    char c = string.charAt(i);
                    if (JessAdventureCore.isValidInputCharacter(c)) {
                        buffer += c;
                    }
                }
                
                return buffer;
            }
            
            @Override
            public void insertString
                (DocumentFilter.FilterBypass fb, int offset, String string,
                        AttributeSet attr) throws BadLocationException {
                    
                String filtered = getFilteredString(string);
                
                //System.out.println("Filtered insert: " + filtered);
                
                super.insertString(fb, offset, filtered, attr);
            }
                
            @Override
            public void replace
                (DocumentFilter.FilterBypass fb, int offset, int length, String string,
                        AttributeSet attr) throws BadLocationException {
                    
                String filtered = getFilteredString(string);
                
                //System.out.println("Filtered replace: " + filtered);
                
                super.replace(fb, offset, length, filtered, attr);
            }
        });
        
        //TODO: Make autocomplete optional: Off, No popup, With popup
        
        textField.addCaretListener((CaretEvent e) -> {
            if (!needsNewSuggestions) {
                prepareForSuggestions();
            }
        });
        
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                switch (c) {
                    case KeyEvent.VK_BACK_SPACE:
                    case KeyEvent.VK_TAB:
                        return;
                    case ';':
                        handleAutocomplete();
                        e.consume();
                        return;
                    case '=':
                        readSuggestions();
                        e.consume();
                        return;
                }
                
                if (!JessAdventureCore.isValidInputCharacter(c)) {
                    e.consume();
                }
            }
        });
        
        JLabel marker = new JLabel(" > "); //TODO: State current character
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
                return doSuggestions;
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
        doSuggestions = false;
        cachedSuggestions.clear();
        
        //StringCaretPair sterileInput = getSterileInput();
        StringCaretPair sterileInput =
                new StringCaretPair(cachedInputString, cachedInputCaretPosition);
        getWorkingIndices(sterileInput, workingIndices);
        String workingWord = getWorkingWord();
        doSuggestions = !(workingWord.equals(""));
        
        if (doSuggestions) {
            headerString = cachedInputString.substring(0, workingIndices[0]);
            
            try {
                PromptContext contextObject = PromptContext
                        .createContext(sterileInput.str, workingIndices);

                System.out.println(contextObject.toString());
                
                // Only make suggestions if we understand the input so far
                if (workingWord.equals(" ")) {
                    getSuggestionsFromContext(contextObject);
                }
                else {
                    getSuggestionsFromContextAndInput(contextObject, workingWord);
                }

                Collections.sort(cachedSuggestions, (x, y)
                        -> Float.compare(x.score, y.score));
                while (cachedSuggestions.size() > JessAdventureCore.MAX_SUGGESTION_COUNT) {
                    cachedSuggestions.remove(0);
                }

                doSuggestions = cachedSuggestions.size() > 0;
            } catch (ContextException ex) {
                // This is actually fine; we just won't handle suggestions
                doSuggestions = false;
            } catch (FatalContextException ex) {
                doSuggestions = false;
                ex.printStackTrace(System.err);
            }
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
    
    private void getSuggestionsFromContext(PromptContext contextObject) {
        int count = 0;
        for (VocabularyWord suggestion : contextObject.suggestions) {
            float matchingFactor = 1; //TODO: Suggestion matching strings can have individual biases
            float bias = matchingFactor + ((float)count / 1000f);
            cachedSuggestions.add(new SortableSuggestion(suggestion, bias));
            count++;
        }
    }
    
    private void getSuggestionsFromContextAndInput
        (PromptContext contextObject, String workingInput) {
            
        String caselessInput = workingInput.toLowerCase();
            
        for (VocabularyWord suggestion : contextObject.suggestions) {
            String matchingString = suggestion.str; //TODO: Suggestions can have multiple matching strings
            float matchingFactor = 1; //TODO: Suggestion matching strings can have individual biases
            
            if (caselessInput.length() > matchingString.length()) {
                // If we're already typing past the word, then don't
                // suggest it
                continue;
            }

            float bestScore = -1;

            int smallerLength = caselessInput.length();
            int lengthDiff = matchingString.length() - smallerLength;

            // Match partial words too
            for (int i = 0; i <= lengthDiff; i++) {
                String fragment = matchingString
                        .substring(i, smallerLength + i).toLowerCase();
                int charMatches = 0;
                for (int j = 0; j < smallerLength; j++) {
                    if (caselessInput.charAt(j) == fragment.charAt(j)) {
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
                        new SortableSuggestion(suggestion, bestScore)
                );
            }
        }
    }
    
    private void handleAutocomplete() {
        //TODO
        System.out.println("Autocomplete");
    }
    
    private void readSuggestions() {
        //TODO
        System.out.println("Read suggestions");
    }
    
    /*private StringCaretPair getSterileInput() {
        String str = cachedInputString;
        int caretPosition = cachedInputCaretPosition;
        
        String buffer = "";
        boolean wasLastCharASpace = true; // Will crop leading spaces
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean isSpace = Character.isWhitespace(c);
            // spaceGate: Keeps repetition of spaces out of input
            boolean spaceGate = (!wasLastCharASpace && isSpace) || !isSpace;
            if (JessAdventureCore.isValidInputCharacter(c) && spaceGate) {
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
    }*/
    
    private static void getWorkingIndices(StringCaretPair sterileInput, int[] buffer) {
        if (buffer == null) {
            throw new RuntimeException("Index buffer is null!");
        }
        if (buffer.length != 2) {
            throw new RuntimeException("Index buffer is not of length 2!");
        }
        
        String str = sterileInput.str;
        int caretIndex = sterileInput.caretPosition;
        
        if (str.endsWith(" ") && caretIndex == str.length()) {
            buffer[0] = str.length() - 1;
            buffer[1] = str.length();
            return;
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
        
        buffer[0] = firstIndex;
        buffer[1] = lastIndex;
    }
    
    private String getWorkingWord() {
        String str = cachedInputString;
        
        if (str.isBlank()) {
            return ""; // Indicates no input
        }
        
        // Any cases after this will probably have a word...
        
        if (str.endsWith(" ") && cachedInputCaretPosition == str.length()) {
            return " "; // Indicates a new word, not yet entered
        }
        
        int firstIndex = workingIndices[0];
        int lastIndex = workingIndices[1];
        
        if (firstIndex < 0) {
            firstIndex = 0;
        }
        
        if (lastIndex > str.length()) lastIndex = str.length();
        
        return str.substring(firstIndex, lastIndex);
    }
}
