package org.jsw.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ManageMessage {
	
	/*
	16 Nov 2018: 
	Expected Message: 
	{	"baguette": {"bakery_1": 500, "bakery_2": 100, ...},
		"apple pie": {"bakery_2": 10, ...},
		...
	} 
	Process: pick the lowest offered price from all bakeries for each type of product
	Expected Confirmation: 
	{
		"baguette": "bakery_1", "apple pie": "bakery_3", ...
	}
	*/
	public static JSONObject getConfirmation(JSONObject message, List<String> bakeryName, List<String> productTypes) throws JSONException {
		JSONObject confirmation = new JSONObject();
		JSONObject bakeries = new JSONObject();
		
		int min_price = Integer.MAX_VALUE;
		String chosenBakery = "";
		
		for (String type : productTypes) {
			if (message.has(type)) {
	            bakeries = message.getJSONObject(type);
	        }
			for (String bakery : bakeryName) {
				if (bakeries.has(bakery)) {
					if (min_price > bakeries.getInt(bakery)) {
						chosenBakery = bakery;
						min_price = bakeries.getInt(bakery);
					}
				}
			}
			min_price = Integer.MAX_VALUE;
			confirmation.put(type, chosenBakery);
		}
		
		return confirmation;
	}

}
