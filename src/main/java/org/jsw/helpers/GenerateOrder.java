package org.jsw.helpers;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GenerateOrder{
	private List<JSONObject> orders;
	
	private List<String> product_types = new ArrayList<>(Arrays.asList("baguette", 
			"eclair", "muffin", "doughnut", "cheesecake", "croissant", "apple pie", 
			"swiss roll", "brownies", "strudel", "cup bake", "biscuit"));
	
	public List<JSONObject> getOrder() {
		//Object[] args = getArguments(); //For now no arguments taken
		orders = new ArrayList<>();
		JSONObject order = new JSONObject();
				
		String customer_id = generateRandomID(10);
		int min = 1;
		int max = 50;
		int total_order = 0;
		
		order.put("id", customer_id);
		JSONArray products = new JSONArray();
		JSONObject product = new JSONObject();
		
		for(String product_type : product_types ) {
			total_order = ThreadLocalRandom.current().nextInt(min, max + 1);
			product.put(product_type, total_order);
			products.add(product);
		}
		
		order.put("Product List", products);
		orders.add(order);
		return orders;
	}
	
	private String generateRandomID(int length) {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
		             + "abcdefghijklmnopqrstuvwxyz"
		             + "0123456789";
		String str = new Random().ints(length, 0, chars.length())
		                         .mapToObj(i -> "" + chars.charAt(i))
		                         .collect(Collectors.joining());
		return str;
	}
}