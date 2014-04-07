package com.gt.seniordesign.dlmns;

import java.util.HashMap;
import java.util.UUID;

public class DLMNSGattAttributes {
	private static HashMap<String, UUID> attributes = new HashMap<String, UUID>();
	
	public static String DLMNS_SERVICE = "00000000-0000-0000-0000-000001ab34e6";
    public static String DUTY_CYCLE_CHAR = "00000000-0000-0000-0000-00012530b0cd";
    public static String ACK_CHAR = "00000000-0000-0000-0000-00345070ab01";
    
    static {
    	attributes.put("DLMNS Service", UUID.fromString(DLMNS_SERVICE));
    	attributes.put("Duty Cycle", UUID.fromString(DUTY_CYCLE_CHAR));
    	attributes.put("Acknowledge", UUID.fromString(ACK_CHAR));
    }
    
    public static UUID lookup(String name) {
    	return attributes.get(name);
    }
}
