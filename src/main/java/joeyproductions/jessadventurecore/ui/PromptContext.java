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
class PromptContext {
    
    //public Verb verb;
    public VocabularyWord[] suggestions;
    
    private ArrayList<ArrayList<SyntaxObject>> syntaxSequence;
    private boolean makesSense;
    private String nonsenseReason = "No reason given";
    //TODO: We are storing a sequence of word strings, and the world references
    //      they could be referring to. We will then choose the references
    //      which support the longest chains of consistent references.
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
        //verb = null;
        syntaxSequence = new ArrayList<>();
        suggestions = new VocabularyWord[0];
        makesSense = true;
    }
    
    int size() {
        if (!makesSense) {
            return 0;
        }
        return syntaxSequence.size();
    }
    
    ListIterator<SyntaxObject> listIterator(int sequenceIndex) {
        if (!makesSense) {
            throw new RuntimeException(
                    "Cannot get an iterator from a context that makes no sense"
            );
        }
        return syntaxSequence.get(sequenceIndex).listIterator();
    }
    
    boolean isInstanceOf(int sequenceIndex, Class<? extends SyntaxObject> listType) {
        if (!makesSense) {
            return false;
        }
        ArrayList<SyntaxObject> list = syntaxSequence.get(sequenceIndex);
        if (list.isEmpty()) {
            return false;
        }
        return listType.isInstance(list.get(0));
    }
    
    String getClassNameOf(int sequenceIndex) {
        if (!makesSense) {
            return "nonsense";
        }
        ArrayList<SyntaxObject> list = syntaxSequence.get(sequenceIndex);
        if (list.isEmpty()) {
            return "nonsense";
        }
        return list.get(0).getClass().getName();
    }
    
    boolean makesSense() {
        return makesSense;
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("//// CONTEXT\n");
        
        if (!makesSense) {
            str.append("  Doesn't make sense!\n");
            str.append("  ").append(nonsenseReason);
            return str.toString();
        }
        
        for (int i = 0; i < size(); i++) {
            str.append("  ").append(Integer.toString(i)).append(" - ");
            str.append(getClassNameOf(i)).append("\n");
            ListIterator<SyntaxObject> iter = listIterator(i);
            while (iter.hasNext()) {
                SyntaxObject obj = iter.next();
                str.append("    ").append(obj.toString()).append("\n");
            }
        }
        
        return str.toString();
    }
    
    static PromptContext createContext(StringCaretPair sterileInput, int[] workingIndices) {
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
        
        // We are not wasting our computation time wondering what the player was
        // currently typing. We are only concerned with the fully-complete
        // words already typed, or halting when the input so far makes no sense.
        // For this reason, we are cleaving off the word which contains the
        // player's input caret.
        String relevantPart = sterileInput.str
                .substring(0, workingIndices[0]).trim();
        
        //System.out.println("Relevant part: " + relevantPart);
        
        TreeSet<VocabularyWord> vocab = new TreeSet<>();
        world.loadRelevantVocabulary(vocab);
        ArrayList<ArrayList<VocabularyWord>> referenceSequence = new ArrayList<>();
        
        while (relevantPart.length() > 0) {
            boolean foundMatch = false;
            
            ArrayList<VocabularyWord> references = new ArrayList<>();
            referenceSequence.add(references);
            Iterator<VocabularyWord> iter = vocab.iterator();
            int longestLength = vocab.first().str.length();
            
            //System.out.println("Matching |" + relevantPart + "|");
            //System.out.println("Longest length is " + Integer.toString(longestLength));
            
            // Find matches
            while (iter.hasNext()) {
                VocabularyWord word = iter.next();
                int wordLength = word.str.length();
                //System.out.println("Checking " + word.str);
                
                if (wordLength > relevantPart.length()) {
                    // This word is too long; it won't match.
                    continue;
                }
                
                if (foundMatch && wordLength < longestLength) {
                    // If we found a match before, so we cannot match shorter
                    // words, because they will always be less of a match.
                    break;
                }
                
                if (relevantPart.startsWith(word.str)) {
                    foundMatch = true;
                    references.add(word);
                    longestLength = wordLength;
                    //System.out.println("Adjusting longest length to " + Integer.toString(longestLength));
                    //System.out.println("Matched!");
                }
                
                if (!foundMatch) {
                    // If we have not yet found a match, we can keep adjusting
                    // the last longest length we were comparing with.
                    longestLength = wordLength;
                    //System.out.println("Adjusting longest length to " + Integer.toString(longestLength));
                }
            }
            
            if (!foundMatch) {
                // We have lost our way lol.
                // We could not match anything with what the player wrote.
                context.makesSense = false;
                context.nonsenseReason = "No vocabulary match found";
                return context;
            }
            
            // Crop what we've matched
            relevantPart = relevantPart.substring(longestLength);
            
            // If we actually did this right, the new relevantPart should
            // start with a space (or be empty), because we should be
            // matching whole words. If this check fails, then the player
            // wrote a longer word than anything we could match with.
            if (relevantPart.length() > 0) {
                if (relevantPart.charAt(0) != ' ') {
                    // Aha! We have cropped off the beginning of a long word
                    // that we do not recognize. We are lost.
                    context.makesSense = false;
                    context.nonsenseReason = "Failed to match whole word. Remainder: \"" + relevantPart + "\"";
                    return context;
                }
            }
            
            // With our check for success, we know we can continue with the
            // rest of the input.
            relevantPart = relevantPart.trim();
        }
        
        // Make sure our first word is a verb, and following words are not
        ListIterator<ArrayList<VocabularyWord>> sequenceIter =
                referenceSequence.listIterator();
        boolean requiringVerb = true;
        
        while (sequenceIter.hasNext()) {
            ArrayList<VocabularyWord> sequenceList = sequenceIter.next();
            ListIterator<VocabularyWord> listIter = sequenceList.listIterator();

            while (listIter.hasNext()) {
                VocabularyWord listItem = listIter.next();
                if ((listItem.referable instanceof Verb) != requiringVerb) {
                    // Remove a possible reference if it does not meet the
                    // part-of-speech requirement.
                    listIter.remove();
                }
            }

            // If the resulting list in the sequence is empty, then the
            // player did not start their input with the correct pare of speech,
            // and makes no sense as an input.
            if (sequenceList.isEmpty()) {
                context.makesSense = false;
                context.nonsenseReason = "Incorrect part of speech";
                return context;
            }
            
            // Only the first word in the input is a verb
            requiringVerb = false;
        }
        
        // IF WE HAVE MADE IT THIS FAR, then we can confirm that we have matched
        // a verb AT THE LEAST. So, at this point, we should identify the
        // prepositions being used, and then narrow down the verb from there.
        ArrayList<SyntaxObject> syntaxList = new ArrayList<>();
        
        //TODO: Implement proper prepositions
        
        // Load verbs
        ListIterator<VocabularyWord> verbIter = referenceSequence.get(0).listIterator();
        while (verbIter.hasNext()) {
            syntaxList.add(verbIter.next());
        }
        context.syntaxSequence.add(syntaxList);
        
        // TODO: Handle nouns that have a preposition in their adjectives
        //       "Angel with large wings"
        // For now, we are assuming no nouns are described with prepositions
        
        // Build noun profile clusters, so we don't suggest descriptors that
        // have already been typed in before.
        // Separate these clusters by non-noun-related words.
        // Since English is an adjective-noun-ordered language, we will assume
        // that when a noun occurs, that it marks the end of the cluster.
        int currentClusterIndex = 0;
        int lastClusterIndex = -1;
        int currentStreakIndex = 0;
        sequenceIter = referenceSequence.listIterator(1);
        ArrayList<NounProfile> profilesInCluster = new ArrayList<>();
        
        while (sequenceIter.hasNext()) {
            ArrayList<VocabularyWord> sequenceList = sequenceIter.next();
            
            // Check for prepositions
            ListIterator<VocabularyWord> listIter = sequenceList.listIterator();
            boolean prepositionFound = false;
            boolean actualNounFound = false;
            while (listIter.hasNext()) {
                VocabularyWord word = listIter.next();
                if (!word.isNoun()) {
                    // End the last noun cluster
                    currentClusterIndex = lastClusterIndex + 1;
                    //System.out.println("Prep > Last: " + Integer.toString(lastClusterIndex) + " ; Current: " + Integer.toString(currentClusterIndex));
                    prepositionFound = true;
                    break;
                }
            }
            
            // Create a new list to fill in the syntax sequence
            //System.out.println("Last: " + Integer.toString(lastClusterIndex) + " ; Current: " + Integer.toString(currentClusterIndex));
            if (currentClusterIndex > lastClusterIndex) {
                //System.out.println("New list");
                lastClusterIndex = currentClusterIndex;
                currentStreakIndex = 0;
                if (!syntaxList.isEmpty()) {
                    syntaxList = new ArrayList<>();
                    context.syntaxSequence.add(syntaxList);
                    profilesInCluster.clear();
                    //System.out.println("List added");
                }
            }
            
            listIter = sequenceList.listIterator();
            if (prepositionFound) {
                // Filter out non-prepositions, if found
                while (listIter.hasNext()) {
                    VocabularyWord word = listIter.next();
                    if (word.isNoun()) {
                        listIter.remove();
                    }
                    else if (syntaxList.isEmpty()) {
                        // Add prepositions to the list.
                        // We'll be using a special algorithm later if we're
                        // adding nouns and their adjectives.
                        syntaxList.add(word);
                    }
                    else {
                        context.makesSense = false;
                        context.nonsenseReason = "Cannot have multiple matches for preposition"
                                + word.str + "\"!";
                        throw new RuntimeException(
                                "We have multiple matches for preposition \""
                                + word.str + "\"!"
                        );
                    }
                }
            }
            else {
                // Gather noun clusters, if no prepositions were found here
                while (listIter.hasNext()) {
                    VocabularyWord word = listIter.next();
                    
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
                        
                        if (!syntaxList.contains(cluster)) {
                            syntaxList.add(cluster);
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
                    }
                    
                    //System.out.println("Marking cluster from: " + word.str);
                    //System.out.println(profile.getLastCluster().toString());
                    
                    if (word.nounProfile.actualNouns.contains(word)) {
                        // This is the actual noun, so we will end the noun
                        // cluster here.
                        //System.out.println("Found actual noun: " + word.str);
                        actualNounFound = true;
                    }
                }
            }
            
            if (actualNounFound) {
                currentClusterIndex = lastClusterIndex + 1;
                //System.out.println("Noun > Last: " + Integer.toString(lastClusterIndex) + " ; Current: " + Integer.toString(currentClusterIndex));
            }
            currentStreakIndex++;
        }
        
        // Now that the nouns are collected into clusters, we can clear out
        // the ones that have broken streaks, as the player probably was not
        // intending to type those.
        
        ListIterator<ArrayList<SyntaxObject>> synSeqIter = context.syntaxSequence.listIterator();
        while (synSeqIter.hasNext()) {
            syntaxList = synSeqIter.next();
            ListIterator<SyntaxObject> synListIter = syntaxList.listIterator();
            while (synListIter.hasNext()) {
                SyntaxObject synObj = synListIter.next();
                if (synObj instanceof NounProfileCluster) {
                    NounProfileCluster cluster = (NounProfileCluster)synObj;
                    if (!cluster.hasClusterStreak()) {
                        synListIter.remove();
                    }
                }
            }
            
            if (syntaxList.isEmpty()) {
                // We couldn't find a noun that is being consistently
                // referred to, so the input doesn't make sense.
                context.makesSense = false;
                context.nonsenseReason = "Failed to find noun with full streak";
                return context;
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
        syntaxList = context.syntaxSequence.get(context.syntaxSequence.size() - 1);
        ListIterator<SyntaxObject> lastIter = syntaxList.listIterator();
        while (lastIter.hasNext()) {
            SyntaxObject synObj = lastIter.next();
            if (synObj instanceof NounProfileCluster) {
                suggestions.addAll(((NounProfileCluster)synObj).unmentioned);
            }
            else {
                suggestions.addAll(nounStarters);
                break;
            }
        }
        
        context.suggestions = suggestions.toArray(new VocabularyWord[suggestions.size()]);
        
        return context;
    }
}
