package com.fluidsimulator.gameobjects.fluid;

import java.util.ArrayList;
import java.util.Iterator;

abstract public class SpatialTable<V> implements Iterable<V> {
	
	// default nearby table size
	private static final int DEFAULT_NEARBY_SIZE = 50;
	// the actual spatial table
	private final ArrayList<V> table;
	// a void list initialized here for reuse
	private ArrayList<V> voidList = new ArrayList<V>(1);
	// the nearby elements table
	private ArrayList<V>[][] nearby;
	// row and column of the spatial table
	private int row;
	private int column;
	// temporary variables for faster iterations and optimized object allocations
	private int i;
	private int j;
	private int tempSize;
	private int z;
	private int x;
	private int y;
	private int xPrev;
	private int yPrev;
	// abstract position variables must be implemented on actual class instantiation
	abstract protected int posX(V value);
	abstract protected int posY(V value);
	abstract protected int prevPosX(V value);
	abstract protected int prevPosY(V value);
	
	@SuppressWarnings("unchecked")
	public SpatialTable(int column, int row) {
		this.row = row; 
		this.column = column;
		table = new ArrayList<V>((row*column)/2);
		nearby = new ArrayList[column][row];
	}
	
	/**
	 * Initialize the nearby table with default size
	 */
	public void initialize() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j] = new ArrayList<V>(DEFAULT_NEARBY_SIZE);
			}
		}
	}
	
	/**
	 * Append value to the table and identify its position in the space.
	 * Don't need to rehash table after append operation.
	 * 
	 * @return true (as per the general contract of Collection.add).
	 */
	public boolean add(V value) {
		addInterRadius(value);
		table.add(value);
		return true;
	}
	
	public Iterator<V> iterator() {
		return table.iterator();
	}
	
	/**
	 * Get an item from table.
	 * 
	 * @return the element if contained (as per the general contract of Collection.get), otherwise null.
	 */
	public V get(int i) {
		return table.get(i);
	}
	
	/**
	 * Remove entity from table.
	 * 
	 * @return true (as per the general contract of Collection.remove).
	 */
	public boolean remove(V value) {
		table.remove(value);
		return true;
	}
	
	/**
	 * Clear everything
	 */
	public void clear() {
		for (i = 0; i < column; ++i) {
			for (j = 0; j < row; ++j) {
				nearby[i][j].clear();
				nearby[i][j] = null;
			}
		}
		table.clear();
	}
	
	/**
	 * The size of the table
	 * 
	 * @return the integer size of the table.
	 */
	public int size() {
		return table.size();
	}
	
	/**
	 * Returns array of nearby objects in space.
	 * 
	 * @param value - central object
	 * @return array containing close elements
	 */
	public ArrayList<V> nearby(V value) {
		x = posX(value);
		y = posY(value);
		if (!inRange(x, y)) 
			return voidList; 
		return nearby[x][y];
	}
	
	/**
	 * Update position for an item
	 */
	public void updatePosition(V value) {
		x = posX(value);
		y = posY(value);
		xPrev = prevPosX(value);
		yPrev = prevPosY(value);
		if (inRange(xPrev, yPrev))
			nearby[xPrev][yPrev].remove(value);
		if (inRange(x, y))
			nearby[x][y].add(value);
	}
	
	/**
	 * The size of the nearby table
	 * 
	 * @return the integer size of the nearby table.
	 */
	public int sizeNearby(V value) {
		return nearby(value).size();
	}

	/**
	 * Updates the spatial relationships of objects. Rehash function
	 * needed if elements change their position in the space.
	 */
	public void rehash() {
		for (i=0; i<column; i++) {
			for (j=0; j<row; j++) {
				if (nearby[i][j] != null)
					nearby[i][j].clear();
			}
		}
		tempSize = table.size();
		for (z=0; z<tempSize; z++) {
			addInterRadius(table.get(z));
		}
	}
	
	/**
	 * Add element to its position and neighbor cells.
	 */
	private void addInterRadius(V value) {
		for (i = -1; i < 2; ++i) {
			for (j = -1; j < 2; ++j) {
				x = posX(value)+i;
				y = posY(value)+j;
				if (inRange(x, y))
					nearby[x][y].add(value);
			}
		}
	}
	
	/**
	 * Check if a position is out of the spatial range
	 * 
	 * @return true if position is in range.
	 */
	private boolean inRange(float x, float y) {
		return (x > 0 && x < column && y > 0 && y < row);
	}

}
