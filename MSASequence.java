/*
 * MSASequence.java
 *
 * Version:
 *     $1.0$
 *
 * Revisions:
 *     $Log$
 */

/**
 * This program will store sequences and sequence names
 *
 *  
 * @author      Sheethal Umesh Nagalakshmi
 */

import java.util.ArrayList;
import java.util.List;

public class MSASequence {
    List<String> sequenceNames;
    List<String> sequenceValues;

    public MSASequence(List<String> sequences) {
        this.sequenceNames = new ArrayList<>();
        this.sequenceValues = new ArrayList<>();
        sequences.forEach(sequence -> {
            this.sequenceNames.add(sequence.substring(0, 10));
            this.sequenceValues.add(sequence.substring(10));
        });
    }

    public MSASequence(List<String> sequenceNames, List<String> sequenceValues) {
        this.sequenceNames = sequenceNames;
        this.sequenceValues = sequenceValues;
    }
}

