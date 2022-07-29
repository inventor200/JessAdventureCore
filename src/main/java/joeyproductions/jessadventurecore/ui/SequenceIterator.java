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
import java.util.ListIterator;

/**
 * A handy iterator for ListSequences.
 * This assumes a while loop nested in another while loop, both of which are
 * calling hasNext() and next() to iterate through each list.
 * Responses are timed accordingly to reduce complexity.
 * @author Joseph Cramsey
 */
class SequenceIterator<T> {
    
    private final ListIterator<ArrayList<T>> sequenceIterator;
    private ListIterator<T> listIterator;
    private boolean isListMode;
    private ArrayList<T> currentList;
    
    SequenceIterator(ArrayList<ArrayList<T>> source, int index) {
        this.sequenceIterator = source.listIterator(index);
        this.isListMode = false;
    }
    
    boolean hasNext() {
        if (isListMode) {
            boolean response = listIterator.hasNext();
            if (!response) {
                // This list is done. The inner while-loop will see the
                // upcoming "false" value, and exit. The outer loop will
                // take over next.
                isListMode = false;
            }
            return response;
        }
        else {
            return sequenceIterator.hasNext();
        }
    }
    
    T next() {
        if (isListMode) {
            return listIterator.next();
        }
        else {
            // The outer while-loop is intending to move onto the next list
            // in the sequence. The inner while-loop is awaiting.
            currentList = sequenceIterator.next();
            listIterator = currentList.listIterator();
            isListMode = true;
            return null;
        }
    }
    
    void remove() {
        if (isListMode) {
            listIterator.remove();
        }
        else {
            sequenceIterator.remove();
        }
    }
    
    ArrayList<T> peekList() {
        return currentList;
    }
}
