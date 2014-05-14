package cs.uga.edu.Graph;

public class Binpacking {

	static int[] itemSize;
	static int[] bagFreeSpace;
	static boolean[][] doesBagContainItem; // in case this looks weird, [,] is a matrix, in java it would be [][]
	
	public static boolean pack(int item)
    { 
		// output the solution if we're done
        if (item == itemSize.length)
        {
            for (int i = 0; i < bagFreeSpace.length; i++)
            {
                System.out.println("bag" + i);
                for (int j = 0; j < itemSize.length; j++)
                    if (doesBagContainItem[i][j])
                    	 System.out.print("item" + j + "(" + itemSize[j] + ") ");
                System.out.println(); 
            }
            return true;
        }

        // otherwise, keep traversing the state tree
        for (int i = 0; i < bagFreeSpace.length; i++)
        {
            if (bagFreeSpace[i] >= itemSize[item])
            {
                doesBagContainItem[i][item] = true; // put item into bag
                bagFreeSpace[i] -= itemSize[item];
                if (pack(item + 1))                 // explore subtree
                    return true;
                bagFreeSpace[i] += itemSize[item];  // take item out of the bag
                doesBagContainItem[i][item] = false;
            }
        }

        return false;
		
    }

	public static void main(String[] args) {
		itemSize = new int[] { 209, 37,1,586,700,12,1648,109,27,152 };
        bagFreeSpace = new int[] { 1000, 2000, 2000, 2000 };
        doesBagContainItem = new boolean[bagFreeSpace.length][itemSize.length];

        if (!pack(0))
            System.out.println("No solution");
	}
}
