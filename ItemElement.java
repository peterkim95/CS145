
import java.util.Comparator;

public class ItemElement implements Comparable<ItemElement> {
	private String item;
	private int frequency;
	private int pilIndex = -1;
	
	public ItemElement() 
	{
		item = null;
		frequency = 0;
	}
	
	public ItemElement(String item, int frequency) 
	{
		this.item = item;
		this.frequency = frequency;
	}

	public ItemElement(String item, int frequency, int pilIndex) 
	{
		this.item = item;
		this.frequency = frequency;
		this.pilIndex = pilIndex;
	}
	
	public ItemElement(String item) 
	{
		this.item = item;
		this.frequency = 0;
	}
	
	public String getItem() 
	{
		return item;
	}
	
	public int getFrequency() 
	{
		return frequency;
	}

	public int getPilIndex() 
	{
		return pilIndex;
	}
	
	@Override
	public int compareTo(ItemElement ie) 
	{
		if (this.getPilIndex() > 0) {
			return this.getPilIndex() - ie.getPilIndex();
		} else {
			if (this.getFrequency() != ie.getFrequency())
				return ie.getFrequency() - this.getFrequency(); //sort in descending order of frequencies
			else 
				return (this.getItem()).compareTo(ie.getItem()); //if frequencies are equal, use lexicographic order
		}
		
	}
	
	@Override
	public String toString() 	//<item>:<frequency>(pilIndex)
	{
		if (pilIndex > 0)
			return item + ":" + frequency + "("+pilIndex+") ";
		return item + ":" + frequency + " ";
	}
}

/*
 * Explicitly writing the comparator for the default sort order of items 
 * 1. descending order of frequencies 
 * 2. if frequencies are equal, then use lexicographic order 
 */ 
class ItemElementFrequencyComparator implements Comparator<ItemElement> {
    public int compare(ItemElement ie1, ItemElement ie2) 
    {
    	if(ie1.getFrequency() != ie2.getFrequency())
    		return ie2.getFrequency() - ie1.getFrequency();
    	else
    		return (ie1.getItem()).compareTo(ie2.getItem());
    }
} 

//Comparator for lexicographic sorting 
class ItemElementLexicographicComparator implements Comparator<ItemElement> {
  public int compare(ItemElement ie1, ItemElement ie2) 
  {
  	return (ie1.getItem()).compareTo(ie2.getItem());
  }
} 
