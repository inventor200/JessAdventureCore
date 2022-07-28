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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.TreeSet;
import joeyproductions.jessadventurecore.world.Verb;
import joeyproductions.jessadventurecore.world.VocabularyWord;
import joeyproductions.jessadventurecore.world.World;

/**
 * An object storing the context of the player's input so far.
 * @author Joseph Cramsey
 */
class PromptContext {
    
    public Verb verb;
    
    private boolean makesSense;
    //TODO: We are storing a sequence of word strings, and the world references
    //      they could be referring to. We will then choose the references
    //      which support the longest chains of consistent references.
    //
    // For example, if we have...
    // 1. A pale red sandy plastic bucket (reference ITEM_1)
    // 2. A blue clean plastic bucket (reference ITEM_2)
    // 3. A red candy (reference ITEM_3)
    // 4. A small pale red plastic bucket (reference ITEM_4)
    // 5. The input: "pale red plastic"
    // We get the following:
    // 0: PALE = [ ITEM_1, ITEM_4 ]
    // 1: RED = [ ITEM_1, ITEM_3, ITEM_4 ]
    // 2: PLASTIC = [ ITEM_1, ITEM_2, ITEM_4 ]
    //
    // While all 4 reference items got hits, ITEM_1 and ITEM-4 both got
    // unbroken streaks of reference hits for the entire input, so we will
    // load only those two items into the context for those words.
    // Because of this, for whatever the player is currently typing, the only
    // suggestions presented will be ones that either complete the noun, or
    // complete any adjectives that describe either of the two items.
    
    private PromptContext() {
        makesSense = true;
    }
    
    static PromptContext createContext(StringCaretPair sterileInput, int[] workingIndices) {
        World world = JessAdventureCore.CORE.world;
        PromptContext context = new PromptContext();
        
        // We are not wasting our computation time wondering what the player was
        // currently typing. We are only concerned with the fully-complete
        // words already typed, or halting when the input so far makes no sense.
        // For this reason, we are cleaving off the word which contains the
        // player's input caret.
        String relevantPart = sterileInput.str
                .substring(0, workingIndices[0]).trim();
        int relevantLength = relevantPart.length();
        
        //TODO: Process strings into possible vocabulary matches, starting from
        //      longest strings, and working our way shorter.
        
        TreeSet<VocabularyWord> vocab = new TreeSet<>();
        world.loadRelevantVocabulary(vocab);
        ArrayList<ArrayList<VocabularyWord>> referenceSequence = new ArrayList<>();
        
        while (relevantPart.length() > 0) {
            boolean foundMatch = false;
            
            ArrayList<VocabularyWord> references = new ArrayList<>();
            referenceSequence.add(references);
            Iterator<VocabularyWord> iter = vocab.iterator();
            int longestLength = vocab.first().str.length();
            
            // Find matches
            while (iter.hasNext()) {
                VocabularyWord word = iter.next();
                int wordLength = word.str.length();
                
                if (wordLength > relevantLength) {
                    // This word is too long; it won't match.
                    continue;
                }
                
                if (foundMatch && wordLength < longestLength) {
                    // If we found a match before, so we cannot match shorter
                    // words, because they will always be less of a match.
                    break;
                }
                
                if (relevantPart.startsWith(word.str)) {
                    foundMatch = true;
                    references.add(word);
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
                context.makesSense = false;
                return context;
            }
            
            // Crop what we've matched
            relevantPart = relevantPart.substring(longestLength);
            relevantLength = relevantPart.length();
            
            // If we actually did this right, the new relevantPart should
            // start with a space (or be empty), because we should be
            // matching whole words. If this check fails, then the player
            // wrote a longer word than anything we could match with.
            if (relevantLength > 0) {
                if (relevantPart.charAt(0) != ' ') {
                    // Aha! We have cropped off the beginning of a long word
                    // that we do not recognize. We are lost.
                    context.makesSense = false;
                    return context;
                }
            }
            
            // With our check for success, we know we can continue with the
            // rest of the input.
            relevantPart = relevantPart.trim();
            relevantLength = relevantPart.length();
        }
        
        // Make sure our first word is a verb
        ArrayList<VocabularyWord> firstList = referenceSequence.get(0);
        ListIterator<VocabularyWord> verbIter = firstList.listIterator();
        
        while (verbIter.hasNext()) {
            VocabularyWord possibleVerb = verbIter.next();
            if (!(possibleVerb.referable instanceof Verb)) {
                verbIter.remove();
            }
        }
        
        // If the resulting first list in the sequence is empty, then the
        // player did not start their input with a verb, and makes no sense.
        if (firstList.isEmpty()) {
            context.makesSense = false;
            return context;
        }
        
        // TODO: Using the matches we found, weave the most-likely sequence
        
        return context;
    }
    
    boolean makesSense() {
        return makesSense;
    }
}
