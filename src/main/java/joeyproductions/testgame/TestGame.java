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
package joeyproductions.testgame;

import joeyproductions.jessadventurecore.ui.JessAdventureCore;
import joeyproductions.jessadventurecore.world.Noun;
import joeyproductions.jessadventurecore.world.Verb;
import joeyproductions.jessadventurecore.world.World;

/**
 * This is just a test game to experiment with the adventure system.
 * @author Joseph Cramsey
 */
public class TestGame {
    
    public static JessAdventureCore ADV;
    
    public static void main(String[] args) {
        World testWorld = World.createWorld();
        
        testWorld.verbs.add(new Verb(
                "examine", new String[] {
                    "look at",
                    "inspect"
                }, "x", testWorld
        ));
        testWorld.verbs.add(new Verb("take", testWorld));
        
        testWorld.nouns.add(new Noun(
                "bucket", testWorld, "pale", "red", "sandy", "plastic"
        ));
        testWorld.nouns.add(new Noun(
                "bucket", testWorld, "blue", "clean", "plastic"
        ));
        testWorld.nouns.add(new Noun(
                "candy", testWorld, "red"
        ));
        testWorld.nouns.add(new Noun(
                "bucket", testWorld, "small", "pale", "red", "plastic"
        ));
        
        JessAdventureCore.initAdventure("Test Game", "Joseph Cramsey", testWorld);
        ADV = JessAdventureCore.CORE;
    }
}
