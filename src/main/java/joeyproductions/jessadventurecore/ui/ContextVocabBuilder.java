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

import java.util.Iterator;
import java.util.TreeSet;
import joeyproductions.jessadventurecore.world.VocabularyWord;

/**
 * A subsystem of PromptContext, which finds the best-match
 * VocabularyWords, based on an input string.
 * @author Joseph Cramsey
 */
class ContextVocabBuilder {
    
    //
    
    private ContextVocabBuilder() {
        //
    }
    
    static ListSequence<VocabularyWord> buildVocabSequence
        (String relevantPart, TreeSet<VocabularyWord> vocab) throws ContextException {
        
        ListSequence<VocabularyWord> referenceSequence = new ListSequence<>();
        
        while (relevantPart.length() > 0) {
            boolean foundMatch = false;
            
            referenceSequence.addEmptyList();
            Iterator<VocabularyWord> iter = vocab.iterator();
            int longestLength = vocab.first().str.length();
            
            // Find matches
            while (iter.hasNext()) {
                VocabularyWord word = iter.next();
                int wordLength = word.str.length();
                
                if (wordLength > relevantPart.length()) {
                    // This word is too long; it won't match.
                    continue;
                }
                
                if (foundMatch && wordLength < longestLength) {
                    // If we found a match before, so we cannot match shorter
                    // words, because they will always be less of a match.
                    break;
                }
                
                if (relevantPart.toLowerCase().startsWith(word.str.toLowerCase())) {
                    foundMatch = true;
                    referenceSequence.addToLastList(word);
                    longestLength = wordLength;
                }
                
                if (!foundMatch) {
                    // If we have not yet found a match, we can keep adjusting
                    // the last longest length we were comparing with.
                    longestLength = wordLength;
                }
            }
            
            if (!foundMatch) {
                // We have lost our way lol.
                // We could not match anything with what the player wrote.
                throw new ContextException("No vocabulary match found");
            }
            
            // Crop what we've matched
            relevantPart = relevantPart.substring(longestLength);
            
            // If we actually did this right, the new relevantPart should
            // start with a space (or be empty), because we should be
            // matching whole words. If this check fails, then the player
            // wrote a longer word than anything we could match with.
            if (relevantPart.length() > 0) {
                if (relevantPart.charAt(0) != ' ') {
                    // Aha! We have cropped off the beginning of a long word
                    // that we do not recognize. We are lost.
                    throw new ContextException("Failed to match whole word. Remainder: \"" + relevantPart + "\"");
                }
            }
            
            // With our check for success, we know we can continue with the
            // rest of the input.
            relevantPart = relevantPart.trim();
        }
        
        return referenceSequence;
    }
}
