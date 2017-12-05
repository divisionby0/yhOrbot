package dev.div0.robotOperations.events;

import dev.div0.events.BaseEvent;


public class OperationEvent extends BaseEvent {

	public static String COORDINATES_CHANGED = "COORDINATES_CHANGED";
	public static String DATE_SELECTED = "DATE_SELECTED";
	public static String MONTH_SELECTED = "MONTH_SELECTED";
	public static String ELEMENT_TEXT = "ELEMENT_TEXT";
	public static String TIME_SELECTED = "TIME_SELECTED";
	
	public OperationEvent(String type) {
		super(type);
	}
}