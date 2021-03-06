package io.spotnext.core.infrastructure.service;

public interface ConfigurationService {

	/**
	 * Returns a String value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 */
	String getString(String key);

	/**
	 * Returns a String value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 */
	String getString(String key, String defaultValue);

	/**
	 * Returns a Integer value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 * @throws Exception
	 */
	Integer getInteger(String key);

	/**
	 * Returns a Integer value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 * @throws Exception
	 */
	Integer getInteger(String key, Integer defaultValue);

	/**
	 * Returns a Integer value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 * @throws Exception
	 */
	Double getDouble(String key);

	/**
	 * Returns a Integer value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 * @throws Exception
	 */
	Double getDouble(String key, Double defaultValue);

	/**
	 * Returns a Boolean value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 */
	Boolean getBoolean(String key);

	/**
	 * Returns a Boolean value for the given key or null, if the key doesn't
	 * exist.
	 * 
	 * @param key
	 * @param defaultValue
	 */
	boolean getBoolean(String key, boolean defaultValue);

}
