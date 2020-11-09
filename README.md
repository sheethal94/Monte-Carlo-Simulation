# Monte Carlo Simulation

Program will call PHYLIP on the server to generate a neighbor-joining tree in 
Newick format and performs a bootstrap operation for a user-determined number of 
simulations, and report the confidence in each branch position.

## Usage 

### Input

MSA file

### Output

Confidence tree and consensus tree

### Command to run this program 
javac Phylip.java

java Phylip

#### Note
MSASequence.java and Confidence.java should be placed under the same directory where this program 
is being run.
