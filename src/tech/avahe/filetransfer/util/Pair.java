package tech.avahe.filetransfer.util;

/**
 * 
 * @author Avahe
 *
 * @param <K> The <code>Pair's</code> key.
 * @param <V> The <code>Pair's</code> value.
 */
public class Pair<K, V> {

	private final K key;
	private final V value;
	
	/**
	 * Creates a new key/value pair.
	 * @param key The <code>Pair's</code> key.
	 * @param value The <code>Pair's</code> value.
	 */
	public Pair(final K key, final V value) {
		this.key = key;
		this.value = value;
	}
	
	/**
	 * @return The key of the <code>Pair</code>.
	 */
	public K getKey() {
		return this.key;
	}
	
	/**
	 * @return The value of the <code>Pair</code>.
	 */
	public V getValue() {
		return this.value;
	}
	
}