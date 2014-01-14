package cs.uga.edu.util;

import java.util.Set;

import com.google.common.collect.HashMultimap;

public class MultiBiMap<K,V> {

	private final HashMultimap<K, V> keysToValues = HashMultimap.create();

	private final HashMultimap<V, K> valuesToKeys = HashMultimap.create();

	public Set<V> getValues(K key)
	{
		return keysToValues.get(key);
	}

	public Set<K> getKeys(V value)
	{
		return valuesToKeys.get(value);
	}

	public boolean put(K key, V value)
	{
		return keysToValues.put(key, value) && valuesToKeys.put(value, key);

	}
	
	public boolean containEntry(K key, V value)
	{
		return keysToValues.containsEntry(key, value) & valuesToKeys.containsEntry(value, key);
	}

	public boolean putAll(K key, Iterable<? extends V> values)
	{
		boolean changed = false;

		for (V value : values)
		{
			changed = put(key, value) || changed;
		}

		return changed;
	}
}
