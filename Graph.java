import java.util.*;

/*
  A Graph class.

  Solves the perennial trigger problem by using the predictive measure SPREAD
  to return an optimal 1-itemset order for FP-Growth, after being initialized
  and fed in an arbitraty data set.

  Justification for creating an entire class:
  - We would prefer to encapsulate the Graph internals (Nodes & Edges)
  - We might want to fine tune the algorthim by trying different data
    strucutures to represent these internals.
    - Computations and manipulations are perfomed in-memory, so poorly chosen
      data strucutres may be bottleneck(s).

*/
public class Graph {

  // Represents a 1-itemset.
  private class Node {

    // The representation of the node in the dataset
    private String m_element;

    // A running counter for the Node's cardinality
    private int m_cardinality;

  }

  /*
    Represents the frequency of 2-itemsets containing the respective
    Nodes.
  */
  private class Edge {

    // The Nodes involved
    private Node[] m_nodes;

    /*
      A running counter for the Edge's cardinality. We can immediately extract
      frequent 2-itemsets by inspecting this value.
    */
    private int m_cardinality;

  }

  // A data structure holding Nodes extracted from the dataset

  // A data structure holding Edges extracted from the dataset

  /*
    Determines the SPREAD of a particular Node.

    Concepts:

    1) Possible 3-itemsets involving Node:

        Pick two Nodes at the end of frequent edges, & check if there is a frequent edge
        between the two Nodes.

    2) Probability Node is a 3-itemset "pivot":

        The chance that the Node serves as a joining itemset between two Nodes it has
        frequent Edges with. We want to estimate the probability that we have:

          A B C

        And not:

          A B
          A C

        WITHOUT having to scan the transaction database again.
  */
  private double computeSpread(Node n) {

    // A: Determine the possible 3-itemsets involving the Node (see above)

    // B: Compute the probability that a Node is a 3-itemset "pivot" (see above)

    // Return a combined measure (for now this is simply A * B)

  }

  // Return an optimally ordered ArrayList of the 1-itemsets, using SPREAD.
  public ArrayList<String> computeOrder() {
    // Sort Nodes by "spread factor"

      /*
        Break ties with total # of frequent Edges involving a Node =>then
        with Node cardinality => then just leave as is.
      */

    // Return result of sort as just String values
  }
}



// The following should be moved to the FPTree.java

  // Initialize an instance of the Graph class with an input dataset

  // Call on the Graph class to return an optimal order for FP-Growth.

  // Clean up the Graph class

  // Use FP Growth normally, with the returned order ...
