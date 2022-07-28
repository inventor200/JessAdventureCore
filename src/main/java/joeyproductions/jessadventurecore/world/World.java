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

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.TreeSet;

/**
 * The object that contains all objects which can be referred to by the player.
 * @author Joseph Cramsey
 */
public class World {
    
    // The final product will be a lot more complex; this is the cardboard
    // cutout version for testing.
    public final ArrayList<Noun> nouns;
    public final ArrayList<Verb> verbs;
    private Runnable startMethod;
    
    private World() {
        this.nouns = new ArrayList<>();
        this.verbs = new ArrayList<>();
    }
    
    public static World createWorld() {
        World world = new World();
        
        //TODO: Load all the defaults here
        
        return world;
    }
    
    // This should only be called by the adventure core
    public void prestartWorld() {
        startWorldDefaults();
        if (startMethod != null) {
            startMethod.run();
        }
    }
    
    private void startWorldDefaults() {
        //
    }
    
    public void addStartMethod(Runnable startMethod) {
        this.startMethod = startMethod;
    }
    
    //TODO: Once we get enough complexity, this will only gather vocab from
    //      stuff the player can interact with
    public void loadRelevantVocabulary(TreeSet<VocabularyWord> buffer) {
        for (Verb verb : verbs) {
            ArrayList<VocabularyWord> words = verb.gatherVocabulary();
            ListIterator<VocabularyWord> iter = words.listIterator();
            while (iter.hasNext()) {
                buffer.add(iter.next());
            }
        }
        
        for (Noun noun : nouns) {
            ArrayList<VocabularyWord> words = noun.gatherVocabulary();
            ListIterator<VocabularyWord> iter = words.listIterator();
            while (iter.hasNext()) {
                buffer.add(iter.next());
            }
        }
    }
}
