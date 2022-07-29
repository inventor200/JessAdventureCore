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
import joeyproductions.jessadventurecore.world.VocabularyWord;

/**
 * A tidy utility class for assembling the SyntaxObject sequence in the final
 * major stage of the ContextObject function.
 * @author Joseph Cramsey
 */
class ContextSequenceWeaver {
    
    private final PromptContext context;
    private final ListSequence<VocabularyWord> referenceSequence;
    private int currentClusterIndex;
    private int currentStreakIndex;
    private final ArrayList<NounProfile> profilesInCluster;
    private SequenceIterator<VocabularyWord> refSeqIter;
    
    private ContextSequenceWeaver(PromptContext context, ListSequence<VocabularyWord> referenceSequence) {
        this.context = context;
        this.referenceSequence = referenceSequence;
        this.currentClusterIndex = 0;
        this.currentStreakIndex = 0;
        this.profilesInCluster = new ArrayList<>();
    }
    
    static void weave(PromptContext context, ListSequence<VocabularyWord> referenceSequence) throws ContextException, FatalContextException {
        ContextSequenceWeaver weaver = new ContextSequenceWeaver(context, referenceSequence);
        
        weaver.refSeqIter = referenceSequence.sequenceIterator();
        weaver.loadVerbs();
        weaver.buildClusters();
    }
    
    private void loadVerbs() {
        if (refSeqIter.hasNext()) {
            
            // Passes into list mode
            refSeqIter.next();
            
            while (refSeqIter.hasNext()) {
                context.addToLastList(refSeqIter.next());
            }
            
            // Passes out of list mode
        }
        
        // After this, the list index will be 1, so we don't need to
        // re-initialize this iterator. Reduce, reuse, recycle!
        
        context.addEmptyList();
    }
    
    private boolean hasPreposition() {
        ListIterator<VocabularyWord> peekIter = refSeqIter.peekList().listIterator();
            
        // Check for prepositions
        while (peekIter.hasNext()) {
            VocabularyWord word = peekIter.next();
            if (!word.isNoun()) {
                return true;
            }
        }
        
        return false;
    }
    
    private void buildClusters() throws FatalContextException {
        // Build noun profile clusters, so we don't suggest descriptors that
        // have already been typed in before.
        // Separate these clusters by non-noun-related words.
        // Since English is an adjective-noun-ordered language, we will assume
        // that when a noun occurs, that it marks the end of the cluster.
        
        while (refSeqIter.hasNext()) {
            refSeqIter.next();
            
            if (hasPreposition()) {
                breakCluster();
                
                // Filter out non-prepositions, if found
                while (refSeqIter.hasNext()) {
                    VocabularyWord word = refSeqIter.next();
                    if (word.isNoun()) {
                        refSeqIter.remove();
                    }
                    else if (context.isLastListEmpty()) {
                        // Add prepositions to the list.
                        // We'll be using a special algorithm later if we're
                        // adding nouns and their adjectives.
                        context.addToLastList(word);
                    }
                    else {
                        throw new FatalContextException(
                                "We have multiple matches for preposition \""
                                + word.str + "\"!"
                        );
                    }
                }
            }
            else {
                handleNouns();
            }
        }
    }
    
    private void breakCluster() {
        currentClusterIndex++;
        currentStreakIndex = 0;
        context.addEmptyList();
        profilesInCluster.clear();
    }
    
    private void handleNouns() {
        boolean actualNounFound = false;
        
        // Gather noun clusters, if no prepositions were found here
        while (refSeqIter.hasNext()) {
            VocabularyWord word = refSeqIter.next();

            // Add it to the cluster's profile list so it can be marked
            // as missed, if necessary.
            if (!profilesInCluster.contains(word.nounProfile)) {
                profilesInCluster.add(word.nounProfile);
            }

            // Make the necessary marks
            ListIterator<NounProfile> profileIter =
                    profilesInCluster.listIterator();
            while (profileIter.hasNext()) {
                NounProfile profile = profileIter.next();

                NounProfileCluster cluster =
                        profile.getClusterFromIndex(currentClusterIndex);

                if (!context.lastListContains(cluster)) {
                    context.addToLastList(cluster);
                }

                if (cluster.isRelevantTo(word)) {
                    profile.markAsMentioned(
                            word,
                            currentClusterIndex, currentStreakIndex
                    );
                }
                else {
                    profile.markAsMissed(
                            currentClusterIndex,
                            currentStreakIndex
                    );
                }

                if (profile.actualNouns.contains(word)) {
                    // This is the actual noun, so we will end the noun
                    // cluster here.
                    cluster.isComplete = true;
                    actualNounFound = true;
                }
            }
        }
        
        currentStreakIndex++;
        
        if (actualNounFound) {
            breakCluster();
        }
    }
}
