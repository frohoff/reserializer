package org.frohoff.ysoserial;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static privilegedaccessor.StrictPA.*;


public interface Doppelganger {
	
	public static class Utils {
		public static Map<String,Object> getFieldValues(Object obj) {
			Collection<String> fieldNames = getFieldNames(obj);
			Map<String,Object> fieldValues = new HashMap<String,Object>(fieldNames.size());
			for (String fieldName : fieldNames) {
				try {
					fieldValues.put(fieldName, getValue(obj, fieldName));
				} catch (NoSuchFieldException e) {
					fieldValues.put(fieldName, e);
				}
			}
			return fieldValues;
		}		
	}
}
