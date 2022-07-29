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
import joeyproductions.jessadventurecore.ui.JessAdventureCore;

/**
 * Objects which the player uses to take action.
 * @author Joseph Cramsey
 */
public class Verb implements Referable { //TODO: Implement an express.js-style creation system
    
    public final String spelling;
    public final String[] synonyms;
    public final String shortcut;
    public final String[] objectPrepositions;
    private final long id;
    
    public Verb(String spelling, String[] synonyms, String shortcut,
            String[] objectPrepositions, World world) {
        if (synonyms == null) {
            throw new RuntimeException("similes cannot be null; perhaps declare a zero-length array?");
        }
        if (objectPrepositions == null) {
            throw new RuntimeException("objectPrepositions cannot be null; perhaps declare a zero-length array?");
        }
        this.spelling = JessAdventureCore.validateString(spelling);
        this.synonyms = synonyms;
        this.shortcut = shortcut;
        this.objectPrepositions = objectPrepositions;
        for (int i = 0; i < this.synonyms.length; i++) {
            this.synonyms[i] =
                    JessAdventureCore.validateString(this.synonyms[i])
                            .toLowerCase();
        }
        for (int i = 0; i < this.objectPrepositions.length; i++) {
            this.objectPrepositions[i] =
                    JessAdventureCore.validateString(this.objectPrepositions[i])
                            .toLowerCase();
        }
        this.id = world.getNextID();
    }
    
    public Verb(String spelling, World world) {
        this(spelling, new String[0], "", new String[0], world);
    }
    
    public Verb(String spelling, String shortcut, World world) {
        this(spelling, new String[0], shortcut, new String[0], world);
    }
    
    public Verb(String spelling, String[] similes, World world) {
        this(spelling, similes, "", new String[0], world);
    }
    
    public Verb(String spelling, String[] similes, String shortcut, World world) {
        this(spelling, similes, shortcut, new String[0], world);
    }
    
    public boolean isTransitive() {
        return objectPrepositions.length > 0;
    }

    @Override
    public ArrayList<VocabularyWord> gatherVocabulary() {
        ArrayList<VocabularyWord> list = new ArrayList<>();
        
        list.add(new VocabularyWord(spelling, this, spelling));
        
        for (String synonym : synonyms) {
            list.add(new VocabularyWord(synonym, this, synonym));
        }
        
        list.add(new VocabularyWord(shortcut, this, shortcut + " -> " + spelling));
        
        for (String preposition : objectPrepositions) {
            list.add(new VocabularyWord(preposition, this, preposition));
        }
        
        return list;
    }

    @Override
    public long getID() {
        return id;
    }
}
