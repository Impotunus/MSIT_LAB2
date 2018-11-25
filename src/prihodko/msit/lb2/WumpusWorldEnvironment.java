package prihodko.msit.lb2;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class WumpusWorldEnvironment extends Agent {

    // percept sequence, world objects: g - glitter, s - stench, b - breeze, u - bump, c - scream; p - pit, w - wumpus; k - agent dead
    String[][] WumpusWorldPercepts = {
            {"", "b", "p", "b"},
            {"s", "", "b", ""},
            {"w", "gbs", "p", "b"},
            {"s", "", "b", "p"}
    };
    private final String changedGold = "bs";
    private boolean goldTaken = false;
    private int spelCoord1 = 0;
    private int spelCoord2 = 0;
    private boolean spelAlive = true;
    private boolean bump = false;
    private boolean scream = false;
    private boolean arrowUsed = false;
    private boolean killed = false;
    private int dir = 0; // 0 - East, 1 - North, 2 - West, 3 - South

    @Override
    protected void setup() {

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Cave_with_gold");
        sd.setName("Cave");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent " + getAID().getName() + " is ready!");
        addBehaviour(new RequestBehavior());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent terminated!");
    }

    private class RequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST)
                    myAgent.addBehaviour(new PerceptReplyBehaviour(msg));
                if (msg.getPerformative() == ACLMessage.CFP)
                    myAgent.addBehaviour(new ChangeWorldBehaviour(msg));
            }
            else
            {
                block();
            }
        }

    }

    private class PerceptReplyBehaviour extends OneShotBehaviour {

        ACLMessage msg;

        public PerceptReplyBehaviour(ACLMessage m)
        {
            super();
            msg = m;
        }

        public void action() {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(WumpusWorldPercepts[spelCoord1][spelCoord2]
                    + (bump ? "u" : "")
                    + (scream ? "c" : "")
                    + (spelAlive ? "" : "k"));
            myAgent.send(reply);
            System.out.println("Env: " + reply.getContent()
                    + String.format("\t (%d,%d) -> %d", spelCoord1, spelCoord2, dir));
            if (!spelAlive)
                doDelete();
        }
    }

    private class ChangeWorldBehaviour extends OneShotBehaviour {

        ACLMessage message;

        public ChangeWorldBehaviour(ACLMessage message)
        {
            super();
            this.message = message;
        }

        public void action() {
            if (!spelAlive)
                throw new IllegalStateException("Агент уже мертв!");
            bump = false;
            scream = false;
            String content = message.getContent().toLowerCase();
            if (content.contains("forward")) {
                if ((dir == 0 && spelCoord2 == 3)
                        ||(dir == 2 && spelCoord2 == 0)
                        ||(dir == 1 && spelCoord1 == 3)
                        ||(dir == 3 && spelCoord1 == 0)) {
                    bump = true;
                    return;
                }
                switch (dir) {
                    case 0:
                        spelCoord2++;
                        break;
                    case 1:
                        spelCoord1++;
                        break;
                    case 2:
                        spelCoord2--;
                        break;
                    case 3:
                        spelCoord1--;
                        break;
                }
                if (WumpusWorldPercepts[spelCoord1][spelCoord2].contains("p")
                        || WumpusWorldPercepts[spelCoord1][spelCoord2].contains("w")) {
                    spelAlive = false;
                }

            }
            else if (content.contains("shoot")){
                if (arrowUsed)
                    throw new IllegalStateException("Agent don't have an arrow!");
                else {
                    arrowUsed = true;
                    if (dir == 1) {
                        if (2 > spelCoord2 && 0 == spelCoord1)
                            killed = true;
                    } else if (dir == 2) {
                        if (2 == spelCoord2 && 0 < spelCoord1)
                            killed = true;
                    } else if (dir == 3) {
                        if (2 < spelCoord2 && 0 == spelCoord1)
                            killed = true;
                    } else if (dir == 0) {
                        if (2 == spelCoord2 && 0 > spelCoord1)
                            killed = true;
                    }
                    if (killed) {
                        scream = true;
                    }
                }

            }
            else if (content.contains("climb")){
                doDelete();
            }
            else if (content.contains("grab")){
                if (spelCoord1 == 2 && spelCoord2 == 1) {
                    WumpusWorldPercepts[2][1] = changedGold;
                    goldTaken = true;
                }
                else
                    throw new IllegalStateException("Тут нет золота!");
            }
            else if (content.contains("right")){
                dir = (dir - 1) % 4;
            }
            else if (content.contains("left")){
                dir = (dir + 1) % 4;
            }
        }
    }

}
