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
import java.util.Collection;
import java.util.ListIterator;

/**
 * A cleaned-up implementation of a 2D ArrayList, which is used a lot in the
 * PromptContext system.
 * @author Joseph Cramsey
 */
class ListSequence<T> {
    
    protected final ArrayList<ArrayList<T>> sequence;
    private ArrayList<T> lastList = null;
    
    ListSequence() {
        sequence = new ArrayList<>();
    }
    
    ArrayList<T> addEmptyList() {
        return addEmptyList(false);
    }
    
    ArrayList<T> addEmptyList(boolean forced) {
        // Always add an empty list if there are no lists, or if we are forced
        if (sequence.isEmpty() || forced) {
            ArrayList<T> list = new ArrayList<>();
            sequence.add(list);
            lastList = list;
            return list;
        }
        
        // Only add an empty list if the last list wasn't already empty!
        if (lastList.isEmpty()) {
            return lastList;
        }
        
        ArrayList<T> list = new ArrayList<>();
        sequence.add(list);
        lastList = list;
        return list;
    }
    
    ArrayList<T> getLastList() {
        if (sequence.isEmpty()) {
            // Create a last list, if there isn't one yet
            return addEmptyList(true);
        }
        
        return lastList;
    }
    
    int size() {
        return sequence.size();
    }
    
    /*ListIterator<T> listIterator(int sequenceIndex) {
        return sequence.get(sequenceIndex).listIterator();
    }*/
    
    SequenceIterator<T> sequenceIterator() {
        return sequenceIterator(0);
    }
    
    SequenceIterator<T> sequenceIterator(int index) {
        return new SequenceIterator<>(sequence, index);
    }
    
    void addToLastList(T item) {
        getLastList().add(item);
    }
    
    void addAllToLastList(Collection<T> collection) {
        getLastList().addAll(collection);
    }
    
    void addAllToLastList(T[] arr) {
        for (T item : arr) {
            addToLastList(item);
        }
    }
    
    boolean lastListContains(T item) {
        return getLastList().contains(item);
    }
    
    boolean isLastListEmpty() {
        return getLastList().isEmpty();
    }
    
    void trimEnd() {
        while (isLastListEmpty() && sequence.size() > 1) {
            lastList = sequence.get(sequence.size() - 2);
            sequence.remove(sequence.size() - 1);
        }
    }
}
