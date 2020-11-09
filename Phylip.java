/*
 * Phylip.java
 *
 * Version:
 *     $1.0$
 *
 * Revisions:
 *     $Log$
 */

/**
 * This program will call PHYLIP on the server to generate a neighbor-joining tree in 
 Newick format and performs a bootstrap operation for a user-determined number of 
 simulations, and report the confidence in each branch position
 *
 *  
 * @author      Sheethal Umesh Nagalakshmi
 */
 
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

public class Phylip {

    public static void main(String[] args) throws Exception {

        Scanner input = new Scanner(System.in);

        // Take input from user
        System.out.println("Enter the name of the file: ");
        String fileName = input.nextLine();
        // Take input from user
        System.out.println("Enter the number of iterations: ");
        Integer numberOfIterations = input.nextInt();

        long startimeNanos = System.nanoTime();

        // Copy input file to infile
        long copyFileStartTime = System.nanoTime();
        readInputFile(fileName);
        long copyFileEndTime = System.nanoTime();

        // Create MSASequence object using the infile
        List<String> sequences = getSequences();
        MSASequence msaSequence = new MSASequence(sequences);

        // Running pbylip for the first time
        long RunphylipStartTime = System.nanoTime();
        runPhylip();
        long RunphylipEndTime = System.nanoTime();

        // Creating keys by reading original Newick Tree
        Map<String, Integer> confidenceMap = new HashMap<>();
        String originalTree = storeOriginalTree(confidenceMap);

        long BootstrapStartTime = System.nanoTime();

        // Bootstrap operations
        for (int i = 0; i < numberOfIterations - 1; i++) {

            // Generate new sequences using random generator
            MSASequence newMSASequence = generateNewSample(msaSequence);

            // Generate MSA File to run phylip
            generateNewMSAFile(newMSASequence);

            // Run Phylipfor every iteration
            runPhylip();

            // Update Map with new outtree
            updateTree(confidenceMap);
        }


        long BootstrapEndTime = System.nanoTime();

        Confidence confidence = getConfidence(confidenceMap, originalTree, 
        numberOfIterations);
        System.out.println("Confidence Tree: " + confidence.confidenceTree);
        System.out.println("Consensus Tree: " + confidence.consensusTree);
        System.out.println("Time taken to copy input file to infile =  " + 
        (copyFileEndTime - copyFileStartTime) + "ns");
        System.out.println("Time taken to run initial MSA Sequence =  " + 
        (RunphylipEndTime - RunphylipStartTime) + "ns");
        System.out.println("Time taken for bootstrap operation =  " + 
        (BootstrapEndTime - BootstrapStartTime) + "ns");
        System.out.println("Total time taken for entire program =  " + 
        (System.nanoTime() - startimeNanos) + "ns");
    }

    // Generates new MSA file
    public static File generateNewMSAFile(MSASequence msaSequence) {
        File newMSAFile = new File("infile");
        try (FileWriter fileWriter = new FileWriter(newMSAFile.getName()); 
        PrintWriter printWriter = new PrintWriter(fileWriter)) {

            int numberOfSequences = msaSequence.sequenceNames.size();
            int lengthOfEachSequence = msaSequence.sequenceValues.get(0)
                    .length();
            printWriter.printf(" %d  %d", numberOfSequences, lengthOfEachSequence);
            printWriter.println();
            for (int i = 0; i < numberOfSequences; i++) {
                printWriter.print(msaSequence.sequenceNames.get(i));
                for (int j = 0; j < 50; j++) {
                    printWriter.print(msaSequence.sequenceValues.get(i)
                            .charAt(j));
                }
                printWriter.println();
            }

            printWriter.println();

            for (int k = 0; k < lengthOfEachSequence; k += 50) {
                for (int i = 0; i < numberOfSequences; i++) {
                    for (int j = 0; j < 50; j++) {
                        printWriter.print(msaSequence.sequenceValues.get(i)
                                .charAt(j));
                    }
                    printWriter.println();
                }
            }

        } catch (IOException e) {
            System.out.println("Unable to create a new MSA File");
        }

        return newMSAFile;
    }

    // Runs phylip tool to generate outtree

    public static void runPhylip() throws Exception {

        File argumentFile = new File("input");

        try (OutputStream outputStream = new FileOutputStream(argumentFile)) {
            outputStream.write("Y".getBytes());
            outputStream.flush();
        }

        String[] command = new String[]{"/usr/local/bin/phylip/dnadist", "-i", "infile"};
        Process process = new ProcessBuilder().command(command)
                .redirectInput(new File("input"))
                .redirectOutput(new File("screenout"))
                .start();
        process.waitFor();

        process = Runtime.getRuntime()
                .exec("rm newOutFile");
        process.waitFor();
        process = Runtime.getRuntime()
                .exec("rm newOutTree");
        process.waitFor();
        process = Runtime.getRuntime()
                .exec("cp outfile infile");
        process.waitFor();

        process = Runtime.getRuntime()
                .exec("rm outfile");

        command = new String[]{"/usr/local/bin/phylip/neighbor", "-i", "infile"};
        process = new ProcessBuilder().command(command)
                .redirectInput(new File("input"))
                .redirectOutput(new File("screenout"))
                .start();
        // Waits for the previous command to finish running before the next command 
        // executes
        process.waitFor();

        process = Runtime.getRuntime()
                .exec("rm infile");
        process.waitFor();

        process = Runtime.getRuntime()
                .exec("cp outfile newOutFile");
        process.waitFor();

        process = Runtime.getRuntime()
                .exec("cp outtree newOutTree");
        process.waitFor();

        process = Runtime.getRuntime()
                .exec("rm infile");
        process.waitFor();
        Runtime.getRuntime()
                .exec("rm outfile");
        process.waitFor();
        process = Runtime.getRuntime()
                .exec("rm outtree");

        process.waitFor();
        process = Runtime.getRuntime()
                .exec("rm screenout");
        process.waitFor();
        process = Runtime.getRuntime()
                .exec("rm input");
        process.waitFor();
    }

    //  Stores the frequency of keys in outtree
    public static String storeOriginalTree(Map<String, Integer> confidenceMap) throws Exception {
        File file = new File("newOutTree");
        Scanner fileInput = new Scanner(file);

        StringBuilder stringBuilder = new StringBuilder();

        while (fileInput.hasNextLine()) {
            stringBuilder.append(fileInput.nextLine());
        }

        String outTree = stringBuilder.toString();
        System.out.println("Initial tree: ");
        System.out.println(outTree);
        System.out.println();

        Stack<String> stack = new Stack<>();
        String key = "";
        for (int i = 0; i < outTree.length(); i++) {
            char currentChar = outTree.charAt(i);
            if (currentChar == ')') {
                String popChar;
                StringBuilder keyBuilder = new StringBuilder(")");
                while (!(popChar = stack.pop()).equals("(")) {
                    keyBuilder.insert(0, popChar);
                }
                keyBuilder.insert(0, popChar);
                key = keyBuilder.toString();
                Integer frequency = confidenceMap.getOrDefault(key, 0);
                frequency++;
                confidenceMap.put(key, frequency);
                stack.push(key);
            } else if (currentChar == ':') {
                while (currentChar != ',' && currentChar != ')') {
                    currentChar = outTree.charAt(i++);
                }

                if (currentChar == ',') {
                    stack.push(currentChar + "");
                } else {
                    i--;
                }
                i--;
            } else {
                stack.push(currentChar + "");
            }
        }

        return key;
    }

    // Reads input file and store it's contents in a new file called infile

    public static void readInputFile(String fileName) {
        long databasefilereadStart = System.nanoTime();
        File file = new File(fileName);
        String sb = "";
        try (FileWriter dataWriter = new FileWriter("infile")) {
            Scanner data = new Scanner(file);
            while (data.hasNextLine()) {
                String currentLine = data.nextLine();
                sb = sb + currentLine;
            }

            dataWriter.write(sb.toString());
            dataWriter.flush();

        } catch (IOException e) {
            System.out.println("Unable to read input file");
        }
        long databasefilereadEnd = System.nanoTime();
        System.out.println("Time taken to read input file = " + 
        (databasefilereadEnd-databasefilereadStart) + "ns");
    }

    // Updates the map with new outtree
    public static void updateTree(Map<String, Integer> confidenceMap) throws IOException {
        long generateOuttreeStartTime = System.nanoTime();
        File file = new File("newOutTree");
        Scanner fileInput = new Scanner(file);

        StringBuilder stringBuilder = new StringBuilder();

        while (fileInput.hasNextLine()) {
            stringBuilder.append(fileInput.nextLine());
        }

        String outTree = stringBuilder.toString();

        Stack<String> stack = new Stack<>();

        for (int i = 0; i < outTree.length(); i++) {
            char currentChar = outTree.charAt(i);
            if (currentChar == ')') {
                String popChar;
                StringBuilder keyBuilder = new StringBuilder(")");
                while (!(popChar = stack.pop()).equals("(")) {
                    keyBuilder.insert(0, popChar);
                }
                keyBuilder.insert(0, popChar);
                String key = keyBuilder.toString();
                if (confidenceMap.containsKey(key)) {
                    Integer frequency = confidenceMap.get(key);
                    frequency++;
                    confidenceMap.put(key, frequency);
                }
                stack.push(key);
            } else if (currentChar == ':') {
                while (currentChar != ',' && currentChar != ')') {
                    currentChar = outTree.charAt(i++);
                }

                if (currentChar == ',') {
                    stack.push(currentChar + "");
                } else {
                    i--;
                }
                i--;
            } else {
                stack.push(currentChar + "");
            }
        }
        long generateOuttreeEndTime = System.nanoTime();
    }


    // Generates the newick tree with confidence values
    public static Confidence getConfidence(Map<String, Integer> confidenceMap, 
    String originalTree, int numberOfIterations) {
        long confidencevalueStartTime = System.nanoTime();
        Stack<String> stack = new Stack<>();

        Stack<String> confidenceStack = new Stack<>();
        Stack<String> consensusStack = new Stack<>();
        for (int i = 0; i < originalTree.length(); i++) {
            char currentChar = originalTree.charAt(i);
            if (currentChar == ')') {
                String popChar;
                StringBuilder keyBuilder = new StringBuilder(")");
                StringBuilder confidenceKeyBuilder = new StringBuilder(")");
                StringBuilder consensusKeyBuilder = new StringBuilder(")");
                while (!(popChar = stack.pop()).equals("(")) {
                    keyBuilder.insert(0, popChar);
                    confidenceKeyBuilder.insert(0, confidenceStack.pop());
                    consensusKeyBuilder.insert(0, consensusStack.pop());
                }
                confidenceKeyBuilder.insert(0, confidenceStack.pop());
                consensusKeyBuilder.insert(0, consensusStack.pop());
                keyBuilder.insert(0, popChar);
                String key = keyBuilder.toString();
                String confidenceTreeKey = confidenceKeyBuilder.toString();
                String consensusTreeKey = consensusKeyBuilder.toString();

                Integer frequency = confidenceMap.get(key);
                stack.push(key);
                confidenceStack.push(confidenceTreeKey + ":" + 
                ((double)frequency/numberOfIterations));
                consensusStack.push(consensusTreeKey + ":" + frequency);
            } else {
                String currentString = currentChar + "";
                stack.push(currentString);
                confidenceStack.push(currentString);
                consensusStack.push(currentString);
            }
        }

        Confidence confidence = new Confidence(confidenceStack.pop(), 
        consensusStack.pop());
        long confidencevalueEndTime = System.nanoTime();
        System.out.println("Time taken to generate confidence values =  " + 
        (confidencevalueEndTime - confidencevalueStartTime) + "ns");
        System.out.println();
        return confidence;
    }

    // Generates new MSA sequences

    public static MSASequence generateNewSample(MSASequence msaSequence) {

        List<String> sequenceValues = msaSequence.sequenceValues;
        List<String> newSequenceValues = new ArrayList<>();
        int length = sequenceValues.get(0).length();
        int randomInitialIndex = getRandomRange(0, length/2);
        int randomMidIndex = getRandomRange(length/2, length - 1);

        for (String sequenceValue : sequenceValues) {
            String part1 = sequenceValue.substring(0, randomInitialIndex);
            String part2 = sequenceValue.substring(randomInitialIndex, randomMidIndex);
            String part3 = sequenceValue.substring(randomMidIndex);
            String updatedSequenceValue = part2 + part3 + part1;
            newSequenceValues.add(updatedSequenceValue);
        }

        MSASequence newMSASequence = new MSASequence(msaSequence.sequenceNames, 
        newSequenceValues);
        return newMSASequence;
    }

    // Generates a random number for performing bootstrap operation
    private static int getRandomRange(int min, int max) {

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }


    public static List<String> getSequences() throws Exception {

        Scanner fileInput = new Scanner(new File("infile"));

        int numberOfSequences = 0;
        while (fileInput.hasNextLine()) {
            String currentLine = fileInput.nextLine()
                    .trim();

            if (currentLine.isEmpty()) {
                continue;
            }

            String[] seqMetadata = currentLine.split(" ");
            numberOfSequences = Integer.parseInt(seqMetadata[0]);
            break;
        }

        List<String> sequences = new ArrayList<>(Collections.nCopies(numberOfSequences, ""));

        while (fileInput.hasNextLine()) {
            String currentLine = fileInput.nextLine()
                    .trim();
            if (currentLine.isEmpty()) {
                continue;
            }

            for (int i = 0; i < numberOfSequences; i++) {

                sequences.set(i, (sequences.get(i) + currentLine));
                if (fileInput.hasNextLine()) {
                    currentLine = fileInput.nextLine()
                            .trim();
                    if (currentLine.isEmpty()) {
                        break;
                    }
                }
            }
        }

        return sequences;
    }
}
