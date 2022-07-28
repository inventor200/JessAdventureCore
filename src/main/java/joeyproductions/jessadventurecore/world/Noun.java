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
 * An association of a noun with adjectives.
 * @author Joseph Cramsey
 */
public class Noun implements Referable { //TODO: Implement an express.js-style creation system
    
    public String primaryName;
    public ArrayList<String> alternativeNames;
    public ArrayList<String> adjectives;
    
    public Noun(String namesWithSpaces, String... adjectives) {
        this.alternativeNames = new ArrayList<>();
        this.adjectives = new ArrayList<>();
        if (namesWithSpaces.isBlank()) {
            throw new RuntimeException("A noun name cannot be blank!");
        }
        String[] names = namesWithSpaces.split(" ");
        primaryName = JessAdventureCore.validateString(names[0]);
        for (int i = 1; i < names.length; i++) {
            alternativeNames.add(JessAdventureCore.validateString(names[i]));
        }
        for (String adjective : adjectives) {
            if (adjective.isBlank()) continue;
            this.adjectives.add(JessAdventureCore.validateString(adjective));
        }
    }

    @Override
    public ArrayList<VocabularyWord> gatherVocabulary() {
        ArrayList<VocabularyWord> list = new ArrayList<>();
        
        list.add(new VocabularyWord(primaryName, this));
        
        for (String alternative : alternativeNames) {
            list.add(new VocabularyWord(alternative, this));
        }
        
        for (String adjective : adjectives) {
            list.add(new VocabularyWord(adjective, this));
        }
        
        return list;
    }
}
