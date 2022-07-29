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
import java.util.TreeSet;
import joeyproductions.jessadventurecore.world.Noun;
import joeyproductions.jessadventurecore.world.VocabularyWord;

/**
 * A class for checking how a noun is addressed in the player's input.
 * @author Joseph Cramsey
 */
public class NounProfile implements Comparable<NounProfile> {
    
    final Noun noun;
    final ArrayList<VocabularyWord> wordList;
    final TreeSet<VocabularyWord> actualNouns;
    
    private final ArrayList<NounProfileCluster> clusters;
    
    private NounProfile(Noun noun,
            ArrayList<VocabularyWord> wordList,
            TreeSet<VocabularyWord> actualNouns) {
        this.noun = noun;
        this.wordList = wordList;
        this.actualNouns = actualNouns;
        clusters = new ArrayList<>();
    }
    
    private NounProfileCluster getLastCluster(int clusterIndex) {
        if (clusters.isEmpty()) {
            clusters.add(new NounProfileCluster(clusterIndex, wordList));
        }
        return clusters.get(clusters.size() - 1);
    }
    
    NounProfileCluster markAsMentioned(VocabularyWord word,
            int clusterIndex, int streakIndex) {
        NounProfileCluster cluster = getClusterFromIndex(clusterIndex);
        
        cluster.markAsMentioned(word, streakIndex);
        
        return cluster;
    }
    
    void markAsMissed(int clusterIndex, int streakIndex) {
        getClusterFromIndex(clusterIndex).markAsMissed(streakIndex);
    }
    
    NounProfileCluster getClusterFromIndex(int clusterIndex) {
        NounProfileCluster cluster = getLastCluster(clusterIndex);
        
        if (cluster.clusterIndex < clusterIndex) {
            cluster = new NounProfileCluster(clusterIndex, wordList);
            clusters.add(cluster);
        }
        
        return cluster;
    }
    
    public static void apply(Noun noun,
            ArrayList<VocabularyWord> wordList,
            TreeSet<VocabularyWord> actualNouns) {
        NounProfile profile = new NounProfile(noun, wordList, actualNouns);
        ListIterator<VocabularyWord> iter = wordList.listIterator();
        while (iter.hasNext()) {
            iter.next().nounProfile = profile;
        }
    }

    @Override
    public int compareTo(NounProfile o) {
        return Long.compare(this.noun.getID(), o.noun.getID());
    }
}
