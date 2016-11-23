/* This code is provided as part of UCLA CS145 Project 1A: FP-Tree Optimization.
 * Please contact TA Ariyam Das for any discrepancies. 
 *
 * 
 * Implementation based on the paper: 
 * Han J, Pei J, Yin Y (2000) 
 * Mining frequent patterns without candidate generation. 
 * In: Proceeding of the 2000 ACM-SIGMOD, Dallas, TX
 * 
 * Description:
 * - 	Constructs a FP-tree by scanning the 
 * 		transaction database (provided as an input file) twice.
 *  
 * -	Some additional attributes are added to this FP-tree 
 * 		(which are not present in the original paper) to aid the FP-growth method.  
 * 
 */

import java.io.*;
import java.util.*;


public class FPTree {
	private ArrayList<FPTreeHeaderElement> header_table;
	private FPTreeNode fptree_root;
	private int support_threshold;
	
	//performance measurement counters
	static int fptree_construction_calls = 0;		//FP-tree construction from file
	static int cond_fptree_construction_calls = 0;	//conditional FP-tree constructions
	static int fptree_mining_calls = 0;				//FP-tree mining call
	static int single_path_mining_calls = 0;		//direct mining from single paths
	static int num_of_single_paths = 0;				//number of single paths encountered
	
	public FPTree() 
	{
		header_table = new ArrayList<FPTreeHeaderElement>();
		fptree_root = new FPTreeNode();
		support_threshold = 0;
	}
	
	//This is to create the FPTree from an input file containing all the transactions
	public FPTree(File inputfile, int support)
	{
		//initializations
		this.header_table = new ArrayList<FPTreeHeaderElement>();
		this.fptree_root = new FPTreeNode();
		this.support_threshold = support;
		
		constructFPTree(inputfile);	//from all transactions in the input file
	}
	
	//This is to create the FPTree from an input file with all transactions starting at line no [start_at] and ending at line no [end_at]
	public FPTree(File inputfile, int support, int start_at, int end_at)
	{
		//initializations
		this.header_table = new ArrayList<FPTreeHeaderElement>();
		this.fptree_root = new FPTreeNode();
		this.support_threshold = support;
			
		constructFPTree(inputfile, start_at, end_at);	//from specific range of transactions in the input file
	}
	
	//This is to create the FPTree from a conditional pattern base
	public FPTree(ArrayList<String> cond_pattern_base, int support)
	{
		//initializations
		this.header_table = new ArrayList<FPTreeHeaderElement>();
		this.fptree_root = new FPTreeNode();
		this.support_threshold = support;
		
		constructFPTree(cond_pattern_base);
	}

	// Use Pairwise Item Lift (pil)
	public FPTree(File inputfile, int support, boolean usePil) {

		//initializations
		this.header_table = new ArrayList<FPTreeHeaderElement>();
		this.fptree_root = new FPTreeNode();
		this.support_threshold = support;
		
		// try {
		constructFPTreeUsingPil(inputfile);
		// } catch (Exception e) {
			// System.out.println("Error! " + e.toString());
		// }
	}
	
	public FPTreeHeaderElement getHeaderElement(String item) 
	{
		FPTreeHeaderElement ret = null;
		for(int i=0; i<header_table.size(); i++)
			if(header_table.get(i).getItem().equals(item))
			{
				ret = header_table.get(i);
				break;
			}
		return ret;
	}
	
	public FPTreeHeaderElement getHeaderElement(int i) 
	{
		FPTreeHeaderElement ret = null;
		if(header_table.size()>i)
			ret = header_table.get(i);
		return ret;
	}
	
	public ArrayList<FPTreeHeaderElement> getFPTreeHeaderTable()
	{
		return this.header_table;
	}
	
	public FPTreeNode getRoot() 
	{
		return this.fptree_root;
	}
	
	public int getSupportThreshold()
	{
		return this.support_threshold;
	}
	
	//traverses the pointers starting from the node link of the header element to reach the last node containing the item
	public FPTreeNode getLastFPTreeNode(String item)
	{
		FPTreeNode fptn = null;
		FPTreeHeaderElement elem = getHeaderElement(item);
		if(elem!=null)
			fptn = elem.getNodeLink();
		if(fptn!=null)
			while(fptn.getNextNode()!=null)
				fptn = fptn.getNextNode();
							
		return fptn;
	}
	
	/*
	1. Compute pairwise item lift in first scan of the database along with support of the items.
	2. Greedily select a pair of items with the highest pairwise item lift (the item with higher
	support appears first in the order). Add this ordered pair to the F-list. Use total support
	of the paired items to break ties, if any.
	3. Iterate through the remaining frequent items and greedily add an item i to the F-list, such
	that the sum of paiwise item lifts between i and the elements in the F-list is maximized.
	
	*/
	public void constructFPTreeUsingPil(File inputfile) {
		Hashtable<String,Integer> itemsFrequencyTable = new Hashtable<String, Integer>();
		Hashtable<Set<String>,Integer> itemsTwoFreqTable = new Hashtable<Set<String>, Integer>();
		List<Set<String>> pairs = new ArrayList<Set<String>>();
		int n = 0;
		try {
			n = populateAll(inputfile, itemsFrequencyTable, itemsTwoFreqTable, pairs); //populate freq hash table
		} catch(IOException ioe) {
			System.out.println("Error! Populating freq hash failed!");
			System.out.println(ioe.toString());
		}
		//System.out.println(itemsFrequencyTable);
		//System.out.println(itemsTwoFreqTable);
		final int nValue = n;
		System.out.println(pairs);
		Set<String> keys = itemsFrequencyTable.keySet();	// get all items in the transaction database

		// List<Set<String>> pairs = generateAllPairs(keys);	// generate all items of length 2
  
		Hashtable<Set<String>,Double> pilTable = new Hashtable<Set<String>, Double>();
        // System.out.println(pairs);

		pairs.forEach((pair) -> {
        	// Get i and j
			List<String> list = new ArrayList<String>(pair);
			String i = list.get(0);
			String j = list.get(1);

			// System.out.println(pair);
			// System.out.println(itemsTwoFreqTable.get(pair));
			// System.out.println(itemsFrequencyTable.get(i));
			// System.out.println(itemsFrequencyTable.get(j));
			// System.out.println(nValue);

			if (!itemsTwoFreqTable.containsKey(pair)) {	// if sup(ij) == 0;
				pilTable.put(pair, (double)0);
			} else {	// use traditional formula pil(i,j) = (sup(ij) / (sup(i) * sup(j))) * N where N is the total number of transactions in db
				// System.out.println(itemsTwoFreqTable.get(pair));
				double pilValue = ((double)itemsTwoFreqTable.get(pair) / ((double)itemsFrequencyTable.get(i) * (double)itemsFrequencyTable.get(j))) * (double)nValue;
				pilTable.put(pair, pilValue);
			}
		});

		// System.out.println(itemsTwoFreqTable);
		// System.out.println(pilTable);

		// TODO: more efficient way to this?
		// Find highest PIL item - breaking ties with sup if necessary
		// int totsup = -1;
		double maxpil = -1;
		Set<String> highestPilPair = null;
		for (Set<String> pair : pilTable.keySet()) {
			double pil = pilTable.get(pair);
			if (pil > maxpil) {	// found new higest pil value
				maxpil = pil;
				highestPilPair = pair;
			} else if (pil == maxpil) {	// tied; break with sup(ij)
				if (itemsTwoFreqTable.get(highestPilPair) < itemsTwoFreqTable.get(pair)) {	// if current sup(ij) > tied sup(ij)
					maxpil = pil;
					highestPilPair = pair;
				}
			}
		}

		// System.out.println("Highest Pil Pair: " + highestPilPair);
		// System.out.println(maxpil);
		// System.out.println(itemsTwoFreqTable.get(highestPilPair));

		List<String> tmpList = new ArrayList<String>(highestPilPair);
		String a = tmpList.get(0);
		String b = tmpList.get(1);

		Hashtable<String,Integer> fTable = new Hashtable<String, Integer>();
		fTable.put(a,itemsFrequencyTable.get(a));
		fTable.put(b,itemsFrequencyTable.get(b));

		// keys.remove(a)
		List<String> keyList = new ArrayList<>();
		keyList.addAll(keys);
		keyList.remove(a);
		keyList.remove(b);	// remove i,j as they've both been already added to the freq list

		Iterator<String> iter = keyList.iterator();
		// Filter out infrequent items amongst candidates
		while (iter.hasNext()) {
		    String k = iter.next();
		    if (itemsFrequencyTable.get(k) < support_threshold)
		        iter.remove();
		}

		// System.out.println("Remaining frequent items left: " + keyList);

		List<String> pilBasedFList = new ArrayList<>();
		// Must order a and b based on their respective support and break ties by alphabetical order
		int aSupp = itemsFrequencyTable.get(a);
		int bSupp = itemsFrequencyTable.get(b);
		if (aSupp > bSupp) {
			pilBasedFList.add(a);
			pilBasedFList.add(b);
		} else if (aSupp < bSupp) {
			pilBasedFList.add(b);
			pilBasedFList.add(a);
		} else if (a.charAt(0) < b.charAt(0)) { // aSupp == bSupp, break ties alphabetically
			pilBasedFList.add(a);
			pilBasedFList.add(b);
		} else {
			pilBasedFList.add(b);
			pilBasedFList.add(a);
		}
		// TODO: what's the guarantee that that i and j are frequent items in the first place? i.e. should be added pilBasedFList
		// System.out.println(pilTable);
		while (!keyList.isEmpty()) {	// add all keys to the fList
			double maxPilSum = -1;
			String candidate = null;
			for (String i : keyList) {
				// System.out.println("i:" + i);
				double pilSum = 0;
				Set<String> tmpSet = fTable.keySet();
				List<String> fList = new ArrayList<>();
				fList.addAll(tmpSet);
				// System.out.println(fList);
				for (String key : fList) {
					// System.out.println(key);
					Set<String> s = new HashSet<String>();
					s.add(i);
					s.add(key);
					if (pilTable.containsKey(s))
						pilSum += pilTable.get(s);
				}

				if (pilSum > maxPilSum) {
					maxPilSum = pilSum;
					candidate = i;
				}
			}
			// System.out.println("Candidate: " + candidate);
			// at this point, we have our candidate to add on to fTable
			if (candidate != null) {
				keyList.remove(candidate);
				fTable.put(candidate, itemsFrequencyTable.get(candidate));
				pilBasedFList.add(candidate);
			}
		}
		
		// System.out.println("The fList using PIL metric (highest to lowest): " + pilBasedFList);

        for (String itm : pilBasedFList) {
        	header_table.add(new FPTreeHeaderElement(itm));
        }	// FPtree header table formed
			
		// System.out.println(header_table);

		// first scan ends here

		try {
			secondScanUsingPil(inputfile, itemsFrequencyTable, pilBasedFList); //fp-tree will be created in the second scan
		} catch(IOException ioe) {
			System.out.println("Error! Second scan for FPTree construction failed !");
			System.out.println(ioe.toString());
		}

		fptree_construction_calls++;
	}

	/*
	 * Construct FP-Tree from all transactions in the file.
	 */
	public void constructFPTree(File inputfile) 
	{		
		Hashtable<String, Integer> items_frequency = new Hashtable<String, Integer>();
		
		try 
		{
			firstScan(inputfile, items_frequency); //fp-tree header table will be created in the first scan
		} 
		catch(IOException ioe) 
		{
			System.out.println("Error! First scan for FPTree construction failed !");
			System.out.println(ioe.toString());
		}

		try 
		{
			secondScan(inputfile, items_frequency); //fp-tree will be created in the second scan
		} 
		catch(IOException ioe) 
		{
			System.out.println("Error! Second scan for FPTree construction failed !");
			System.out.println(ioe.toString());
		}
		
		fptree_construction_calls++;
	}
	
	/*
	 * Construct FP-Tree from specific range of lines (transaction_ids) in the input file.
	 */
	public void constructFPTree(File inputfile, int start_at, int end_at) 
	{		
		Hashtable<String, Integer> items_frequency = new Hashtable<String, Integer>();
		
		try 
		{
			firstScan(inputfile, items_frequency, start_at, end_at); //fp-tree header table will be created in the first scan
		} 
		catch(IOException ioe) 
		{
			System.out.println("Error! First scan for FPTree construction failed !");
			System.out.println(ioe.toString());
		}
		
		try 
		{
			secondScan(inputfile, items_frequency, start_at, end_at); //fp-tree will be created in the second scan
		} 
		catch(IOException ioe) 
		{
			System.out.println("Error! Second scan for FPTree construction failed !");
			System.out.println(ioe.toString());
		}
		
		fptree_construction_calls++;
	}
	
	public void constructFPTree(ArrayList<String> cond_pattern_base) 
	{		
		Hashtable<String, Integer> items_frequency = new Hashtable<String, Integer>();
		
		firstScan(cond_pattern_base, items_frequency); 	//fp-tree header table will be created in the first scan
		secondScan(cond_pattern_base, items_frequency); //fp-tree will be created in the second scan
		
		cond_fptree_construction_calls++;
	}

	/*
	 * The following function parses the transaction, extracts the items from it, 
	 * and then updates the hash table with count.
	 */
	public Set<String> extractItems(String transaction, int count, Hashtable<String,Integer> items_frequency)
	{
		Set<String> localKeyset = new HashSet<String>();
		String []items = transaction.split("\\s+");	//scan individual items in the transaction
    	for(int i=0; i<items.length; i++) 
    	{ 
    		localKeyset.add(items[i]);
    		int freq = count;
			if(items_frequency.containsKey(items[i])) 			//if this item already encountered before
				freq = items_frequency.get(items[i]) + count; //increment frequency
			items_frequency.put(items[i],new Integer(freq)); //update item with new frequency
    	}
    	return localKeyset;
	}
	
	/*
	 * After the transactions are parsed and items are extracted, 
	 * we sort the items in descending order of frequencies 
	 * and create the FPTree header table.
	 */	
	public void createFPTreeHeaderTable(Hashtable<String,Integer> items_frequency)
	{
    	Enumeration<String> e = items_frequency.keys();  //list of all items
        ArrayList<ItemElement> aie = new ArrayList<ItemElement>();  //placeholder to sort the frequent items      
        while(e.hasMoreElements()) 
        {
        	String item = (String) e.nextElement();
        	int frequency = items_frequency.get(item);
        	if(frequency>=support_threshold)	//ignore items whose frequency is less than support
        		aie.add(new ItemElement(item,frequency)); 
        }        
        Collections.sort(aie);  //sorted properly, in descending order of frequencies
        for(int i=0; i<aie.size(); i++)
        {
        	String itm = aie.get(i).getItem();
        	header_table.add(new FPTreeHeaderElement(itm));
        } //FPTree header formed
	}
	
	/*
	 * Following function inserts a prefix into prefix tree/FPTree with the corresponding count.
	 */

	public void insertIntoFPTreeUsingPil(String prefix, int count, Hashtable<String,Integer> items_frequency, List<String> pilBasedFList) { 	// count = 1
		String []items = prefix.split("\\s+");	//scan individual items in the transaction/prefix
    	ArrayList<ItemElement> aie = new ArrayList<ItemElement>(); //placeholder for sorting the frequent 

    	for(int i=0; i<items.length; i++) { 
			if(pilBasedFList.contains(items[i])) 
				aie.add(new ItemElement(items[i],items_frequency.get(items[i]),pilBasedFList.indexOf(items[i])));
    	}
    	// System.out.println(aie);
    	// System.out.println("asdas---asdas");
    	Collections.sort(aie); 	//prefix is sorted, we will now add it to the FPTree 
    	// System.out.println(aie);

    	FPTreeNode tmp = fptree_root;	
    	for(int i=0; i<aie.size(); i++)	//adding the prefix in FPTree now
    	{
    		String f_item = aie.get(i).getItem();	//get the frequent item
    		tmp.incrementFrequency(count);	//increment support of parent
    		if(tmp.isChild(f_item)==false) //if child not present, add it & update the node links from the header table
    		{
    			FPTreeNode new_child = tmp.addChild(f_item,0);	//child added
    			FPTreeNode last_node = getLastFPTreeNode(f_item);
    			if(last_node==null)	//first time this item is encountered
    			{
    				FPTreeHeaderElement hdrelem = getHeaderElement(f_item);
    				if(hdrelem!=null)
    					hdrelem.setNodeLink(new_child);
    			}
    			else
    				last_node.setNextNode(new_child);	//pointers updated, new child pointed by next node pointer
    		}
    		tmp = tmp.getChild(f_item);       		
    	}
    	if(tmp.getItem().equals("root")==false)
    		tmp.incrementFrequency(count);	//increment frequency of the last child
	}

	public void insertIntoFPTree(String prefix, int count, Hashtable<String,Integer> items_frequency)
	{
		String []items = prefix.split("\\s+");	//scan individual items in the transaction/prefix
    	ArrayList<ItemElement> aie = new ArrayList<ItemElement>(); //placeholder for sorting the frequent items in a single transaction/prefix
    	for(int i=0; i<items.length; i++) 
    	{ 
    		int frequency = items_frequency.get(items[i]);
			if(frequency>=support_threshold) 
				aie.add(new ItemElement(items[i],frequency));
    	}
    	Collections.sort(aie); 	//prefix is sorted, we will now add it to the FPTree 
    	
    	FPTreeNode tmp = fptree_root;	
    	for(int i=0; i<aie.size(); i++)	//adding the prefix in FPTree now
    	{
    		String f_item = aie.get(i).getItem();	//get the frequent item
    		tmp.incrementFrequency(count);	//increment support of parent
    		if(tmp.isChild(f_item)==false) //if child not present, add it & update the node links from the header table
    		{
    			FPTreeNode new_child = tmp.addChild(f_item,0);	//child added
    			FPTreeNode last_node = getLastFPTreeNode(f_item);
    			if(last_node==null)	//first time this item is encountered
    			{
    				FPTreeHeaderElement hdrelem = getHeaderElement(f_item);
    				if(hdrelem!=null)
    					hdrelem.setNodeLink(new_child);
    			}
    			else
    				last_node.setNextNode(new_child);	//pointers updated, new child pointed by next node pointer
    		}
    		tmp = tmp.getChild(f_item);       		
    	}
    	if(tmp.getItem().equals("root")==false)
    		tmp.incrementFrequency(count);	//increment frequency of the last child
	}
	
	/* 	
	 * first scan of transactions (all in one input file)
	 * input file format specified below:
	 * each line is a transaction containing set of items, separated by white space
	 * no transaction id is provided 
	 * The FPTree header table will be created in this scan
	*/	
	public void firstScan(File inputfile, Hashtable<String,Integer> items_frequency) throws IOException
	{       
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
                
        while((transaction = br.readLine()) != null) //read input file (transaction database)
        	extractItems(transaction, 1, items_frequency);
        
        br.close(); //file reading complete
        
        createFPTreeHeaderTable(items_frequency);       
	}

	//overloaded method - file reading done between lines [start_at] and [end_at]
	public void firstScan(File inputfile, Hashtable<String,Integer> items_frequency, int start_at, int end_at) throws IOException
	{       
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
        int line_no = 0;
        
        while((transaction = br.readLine()) != null) //read input file (transaction database) from lines [start_at] & [end_at] (both inclusive)
        {
        	line_no++;
        	if(line_no < start_at)
        		continue;
        	else if(line_no <= end_at)
        		extractItems(transaction, 1, items_frequency);
        	else
        		break;       	
        }
        
        br.close(); //file reading complete
        
        createFPTreeHeaderTable(items_frequency);       
	}
	/*
	 * second scan of transactions will create the FPTree and update the pointers in the FPTree header table.
	 */
	public void secondScanUsingPil(File inputfile, Hashtable<String,Integer> items_frequency, List<String> pilBasedFList) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
                
        while((transaction = br.readLine()) != null) //read input file (transaction DB) again
        	insertIntoFPTreeUsingPil(transaction, 1, items_frequency, pilBasedFList);
        
        br.close(); //file reading complete once and for all
	}

	public void secondScan(File inputfile, Hashtable<String,Integer> items_frequency) throws IOException
	{       
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
                
        while((transaction = br.readLine()) != null) //read input file (transaction DB) again
        	insertIntoFPTree(transaction, 1, items_frequency);
        
        br.close(); //file reading complete once and for all
	}
	
	//overloaded method, reading specific range of lines from the input file.
	public void secondScan(File inputfile, Hashtable<String,Integer> items_frequency, int start_at, int end_at) throws IOException
	{       
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
        int line_no = 0;
        
        while((transaction = br.readLine()) != null) //read input file (transaction DB) again, only the specific range of lines
        {
        	line_no++;
        	if(line_no < start_at)
        		continue;
        	else if(line_no <= end_at)
        		insertIntoFPTree(transaction, 1, items_frequency);
        	else 
        		break;
        }
        
        br.close(); //file reading complete once and for all
	}
		
	/* 	
	 * first scan of transactions entered as prefixes (conditional patter base).
	 * each prefix is in the format <support>:<space separated item list>
	*/	
	public void firstScan(ArrayList<String> cond_pattrn_base, Hashtable<String,Integer> items_frequency)
	{       
		for(int n=0; n<cond_pattrn_base.size(); n++)
		{
			String prefx = cond_pattrn_base.get(n);
			String []tokens = prefx.split(":");
			
			if(tokens.length != 2)	continue;	//for empty prefix
			
			int count = Integer.parseInt(tokens[0]);	//frequency of occurrence of the prefix
			String pattern = tokens[1];					//get the actual pattern			
			extractItems(pattern, count, items_frequency);			
		}													//conditional pattern base completely traversed             
		createFPTreeHeaderTable(items_frequency);	//FPTree header table created
	}	

	/* second scan of transactions entered as conditional pattern base 
	 * will create the FPTree and update the pointers in the FPTree header table.
	 */
	public void secondScan(ArrayList<String> cond_pattrn_base, Hashtable<String,Integer> items_frequency)
	{       
		for(int n=0; n<cond_pattrn_base.size(); n++)
		{
			String prefx = cond_pattrn_base.get(n);
			String []tokens = prefx.split(":");			
			
			if(tokens.length != 2)	continue;		//for empty prefix
			
			int count = Integer.parseInt(tokens[0]);			//frequency of occurrence of the prefix
			String pattern = tokens[1];							//get the actual pattern				
			insertIntoFPTree(pattern, count, items_frequency);	//prefix inserted into FPTree
		}   
	}	
	
	//For Debugging => Pre-Order traversal of FPTree (node first and then all of its children)
	public void traverseFPTree(FPTreeNode node, int depth) 
	{
		// for(int i=0; i<depth; i++)	//tabs printed as per the depth
		// 	System.out.print("\t");
		// System.out.print("-" + node + "\n");	//print node : its support : no. of children
		for(int i=0; i<node.getChildrenCount(); i++)
		{
			FPTreeNode tmp = node.getChild(i);
			traverseFPTree(tmp, depth+1);	//recursively print its child
		}
	}
	
	//overloaded method
	public void traverseFPTree()
	{
		traverseFPTree(this.fptree_root,0);
	}
	
	//For Debugging => Single Linked-List traversal of all elements in the header table, one by one
	public void traverseFPTreeHeaderTable()
	{
		for(int i=0; i<header_table.size(); i++)
		{
			// System.out.print(header_table.get(i).getItem()+": "); //print item
			FPTreeNode tmp = header_table.get(i).getNodeLink();
			while(tmp!=null)
			{
				// System.out.print("-> "+tmp);
				tmp = tmp.getNextNode();
			}
			// System.out.print("\n");
		}	
	}
	
	//following function returns true, if a given node and its descendants form a single branch
	public boolean isSingleBranch(FPTreeNode fptn)
	{
		int child_cnt;
		while((child_cnt = fptn.getChildrenCount()) == 1)
			fptn = fptn.getChild(0); //get the first and only child
		if(child_cnt>0)		//if the last node has more than 1 children, then not a single path
			return false;
		else
			return true;	//if last node is a leaf, then this is a single path
	}
	
	/* 
	 * Generate all possible combinations as frequent items from a single path
	 * E.g. a:3 -> b:2 -> c:1 will yield a:3, b:2, c:1, ab:2, ac:1, bc:1, abc:1 (2^n -1 items for n item path)
	 */	
	public void genAllCombinations(FPTreeNode fptn, String curr)
	{
		String prev = curr;
		curr = prev + " " + fptn.getItem();
		// System.out.print(curr + " : " + fptn.getFrequency() + "\n");	//print combination with support
		if(fptn.getChildrenCount()==1)
		{
			genAllCombinations(fptn.getChild(0), prev);		//combinations not including the item at this index
			genAllCombinations(fptn.getChild(0), curr);		//combinations including the item at this index
		}
		
		single_path_mining_calls++;
	}
	
	/*
	 * Trace the prefix from its immediate parent to the root (containing all the items separated by space).
	 * Also include its support. The returned string will be in the format <support>:<prefix>
	 */
	public String getPrefixEndingWith(FPTreeNode fptn)
	{
		String prefix = fptn.getFrequency()+":" ;	//colon as delimiter between support and prefix (list of items separated by space)
		String tmp;
		
		fptn = fptn.getParent();	
		tmp = fptn.getItem();
		
		if(tmp.equals("root"))
			return null;
		
		while(fptn != null && !(tmp.equals("root")))
		{
			prefix = prefix + tmp + " ";
			fptn = fptn.getParent();
			tmp = fptn.getItem();
		}		
		return prefix;
	}
	
	/* 
	 * For a given header element, form its conditional pattern base by getting all its possible prefixes.
	 * Return all the prefixes in a array list.
	 */
	public ArrayList<String> getConditionalPatternBase(FPTreeHeaderElement elem) 
	{
		ArrayList<String> cond_patt_base;
		FPTreeNode fptn = elem.getNodeLink();
		
		if(fptn==null)	
			return null;
		else
			cond_patt_base = new ArrayList<String>();
		
		while(fptn!=null)
		{
			String prefix = getPrefixEndingWith(fptn);
			if(prefix!=null)
				cond_patt_base.add(prefix);
			fptn = fptn.getNextNode();
		}
		
		return cond_patt_base;
	}
	
	/*
	 * Calculate individual item support by traversing along the node links and adding up the frequencies.
	 * Return <total support> 
	 */
	public int getIndividualItemSupport(FPTreeHeaderElement elem) 
	{		
		int supp=0;
		FPTreeNode fptn = elem.getNodeLink();
		
		while(fptn!=null)
		{
			supp += fptn.getFrequency();
			fptn = fptn.getNextNode();
		}
		
		return supp ;
	}
	
	/*
	 * This is the main FP-Growth method.
	 * Frequent Item sets with their respective supports will be printed through this method.
	 * The frequent item sets mined from the FPTree will have support 
	 * greater than or equal to the default support threshold of the FPTree.
	 */
	public void minePatternsByFPGrowth(String curr)
	{
		if(this.isSingleBranch(fptree_root))	//if tree contains a single path, then generate all possible combinations as frequent item sets
		{
			if(fptree_root.getChildrenCount()==1)	
			{
				this.genAllCombinations(fptree_root.getChild(0), curr);		//leave out fptree_root, start with the child which has the 1st item	
				num_of_single_paths++;
			}
		}
		else
		{
			for(int i=header_table.size()-1; i>=0; i--)
			{
				FPTreeHeaderElement elmnt = header_table.get(i);
				String itemset = curr + " " + elmnt.getItem() + " : " + this.getIndividualItemSupport(elmnt);
				// System.out.print(itemset + "\n");	//print frequent item set containing this element
				
				ArrayList<String> conditional_pattern_base = this.getConditionalPatternBase(elmnt);	//get the conditional pattern base
				FPTree subtree = new FPTree(conditional_pattern_base, support_threshold);	//create FP-tree from this conditional pattern base
				String pattern = curr + " " + elmnt.getItem();	
				subtree.minePatternsByFPGrowth(pattern);	//recursively mine the subtree
			}
		}
		
		fptree_mining_calls++;
	}

	/*
	 * Overloaded FP-Growth method.
	 * We can provide as argument a support value, which must be >= the default support threshold of the FPTree.
	 * Otherwise, we will miss out frequent item sets, as the tree would have been constructed with a greater initial support threshold. 
	 */
	public void minePatternsByFPGrowth(String curr, int support_val)
	{
		if(support_val<support_threshold)	//erroneous parameters
		{
			System.out.println("\nError! Frequent Itemsets can be missed, as FP-Tree is constructed with higher support!\n");
			return;			
		}
		
		if(this.isSingleBranch(fptree_root))	//if tree contains a single path, then generate all possible combinations as frequent item sets
		{
			if(fptree_root.getChildrenCount()==1)
			{
				this.genAllCombinations(fptree_root.getChild(0), curr);		//leave out fptree_root, start with the child which has the 1st item	
				num_of_single_paths++;
			}
		}
		else
		{
			for(int i=header_table.size()-1; i>=0; i--)
			{
				FPTreeHeaderElement elmnt = header_table.get(i);
				
				int prj_supp = this.getIndividualItemSupport(elmnt);
				if(prj_supp<support_val)	//if single ton item is not frequent as per the new higher support value,
					continue;				//then skip it, no need to deal with its projected database
				
				String itemset = curr + " " + elmnt.getItem() + " : " + prj_supp;
				// System.out.print(itemset + "\n");	//print frequent item set containing this element
				
				ArrayList<String> conditional_pattern_base = this.getConditionalPatternBase(elmnt);	//get the conditional pattern base
				FPTree subtree = new FPTree(conditional_pattern_base, support_val);	//create FP-tree from projected database, with higher support
				String pattern = curr + " " + elmnt.getItem();	
				subtree.minePatternsByFPGrowth(pattern);	//recursively mine the subtree, no need to pass higher support as argument again!
			}
		}
		
		fptree_mining_calls++;
	}
	
	/* Prints statistics about the function call. */
	public String printFunctionCallStats() {
		
		String ret = "\nDirect FP Tree Construction: \t"+fptree_construction_calls;
		ret += "\nConditional FP-Tree Constructions: \t"+cond_fptree_construction_calls;
		ret += "\nMining Frequent Itemsets calls: \t"+fptree_mining_calls;
		ret += "\nDirect mining from single path calls: \t"+single_path_mining_calls;
		ret += "\nNo. of single path: \t"+num_of_single_paths;
		
		System.out.print(ret);		
		return ret;
	}
	
	public void refreshFunctionCallStats() {
		
		fptree_construction_calls = 0;
		cond_fptree_construction_calls = 0;
		fptree_mining_calls = 0;
		single_path_mining_calls = 0;
		num_of_single_paths = 0;
	}
	
	
	/*
	 *	The following function computes the average depth of the FREQUENT elements within the prefix Tree structure.  
	 */
	public double computeAvgDepthOfFrequentElements() 
	{
		double avg_depth_tot = 0;
		int count = 0;
		
		for(int i=header_table.size()-1; i>=0; i--)
		{
			FPTreeHeaderElement elmnt = header_table.get(i);
			int suppt = this.getIndividualItemSupport(elmnt);
			
			if(suppt<support_threshold)
				continue;
			
			count++;		//frequent element
			
			int branches=0;
			double running_depth=0;
			FPTreeNode fptn = elmnt.getNodeLink();
			while(fptn!=null)
			{
				branches++;
				FPTreeNode node = fptn.getParent();
				String tmp = node.getItem();
				int ht=1;
				while(!tmp.equals("root"))
				{
					node = node.getParent();
					tmp = node.getItem();
					ht++;
				}
				fptn = fptn.getNextNode();
				running_depth+=ht;
			}
			avg_depth_tot += running_depth/branches;
		}		
		return avg_depth_tot/count;			//average of the average depths
	}
	
	/*
	 *	The following function computes the average spread of the FREQUENT elements within the prefix Tree structure.  
	 */
	public double computeAvgSpreadOfFrequentElements() 
	{
		double spread_tot = 0;
		int count = 0;
		
		for(int i=header_table.size()-1; i>=0; i--)
		{
			FPTreeHeaderElement elmnt = header_table.get(i);
			int suppt = this.getIndividualItemSupport(elmnt);
			
			if(suppt<support_threshold)
				continue;
			
			count++;		//frequent element
			
			int branches=0;
			FPTreeNode fptn = elmnt.getNodeLink();
			while(fptn!=null)
			{
				branches++;
				fptn = fptn.getNextNode();
			}
			spread_tot += branches;
		}		
		return spread_tot/count;			//average of the spreads
	}
	
	/*
	 *	The following function computes the size of the tree.  
	 */
	public int computeTreeSize() 
	{
		int tree_size = 1;		
		for(int i=header_table.size()-1; i>=0; i--)
		{
			FPTreeHeaderElement elmnt = header_table.get(i);
			FPTreeNode fptn = elmnt.getNodeLink();
			while(fptn!=null)
			{
				tree_size++;
				fptn = fptn.getNextNode();
			}
		}		
		return tree_size;
	}
	
	/* Print statistics about the FP-Tree. */
	public String printTreeDetails() {
		
		String ret = "\nAverage Depth: "+computeAvgDepthOfFrequentElements();
		ret += "\nAverage Spread: "+computeAvgSpreadOfFrequentElements();
		
		System.out.print(ret);		
		return ret;
	}

	/*
		Pairwise Item Lift Implementation
	*/
	// public void populatePilTable(Set<String> item, Hashtable<Set<String>,Integer> pilTable) {
	// 	pilTable.put(item,);
	// }

	public void extractItemsTwo(String transaction, Hashtable<Set<String>,Integer> items_frequency, Set<String> item) {
		String []itemsArray = transaction.split("\\s+");	//scan individual items in the transaction
		Set<String> items = new HashSet<String>(Arrays.asList(itemsArray));
		// System.out.println(items);
		if (items.containsAll(item)) {
			int count = 1;
			if (items_frequency.containsKey(item))
				count = items_frequency.get(item) + 1;
			items_frequency.put(item, new Integer(count));
		}
	}

	public void populateTwoFreqTable(File inputfile, Hashtable<Set<String>,Integer> itemsTwoFreqTable, Set<String> pair) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
                
        while((transaction = br.readLine()) != null) //read input file (transaction database)
        	extractItemsTwo(transaction, itemsTwoFreqTable, pair);	// update counters for every 2-length item
        
        br.close(); //file reading complete
	}

	public int populateFreqTable (File inputfile, Hashtable<String,Integer> items_frequency) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
        int n = 0;
        while((transaction = br.readLine()) != null) { //read input file (transaction database)
        	extractItems(transaction, 1, items_frequency);
        	n++;
        }
        
        br.close(); //file reading complete
        return n;
	}

	public int populateAll (File inputfile, Hashtable<String,Integer> items_frequency, Hashtable<Set<String>,Integer> itemsTwoFreqTable, List<Set<String>> pairs) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputfile)));
        String transaction = null;
        int n = 0;
        while((transaction = br.readLine()) != null) { //read input file (transaction database)
        	Set<String> localKeyset = extractItems(transaction, 1, items_frequency);
 			List<Set<String>> localPairs = generateAllPairs(localKeyset);
 			pairs.addAll(localPairs);
 			for (Set<String> localPair: localPairs){
 				// extractItemsTwo(transaction, itemsTwoFreqTable, localPair);
 				int count = 1;
 				if (itemsTwoFreqTable.containsKey(localPair))
 					count = itemsTwoFreqTable.get(localPair) + 1;
 				itemsTwoFreqTable.put(localPair, new Integer(count));
 			}
        	n++;
        }
        
        br.close(); //file reading complete
        return n;
	}

	/*
	String s = "abcd";
    int k = 2;
    int n = s.length();
    int sub = 1 << n;
    
    for (int i = 0; i < sub; i++) {
      int count = 0;
      for (int j = 0; j < n; j++) {
        if ((i&(1<<j)) != 0)
          count++;
      }
      if (count == k) {
        for (int j = 0; j < n; j++) {
          if ((i&(1<<j))!=0)
            System.out.print(s.charAt(j));
        }
        System.out.println();
      }
      
    }

	*/

	// private static void getSubsets(List<String> superSet, int k, int idx, Set<String> current, List<Set<String>> solution) {
	//     //successful stop clause
	//     if (current.size() == k) {
	//         solution.add(new HashSet<>(current));
	//         return;
	//     }
	//     //unseccessful stop clause
	//     if (idx == superSet.size()) return;
	//     String x = superSet.get(idx);
	//     current.add(x);
	//     //"guess" x is in the subset
	//     getSubsets(superSet, k, idx+1, current, solution);
	//     current.remove(x);
	//     //"guess" x is not in the subset
	//     getSubsets(superSet, k, idx+1, current, solution);
	// }


	public List<Set<String>> generateAllPairs(Set<String> keyset) {
		List<String> s = new ArrayList<>();
		List<Set<String>> res = new ArrayList<>();
		s.addAll(keyset);

	    int k = 2;
	    
	    // List<String> s = new ArrayList<String>(Arrays.asList("a","b","c","d"));
	    // Set<String> s = new HashSet<String>(Arrays.asList("a","b","c","d"));
	      int n = s.size();
	      int sub = 1 << n;
	      
	      for (int i = 0; i < sub; i++) {
	        int count = 0;
	        for (int j = 0; j < n; j++) {
	          if ((i&(1<<j)) != 0)
	            count++;
	        }
	        if (count == k) {
	          Set<String> tmp = new HashSet<String>();
	          for (int j = 0; j < n; j++) {
	            if ((i&(1<<j))!=0) {
	              // String x = Character.toString(s.charAt(j));;
	              tmp.add(s.get(j));
	              
	              // System.out.print(s.charAt(j));
	            }
	              
	          }
	          res.add(tmp);
	        }
	        
	      }
	    // System.out.println(res);
	    
	  

		// getSubsets(superSet, 2, 0, new HashSet<String>(), res);	// look for all subsets of length k = 2
		return res;
	}

	public static void main(String args[]) 
	{
		String input_data_filename;
		int min_supp = 1;

		if(args.length>2)
		{
			System.out.println("Improper Usage! Number of arguments cannot be more than 2.");
			System.out.println("Please provide input filename and [optional] minimum support.");
			System.exit(0);
		}
		else if(args.length==2)
			min_supp = Integer.parseInt(args[1]);

		input_data_filename = "" + args[0];
		File file = new File(input_data_filename);
		
		long startTime, endTime, totalTime, tcTime = 0;  

		startTime = System.currentTimeMillis();
		FPTree fpt = new FPTree(file, min_supp);
		endTime   = System.currentTimeMillis();
		totalTime = endTime - startTime;
		tcTime += totalTime;
		System.out.println("\n");
		System.out.println("TreeSize: " + fpt.computeTreeSize() + " nodes\t " + "Construction Time: " + totalTime + " ms");
		System.out.println("\n");
		System.out.println("------------");
		System.out.println("Tree Details:");
		fpt.printTreeDetails();
		System.out.println("\n");
		
		/* 1. Will print the header table. */
		// System.out.println("------------");
		// System.out.println("Header Table Information:");
		fpt.traverseFPTreeHeaderTable();
		// System.out.println("\n");

		/* 2. Will print the prefix tree. */
		// System.out.println("------------");
		// System.out.println("Prefix Tree Information:");
		fpt.traverseFPTree();
		// System.out.println("\n");
		
		System.out.println("------------");
		System.out.println("Tree Mining Result:");
		/* 3. Will mine all the frequent patterns. */
		startTime = System.currentTimeMillis();
		fpt.minePatternsByFPGrowth("");
		endTime = System.currentTimeMillis();
		totalTime = endTime - startTime;
		tcTime += totalTime;
		System.out.println("\n");
		System.out.println("Mining Time: " + totalTime + " ms");
		System.out.println("\n");

		System.out.println("------------");
		System.out.println("Function Call Statistics:");
		fpt.printFunctionCallStats();
		System.out.println("\n");
		System.out.println("------------");
		System.out.println("TOTAL Construction Time (mining+construction): " + tcTime + " ms");
		System.out.println("\n");
		System.out.println("------------");
		System.out.println("USING PAIRWISE ITEM LIFT:");
		System.out.println("\n");

		// FPTree fptPil = new FPTree(file, min_supp, true);
		tcTime = 0;
		startTime = System.currentTimeMillis();
		FPTree fptPil = new FPTree(file, min_supp, true);
		endTime   = System.currentTimeMillis();

		totalTime = endTime - startTime;
		tcTime += totalTime;
		System.out.println("\n");
		System.out.println("TreeSize: " + fptPil.computeTreeSize() + " nodes\t " + "Construction Time: " + totalTime + " ms");
		System.out.println("\n");
		System.out.println("------------");
		System.out.println("Tree Details:");
		fptPil.printTreeDetails();
		System.out.println("\n");
		
		/* 1. Will print the header table. */
		// System.out.println("------------");
		// System.out.println("Header Table Information:");
		fptPil.traverseFPTreeHeaderTable();
		// System.out.println("\n");

		// /* 2. Will print the prefix tree. */
		// System.out.println("------------");
		// System.out.println("Prefix Tree Information:");
		fptPil.traverseFPTree();
		// System.out.println("\n");
		
		System.out.println("------------");
		System.out.println("Tree Mining Result:");
		/* 3. Will mine all the frequent patterns. */
		startTime = System.currentTimeMillis();
		fptPil.minePatternsByFPGrowth("");
		endTime = System.currentTimeMillis();
		totalTime = endTime - startTime;
		tcTime += totalTime;
		System.out.println("\n");
		System.out.println("Mining Time: " + totalTime + " ms");
		System.out.println("\n");

		System.out.println("------------");
		System.out.println("Function Call Statistics:");
		fptPil.printFunctionCallStats();
		System.out.println("\n");
		System.out.println("------------");
		System.out.println("TOTAL Construction Time (mining+construction): " + tcTime + " ms");
		System.out.println("\n");
		System.out.println("------------");

	}	
}
