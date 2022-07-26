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

/**
 * An object storing the context of the player's input so far.
 * @author Joseph Cramsey
 */
class PromptContext {
    
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
    
    PromptContext(StringCaretPair sterileInput, int[] workingIndices) { //TODO: Needs world object
        // We are not wasting our computation time wondering what the player was
        // currently typing. We are only concerned with the fully-complete
        // words already typed, or halting when the input so far makes no sense.
        // For this reason, we are cleaving off the word which contains the
        // player's input caret.
        String relevantPart = sterileInput.str
                .substring(0, workingIndices[0]).trim();
        
        //TODO: Process strings into possible vocabulary matches, starting from
        //      longest strings, and working our way shorter.
    }
}
