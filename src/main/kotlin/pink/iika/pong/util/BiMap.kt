package pink.iika.pong.util

class BiMap<K, V> {
    private val keyToValue: MutableMap<K, V> = mutableMapOf()
    private val valueToKey: MutableMap<V, K> = mutableMapOf()

    fun put(key: K, value: V) {
        keyToValue[key]?.let { valueToKey.remove(it) }
        valueToKey[value]?.let { keyToValue.remove(it) }

        keyToValue[key] = value
        valueToKey[value] = key
    }

    fun getValue(key: K): V? = keyToValue[key]

    fun getKey(value: V): K? = valueToKey[value]

    fun getValues(): MutableSet<V> = valueToKey.keys

    fun getKeys(): MutableSet<K> = keyToValue.keys

    fun removeByKey(key: K): V? {
        val value = keyToValue.remove(key)
        if (value != null) {
            valueToKey.remove(value)
        }
        return value
    }

    fun removeByValue(value: V): K? {
        val key = valueToKey.remove(value)
        if (key != null) {
            keyToValue.remove(key)
        }
        return key
    }

    fun containsKey(key: K): Boolean = keyToValue.containsKey(key)

    fun containsValue(value: V): Boolean = valueToKey.containsKey(value)

    fun size(): Int = keyToValue.size

    fun clear() {
        keyToValue.clear()
        valueToKey.clear()
    }
}
