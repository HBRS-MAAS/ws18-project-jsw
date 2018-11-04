
package org.jsw.agents;


import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.List;
import java.util.Vector;
import java.util.Random;


public class CustomerAgent extends Agent {
	// List of products from which the customer chooses
		private List<String> ProductList;
		// list of items to buy
		private List<String> targetProducts;
		private int noTargetProducts = 3;
		// Product bought
		private List<String> acquiredProducts;
		// The list of Bakery agents
		private AID [] BakeryAgents;

		protected void setup() {
			// Welcome message
			System.out.println(getAID().getLocalName()+" is ready.");

			initializeProducts();
			initializeTargetProducts();
			acquiredProducts = new Vector<>();

			System.out.println(getAID().getName()+ " will try to buy  "+targetProducts);

			registerCustomer();

			// Add a TickerBehaviour for each targetBook
			for (String targetProducts : targetProducts) {
				addBehaviour(new TickerBehaviour(this, 5000) {
					protected void onTick() {
						System.out.println(getAID().getLocalName()+"is trying to buy "+targetProducts);

						// Update bakery's around
						getBakeryAgents(myAgent);

						if(acquiredProducts.contains(targetProducts)){
							System.out.println(getAID().getLocalName()+" has already bought" + targetProducts);
							printAcquiredProducts();
							// Check the number of Products bought 
							checkNBoughtProducts();
	
							stop();
						}
						else{
							// Perform the request
							myAgent.addBehaviour(new RequestPerformer(targetProducts));

						}
					}

				} );

	}
			try {
	 			Thread.sleep(3000);
	 		} catch (InterruptedException e) {
	 			//e.printStackTrace();
	 		}
		}

		public void checkNBoughtProducts(){
			if(acquiredProducts.size() == noTargetProducts){
				System.out.println(getAID().getLocalName()+" has bought " + acquiredProducts.size() + " Products");
				// Stop this agent
				doDelete();
			}
			else{
				System.out.println(getAID().getLocalName()+" has not bought " + noTargetProducts + " yet");
			}
		}

		protected void takeDown() {
	        // Deregister from the yellow pages
			try {
				DFService.deregister(this);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			System.out.println(getAID().getLocalName() + ": Terminating.");
		}

		public void registerCustomer(){
			//Register the customer in particular area
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Customer");
			sd.setName("Bakery Customer");
			dfd.addServices(sd);
			try {
				DFService.register(this, dfd);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
	}
		public void getBakeryAgents(Agent myAgent){
			// Update the list of seller agents
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Product-selling");
			template.addServices(sd);
			try {
				DFAgentDescription [] result = DFService.search(myAgent, template);
				System.out.println("Found the following Bakerys:");
				BakeryAgent = new AID [result.length];
				for (int i = 0; i < result.length; ++i) {
					BakeryAgent[i] = result[i].getName();
					System.out.println(BakeryAgent[i].getName());
				}
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}

		}

		public void printAcquiredProducts(){
			System.out.println("Agent"+ getAID().getLocalName()+" bought:");
			System.out.println(acquiredProducts);
		}

		public void initializeProducts(){
			ProductList = new Vector<>();
			ProductList.add("Bannana Bread");
			ProductList.add("Chocolate Bread");
			ProductList.add("Cinnamon Bread");
			ProductList.add("Nut Bread");
		}

		protected void initializeTargetBooks(){
			targetProducts = new Vector<>();
			Random rand = new Random();
			// Get a random index of the catalogueBooks until the target books has nTargetBooks
			while(targetProducts.size()< noTargetProducts){
				int randomIndex = rand.nextInt(ProductList.size());
				boolean titleInTargetProducts = targetProducts.contains(ProductList.get(randomIndex));
				if (!titleInTargetProducts)
					targetProducts.add(ProductList.get(randomIndex));
			}

	}
		private class RequestPerformer extends Behaviour {
			private AID BakeryNear; // The agent who is near our location
			private int bestPrice;  // The best offered price
			private int location; //location of Bakery
			private int repliesCnt = 0; // The counter of replies from seller agents
			private MessageTemplate mt; // The template to receive replies
			private int step = 0;
			private String targetProduct;

			public RequestPerformer(String targetBook){
				this.targetProduct = targetProduct;
			}

			public void action() {
				switch (step) {
				case 0:
					// Send the cfp to all sellers
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					for (int i = 0; i < BakeryAgents.length; ++i) {
						cfp.addReceiver(BakeryAgents[i]);
					}
					cfp.setContent(targetProduct);
					cfp.setConversationId("buy-products");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
					myAgent.send(cfp);
					// Prepare the template to get proposals
					message_temp = MessageTemplate.and(MessageTemplate.MatchConversationId("buy-products"),
							MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					step = 1;
	                break;
				case 1:
					// Receive all proposals/refusals from seller agents
					ACLMessage reply = myAgent.receive(message_temp);
					if (reply != null) {
						// Reply received
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							// This is an offer
							int price = Integer.parseInt(reply.getContent());
							int location_bakery = Integer.parseInt(reply.getContent());
							if (BakeryNear== null || (price < bestPrice  ) {
								// This is the best offer at present
								bestPrice = price;
								location= location_bakery;
								BakeryNear= reply.getSender();
							}
						}
						repliesCnt++;
						if (repliesCnt >= BakeryAgents.length) {
							// We received all replies
							step = 2;
						}
					}
					else {
						block();
					}
	break;
				case 2:
					// Send the purchase order to the seller that provided the best offer
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(BakeryNear);
					order.setContent(targetProduct);
					order.setConversationId("buy-products");
					order.setReplyWith("order"+System.currentTimeMillis());
					myAgent.send(order);
					// Prepare the template to get the purchase order reply
					message_temp = MessageTemplate.and(MessageTemplate.MatchConversationId("buy-products"),
							MessageTemplate.MatchInReplyTo(order.getReplyWith()));
					step = 3;
					break;
				case 3:
					// Receive the purchase order reply
					reply = myAgent.receive(message_temp);
					if (reply != null) {
						// Purchase order reply received
						if (reply.getPerformative() == ACLMessage.INFORM) {
							// Purchase successful. We can terminate
							System.out.println("Agent "+getAID().getLocalName()+ " successfully purchased "+ targetProduct+ " from agent "+reply.getSender().getLocalName());
							
							acquiredProducts.add(targetProduct);
							
						}
						else {
							System.out.println("Failed no offer available.");
						}

						step = 4;
					}
					else {
						block();
					}
					break;
				}
			}

			public boolean done() {
				if (step == 2 && BakeryNear == null) {
					System.out.println("Attempt failed: "+targetProduct+" not available for sale");
				}
				return ((step == 2 && BakeryNear== null) || step == 4);
			}
		}  // End of inner class RequestPerformer

	    // Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
		private class shutdown extends OneShotBehaviour{
			public void action() {
				ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
				Codec codec = new SLCodec();
				myAgent.getContentManager().registerLanguage(codec);
				myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
				shutdownMessage.addReceiver(myAgent.getAMS());
				shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
				shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
				try {
				    myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
				    myAgent.send(shutdownMessage);
				}
				catch (Exception e) {
				    //LOGGER.error(e);
				}

	}
               
}
		
		
		
		
		
		
		
		
		
