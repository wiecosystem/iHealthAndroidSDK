package com.ihealth.communication.control;

import com.ihealth.communication.manager.iHealthDevicesCallback;

/**
 * Public APIs for the HTS device profiles
 */
public class BtmProfile {
    /**
     * The action type of callback after getBattery() method is called.<br/>
     * <b>See Also:</b><br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <b>KeyList</b> of the message:
     * <ul>
     * <li>
     * {@link #BTM_BATTERY}
     * </li>
     * </ul>
     * <b>message example:</b><br/>
     * {<br/>
     * &nbsp; &nbsp; "battery": 90<br/>
     * }<br/>
     */
    public static final String ACTION_BTM_BATTERY = "battery_btm";
    /**
     * The key of the power of battery for Po device.<br/>
     * Returns the message Key corresponding relation:<br/>
     * <ul>
     * <li>
     * BTM_BATTERY ----> "battery".<br/>
     * </li>
     * </ul>
     * <b>Value range:</b><br/>
     * Normal voltage
     */
    public static final String BTM_BATTERY = "battery";

    /**
     * The action type of callback after getMemoryData() method is called.<br/>
     * <b>See Also:</b><br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <b>KeyList</b> of the message:
     * <ul>
     * <li>
     * {@link #MEMORY_COUNT}
     * </li>
     * <li>
     * {@link #BTM_TEMPERATURE_ARRAY}
     * </li>
     * <li>
     * {@link #BTM_TEMPERATURE}
     * </li>
     * <li>
     * {@link #BTM_TEMPERATURE_TARGET}
     * </li>
     * <li>
     * {@link #BTM_MEASURE_TIME}
     * </li>
     * </ul>
     */
    public static final String ACTION_BTM_MEMORY = "memory_btm";
    /**
     * The key of the count of memory for BTM device.<br/>
     * Returns the message Key corresponding relation:<br/>
     * <ul>
     * <li>
     * MEMORY_COUNT ----> "memory_count".<br/>
     * </li>
     * </ul>
     * <b>Value range:</b><br/>
     * 0-255
     */
    public static final String MEMORY_COUNT = "memory_count";
    /**
     * The key of the temperature array for BTM device.<br/>
     * Returns the message Key corresponding relation:<br/>
     * <ul>
     * <li>
     * BTM_TEMPERATURE_ARRAY ----> "btm_temperature_array".<br/>
     * </li>
     * </ul>
     */
    public static final String BTM_TEMPERATURE_ARRAY = "btm_temperature_array";
    /**
     * The key of the temperature target for BTM device.<br/>
     * Returns the message Key corresponding relation:<br/>
     * <ul>
     * <li>
     * BTM_TEMPERATURE_TARGET ----> "btm_temperature_target".<br/>
     * </li>
     * </ul>
     */
    public static final String BTM_TEMPERATURE_TARGET = "btm_temperature_target";
    /**
     * The key of the temperature for BTM device.<br/>
     * Returns the message Key corresponding relation:<br/>
     * <ul>
     * <li>
     * BTM_TEMPERATURE ----> "btm_temperature".<br/>
     * </li>
     * </ul>
     */
    public static final String BTM_TEMPERATURE = "btm_temperature";
    /**
     * The key of the time of memory data for BTM device.<br/>
     * Returns the message Key corresponding relation:<br/>
     * <ul>
     * <li>
     * BTM_MEASURE_TIME ----> "measure_time".<br/>
     * </li>
     * </ul>
     * <b>Value Format:</b><br/>
     * yyyy-MM-dd HH:mm
     */
    public static final String BTM_MEASURE_TIME = "measure_time";

    /**
     * <b>See Also:</b><br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     * <b>KeyList</b> of the message:
     * <ul>
     * <li>
     * {@link #BTM_TEMPERATURE}
     * </li>
     * <li>
     * {@link #BTM_TEMPERATURE_TARGET}
     * </li>
     * </ul>
     */
    public static final String ACTION_BTM_MEASURE = "measure_btm";

    /**
     * The action type of callback after all method is called.<br/>
     * <b>See Also:</b><br/>
     * {@link iHealthDevicesCallback#onDeviceNotify(String mac, String deviceType, String action, String message)}<br/>
     */
    public static final String ACTION_BTM_CALLBACK = "btm_callback";
}
