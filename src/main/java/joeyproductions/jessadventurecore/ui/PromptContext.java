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
import joeyproductions.jessadventurecore.world.Verb;
import joeyproductions.jessadventurecore.world.VocabularySuggestionComparator;
import joeyproductions.jessadventurecore.world.VocabularyWord;
import joeyproductions.jessadventurecore.world.World;

/**
 * An object storing the context of the player's input so far.
 * @author Joseph Cramsey
 */
class PromptContext extends ListSequence<SyntaxObject> {
    
    public VocabularyWord[] suggestions;
    
    //private final ArrayList<ArrayList<SyntaxObject>> syntaxSequence;
    
    // We are storing a sequence of word strings, and the world references
    // they could be referring to. We then choose the references
    // which support the longest chains of consistent references.
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
    
    private PromptContext() {
        super();
        suggestions = new VocabularyWord[0];
    }
    
    boolean isInstanceOf(int sequenceIndex, Class<? extends SyntaxObject> listType) {
        ArrayList<SyntaxObject> list = sequence.get(sequenceIndex);
        if (list.isEmpty()) {
            return false;
        }
        return listType.isInstance(list.get(0));
    }
    
    String getClassNameOf(int sequenceIndex) {
        ArrayList<SyntaxObject> list = sequence.get(sequenceIndex);
        if (list.isEmpty()) {
            return "null";
        }
        return list.get(0).getClass().getName();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("//// CONTEXT\n");
        
        SequenceIterator<SyntaxObject> iter = sequenceIterator();
        int index = 0;
        while (iter.hasNext()) {
            str.append("  ").append(Integer.toString(index)).append(" - ");
            str.append(getClassNameOf(index)).append("\n");
            //ListIterator<SyntaxObject> iter = listIterator(i);
            iter.next();
            while (iter.hasNext()) {
                SyntaxObject obj = iter.next();
                str.append("    ").append(obj.toString()).append("\n");
            }
            index++;
        }
        
        return str.toString();
    }
    
    static PromptContext createContext
        (String sterileInput) throws ContextException, FatalContextException {
            
        int len = sterileInput.length();
        return createContext(sterileInput, new int[] { len, len });
    }
    
    static PromptContext createContext
        (String sterileInput, int[] workingIndices)
                throws ContextException, FatalContextException {
            
        World world = JessAdventureCore.CORE.world;
        PromptContext context = new PromptContext();
        
        // If the low working index is zero, then the player has not finished
        // typing in the first word, so we can just recommend verbs for auto-
        // completion suggestions.
        
        if (workingIndices[0] == 0) {
            ArrayList<VocabularyWord> verbSuggestions = new ArrayList<>();
            ListIterator<Verb> iter = world.verbs.listIterator();
            while (iter.hasNext()) {
                verbSuggestions.addAll(iter.next().gatherVocabulary());
            }
            context.suggestions =
                    verbSuggestions.toArray(new VocabularyWord[verbSuggestions.size()]);
            return context;
        }
        
        
        String relevantPart;
        
        if (workingIndices[0] == workingIndices[1] && workingIndices[0] == sterileInput.length()) {
            // Both working indices at the end of the string indicate that
            // we are evaluating the entire string.
            relevantPart = sterileInput.trim();
        }
        else {
            // We are not wasting our computation time wondering what the player
            // was currently typing. We are only concerned with the fully-
            // complete words already typed, or halting when the input so far
            // makes no sense.
            // For this reason, we are cleaving off the word which contains the
            // player's input caret.
            relevantPart = sterileInput.substring(0, workingIndices[0]).trim();
        }
        
        //System.out.println("Relevant part: |" + relevantPart + "|");
        
        TreeSet<VocabularyWord> vocab = new TreeSet<>();
        world.loadRelevantVocabulary(vocab);
        ListSequence<VocabularyWord> referenceSequence =
                ContextVocabBuilder.buildVocabSequence(relevantPart, vocab);
        
        // Make sure our first word is a verb, and following words are not
        SequenceIterator<VocabularyWord> refSeqIter = referenceSequence.sequenceIterator();
        boolean requiringVerb = true;
        
        while (refSeqIter.hasNext()) {
            refSeqIter.next();

            while (refSeqIter.hasNext()) {
                VocabularyWord listItem = refSeqIter.next();
                if ((listItem.referable instanceof Verb) != requiringVerb) {
                    // Remove a possible reference if it does not meet the
                    // part-of-speech requirement.
                    refSeqIter.remove();
                }
            }

            // If the resulting list in the sequence is empty, then the
            // player did not start their input with the correct pare of speech,
            // and makes no sense as an input.
            if (refSeqIter.peekList().isEmpty()) {
                throw new ContextException("Incorrect part of speech");
            }
            
            // Only the first word in the input is a verb
            requiringVerb = false;
        }
        
        // IF WE HAVE MADE IT THIS FAR, then we can confirm that we have matched
        // a verb AT THE LEAST. So, at this point, we should identify the
        // prepositions being used, and then narrow down the verb from there.
        
        //TODO: Implement proper prepositions
        
        // TODO: Handle nouns that have a preposition in their adjectives
        //       "Angel with large wings"
        // For now, we are assuming no nouns are described with prepositions
        
        ContextSequenceWeaver.weave(context, referenceSequence);
        
        // Now that the nouns are collected into clusters, we can clear out
        // the ones that have broken streaks, as the player probably was not
        // intending to type those.
        
        if (context.isLastListEmpty()) {
            context.trimEnd();
        }
        
        SequenceIterator<SyntaxObject> synIter = context.sequenceIterator();
        while (synIter.hasNext()) {
            synIter.next();
            while (synIter.hasNext()) {
                SyntaxObject synObj = synIter.next();
                if (synObj instanceof NounProfileCluster) {
                    NounProfileCluster cluster = (NounProfileCluster)synObj;
                    if (!cluster.hasClusterStreak()) {
                        synIter.remove();
                    }
                }
            }
            
            if (synIter.peekList().isEmpty()) {
                // We couldn't find a noun that is being consistently
                // referred to, so the input doesn't make sense.
                throw new ContextException("Failed to find noun with full streak");
            }
        }
        
        // Gather noun starters
        TreeSet<NounProfile> vocabNouns = new TreeSet<>();
        ArrayList<VocabularyWord> nounStarters = new ArrayList<>();
        Iterator<VocabularyWord> vocabIter = vocab.iterator();
        while (vocabIter.hasNext()) {
            VocabularyWord word = vocabIter.next();
            if (word.isNoun()) {
                if (!vocabNouns.contains(word.nounProfile)) {
                    nounStarters.addAll(word.nounProfile.wordList);
                    vocabNouns.add(word.nounProfile);
                }
            }
        }
        
        // Using the sequence, fill out the best suggestions.
        // If the last item in the sequence is not a noun cluster, then we
        // will suggest starter words for other nouns.
        // Otherwise, we will suggest words that finish the current nouns.
        TreeSet<VocabularyWord> suggestions = new TreeSet<>(
                new VocabularySuggestionComparator()
        );
        ArrayList<SyntaxObject> lastList = context.getLastList();
        ListIterator<SyntaxObject> lastIter = lastList.listIterator();
        while (lastIter.hasNext()) {
            SyntaxObject synObj = lastIter.next();
            if (synObj instanceof NounProfileCluster) {
                NounProfileCluster cluster = (NounProfileCluster)synObj;
                if (!cluster.isComplete) {
                    suggestions.addAll(cluster.unmentioned);
                    continue;
                }
            }
            
            suggestions.addAll(nounStarters);
            break;
        }
        
        context.suggestions = suggestions.toArray(new VocabularyWord[suggestions.size()]);
        
        return context;
    }
}
