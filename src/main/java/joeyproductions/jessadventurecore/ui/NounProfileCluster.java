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
import joeyproductions.jessadventurecore.world.SyntaxObject;
import joeyproductions.jessadventurecore.world.VocabularySimpleComparator;
import joeyproductions.jessadventurecore.world.VocabularyWord;

/**
 * A class for labeling the same noun profile in multiple noun phrase clusters.
 * @author Joseph Cramsey
 */
public class NounProfileCluster implements SyntaxObject {
    
    final int clusterIndex;
    final ArrayList<Boolean> marks;
    final TreeSet<VocabularyWord> unmentioned;
    final TreeSet<VocabularyWord> mentioned;
    
    NounProfileCluster(int clusterIndex, ArrayList<VocabularyWord> wordList) {
        this.clusterIndex = clusterIndex;
        marks = new ArrayList<>();
        VocabularySimpleComparator comp = new VocabularySimpleComparator();
        unmentioned = new TreeSet<>(comp);
        mentioned = new TreeSet<>(comp);
        unmentioned.addAll(wordList);
    }
    
    boolean isRelevantTo(VocabularyWord word) {
        return unmentioned.contains(word) || mentioned.contains(word);
    }
    
    void markAsMentioned(VocabularyWord word, int streakIndex) {
        if (unmentioned.contains(word)) {
            unmentioned.remove(word);
        }
        if (!mentioned.contains(word)) {
            mentioned.add(word);
        }
        setMarks(streakIndex, true);
    }
    
    void markAsMissed(int streakIndex) {
        setMarks(streakIndex, false);
    }
    
    private void setMarks(int streakIndex, boolean latest) {
        for (int i = marks.size(); i <= streakIndex; i++) {
            marks.add(false);
        }
        marks.set(streakIndex, latest);
    }
    
    boolean hasClusterStreak() {
        ListIterator<Boolean> iter = marks.listIterator();
        
        while (iter.hasNext()) {
            if (iter.next() == false) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Boolean mark : marks) {
            str.append(mark ? "X" : ".");
        }
        str.append(" | ");
        Iterator<VocabularyWord> iter = mentioned.iterator();
        while (iter.hasNext()) {
            str.append(iter.next().toString());
            if (iter.hasNext()) {
                str.append(", ");
            }
        }
        
        return str.toString();
    }
}
