package prihodko.msit.lb2;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;

public class SpeleologistAgent extends Agent {

    private AID na; // NavigatorAgent
    private AID ea; // EnvironmentAgent
    private boolean alive = true;
    private String[] dict = {"There is a %s here. ",
            "I feel %s here. ",
            "It's a %s here. "};
    @Override
    protected void setup() {
        addBehaviour(new WakerBehaviour(this,1000) {
            @Override
            protected void onWake() {
                DFAgentDescription navigatorDescription = new DFAgentDescription();
                DFAgentDescription environmentDescription = new DFAgentDescription();
                ServiceDescription navigatorService = new ServiceDescription();
                ServiceDescription environmentService = new ServiceDescription();
                navigatorService.setType("Navigator");
                environmentService.setType("Cave_with_gold");
                navigatorDescription.addServices(navigatorService);
                environmentDescription.addServices(environmentService);
                try {
                    na = DFService.search(myAgent, navigatorDescription)[0].getName();
                    ea = DFService.search(myAgent, environmentDescription)[0].getName();
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                System.out.println("Speleologist agent " + getAID().getLocalName() + " is ready!");
                myAgent.addBehaviour(new CaveWanderingBehaviour());
            }
        });

    }

    @Override
    protected void takeDown() {
        System.out.println("Speleologist agent terminated!");
    }

    private class CaveWanderingBehaviour extends Behaviour {

        private int step = 0;
        private MessageTemplate mt;
        private String message;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage requestPercept = new ACLMessage(ACLMessage.REQUEST);
                    requestPercept.addReceiver(ea);
                    requestPercept.setConversationId("Get-percepts");
                    myAgent.send(requestPercept);
                    mt = MessageTemplate.MatchConversationId("Get-percepts");

                    step++;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            message = GeneratePerceptText(reply.getContent());
                            ACLMessage askForAction = new ACLMessage(ACLMessage.REQUEST);
                            askForAction.addReceiver(na);
                            askForAction.setContent(message);
                            askForAction.setConversationId("action");
                            System.out.println(getAID().getLocalName() + ": " + message);
                            myAgent.send(askForAction);
                            mt = MessageTemplate.MatchConversationId("action");
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    ACLMessage reply2 = myAgent.receive(mt);
                    if (reply2 != null) {
                        if (reply2.getPerformative() == ACLMessage.PROPOSE) {
                            // TODO
                            message = ProcessSentence(reply2.getContent());
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 3:
                    ACLMessage action = new ACLMessage(ACLMessage.CFP);
                    action.addReceiver(ea);
                    action.setContent(message);
                    action.setConversationId("action");
                    System.out.println(getAID().getLocalName() + " (to env): "+ message);
                    myAgent.send(action);
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("action"),
                            MessageTemplate.MatchInReplyTo(action.getReplyWith()));
                    step++;
                    break;
                case 4:
                    if (message == "Climb") {
                        step++;
                        doDelete();
                        return;
                    }
                    else
                        step=0;
                    break;

            }
        }

        private String ProcessSentence(String content) {
            if (content.contains("forward"))
                return "Forward";
            else if (content.contains("shoot"))
                return "Shoot";
            else if (content.contains("climb"))
                return "Climb";
            else if (content.contains("grab"))
                return "Grab";
            else if (content.contains("right"))
                return "TurnRight";
            else if (content.contains("left"))
                return "TurnLeft";
            throw new IllegalStateException("Unexpected action!");
        }

        private String GeneratePerceptText(String content) {
            StringBuilder temp = new StringBuilder();
            if (content.contains("k")) {
                System.out.println("Agent is dead.");
                temp.append("Killed..");
            } else {
                if (content.contains("s"))
                    temp.append(String.format(dict[new Random().nextInt(3)], "stench"));
                if (content.contains("b"))
                    temp.append(String.format(dict[new Random().nextInt(3)], "breeze"));
                if (content.contains("g"))
                    temp.append(String.format(dict[new Random().nextInt(3)], "glitter"));
                if (content.contains("u"))
                    temp.append(String.format(dict[new Random().nextInt(3)], "bump"));
                if (content.contains("c"))
                    temp.append(String.format(dict[new Random().nextInt(3)], "scream"));

                temp.append("What should I do?");
            }
            return temp.toString();
        }

        @Override
        public boolean done() {
            return step == 5 || !alive;
        }
    }
}
