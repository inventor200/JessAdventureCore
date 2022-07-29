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
package joeyproductions.jessadventurecore.world;

import joeyproductions.jessadventurecore.ui.NounProfile;

/**
 * A String with an associated Referable.
 * @author Joseph Cramsey
 */
public class VocabularyWord implements Comparable<VocabularyWord>, SyntaxObject {
    
    public final String str;
    public final Referable referable;
    public final String suggestionStr;
    public NounProfile nounProfile;
    
    public VocabularyWord(String str, Referable referable, String suggestionStr) {
        this.str = str;
        this.referable = referable;
        this.suggestionStr = suggestionStr;
        this.nounProfile = null;
    }

    @Override
    public int compareTo(VocabularyWord o) {
        int tLen = this.str.length();
        int oLen = o.str.length();
        
        // First, compare by length
        if (tLen == oLen) {
            // Then, compare alphabetically
            int strComp = this.str.compareToIgnoreCase(o.str);
            if (strComp == 0) {
                // Then, compare by referable ID
                return Long.compare(this.referable.getID(), o.referable.getID());
            }
            return strComp;
        }
        return Integer.compare(oLen, tLen);
    }
    
    public boolean isNoun() {
        return nounProfile != null;
    }
    
    @Override
    public String toString() {
        return str;
    }
}
