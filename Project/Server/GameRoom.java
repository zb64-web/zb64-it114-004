package Project.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.TextFX;
import Project.Common.TimedEvent;
import Project.Common.TextFX.Color;

public class GameRoom extends Room {

    private ConcurrentHashMap<Long, ServerPlayer> players = new ConcurrentHashMap<Long, ServerPlayer>();

    private TimedEvent readyCheckTimer = null;
    private TimedEvent turnTimer = null;
private TimedEvent playerDecisionTimer = null;
    private TimedEvent decisionCheckTimer = null;
    //zb64 4/3/24
    private Phase currentPhase = Phase.READY;
    private long numActivePlayers = 0;
    private boolean canEndSession = false;
    private ServerPlayer currentPlayer = null;
    private List<Long> turnOrder = new ArrayList<Long>(); 

    public GameRoom(String name) {
        super(name);
    }

    @Override
    protected synchronized void addClient(ServerThread client) {
        super.addClient(client);
        if (!players.containsKey(client.getClientId())) {
            ServerPlayer sp = new ServerPlayer(client);
            players.put(client.getClientId(), sp);
            System.out.println(TextFX.colorize(client.getClientName() + " join GameRoom " + getName(), Color.WHITE));

            // sync game state

            // sync phase
            sp.sendPhase(currentPhase);
            // sync ready state
            players.values().forEach(p -> {
                sp.sendReadyState(p.getClientId(), p.isReady());
                sp.sendPlayerTurnStatus(p.getClientId(), p.didTakeTurn());
            });
            if (currentPlayer != null) {
                sp.sendCurrentPlayerTurn(currentPlayer.getClientId());
            }

        }
    }

    @Override
    protected synchronized void removeClient(ServerThread client) {
        super.removeClient(client);
        // Note: base Room can close (if empty) before GameRoom cleans up (possibly)
        if (players.containsKey(client.getClientId())) {
            players.remove(client.getClientId());
            System.out.println(TextFX.colorize(client.getClientName() + " left GameRoom " + getName(), Color.WHITE));
            // update active players in case an active player left
            numActivePlayers = players.values().stream().filter(ServerPlayer::isReady).count();
        }
    }

    // serverthread interactions
    public synchronized void setReady(ServerThread client) {
        if (currentPhase != Phase.READY) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Can't initiate ready check at this time");
            return;
        }
        long playerId = client.getClientId();
        if (players.containsKey(playerId)) {
            players.get(playerId).setReady(true);
            syncReadyState(players.get(playerId));
            System.out.println(TextFX.colorize(players.get(playerId).getClientName() + " marked themselves as ready ",
                    Color.YELLOW));
            readyCheck();
        } else {
            System.err.println(TextFX.colorize("Player doesn't exist: " + client.getClientName(), Color.RED));
        }
    }

    public synchronized void doTurn(ServerThread client, String choice) {
        if (currentPhase != Phase.TURN) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, "You can't do turns just yet");
            return;
        }
        // implementation 1
        long clientId = client.getClientId();
        if (players.containsKey(clientId)) {
            ServerPlayer sp = players.get(clientId);
            //they can only participate if they're ready 
            if(!sp.isReady()){
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Sorry you weren't ready in time");
                return;
            }
            //zb64 4/8/2024
            // player can only update their turn "actions once" 
            System.out.println(choice);
            if(!sp.didTakeTurn()) {
                sp.setTakenTurn(true); 
                sp.setChoice(choice);
                sp.sendChoice(choice);
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s completed their turn ", sp.getClientName()));
                syncUserTookTurn(sp);
            } 
            else {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Your Turn has already been Completed Please Wait");
            }
        }

    }
    // end serverthread interactions


    private synchronized void readyCheck() {

        if (readyCheckTimer == null) {
            readyCheckTimer = new TimedEvent(30, () -> {
                long numReady = players.values().stream().filter(ServerPlayer::isReady).count();
                // condition 1: start if we have the minimum ready
                boolean meetsMinimum = numReady >= Constants.MINIMUM_REQUIRED_TO_START;
                // condition 2: start if everyone is ready
                int totalPlayers = players.size();
                boolean everyoneIsReady = numReady >= totalPlayers;
                if (meetsMinimum || everyoneIsReady) {
                    start();
                } else {
                    sendMessage(ServerConstants.FROM_ROOM,
                            "Minimum players not met during ready check, please try again");
                    // added after recording as I forgot to reset the ready check
                    players.values().forEach(p -> {
                        p.setReady(false);
                        syncReadyState(p);
                    });
                }
                readyCheckTimer.cancel();
                readyCheckTimer = null;
readyDecision();
            });
            readyCheckTimer.setTickCallback((time) -> System.out.println("Ready Countdown: " + time));
        }
    }

    private synchronized void readyDecision() {
        if (decisionCheckTimer == null) {
            decisionCheckTimer = new TimedEvent(15, () -> {
                decisionCheckTimer.cancel();
                decisionCheckTimer = null;
            });
            decisionCheckTimer.setTickCallback((time) -> System.out.println("Player Decision Countdown " + time));
        }
    }
    //zb64 4/3/24

    private void handleEndOfDecisionTime() {
        if (currentPlayer != null) {
            playerDecisionTimer.cancel();
            playerDecisionTimer = null;
        }
        boolean allPlayersDecided = players.values().stream().allMatch(ServerPlayer::didTakeTurn);
        if (allPlayersDecided) {
            handleEndOfTurn();
            nextTurn();
            startPlayerDecisionTimer();
            readyDecision();
        } else {
            System.out.println("Not all players have made their deision yet.");
        }
    }
    //zb64 4/3/24

    private void check() {
        int remainingPlayers = (int) players.values().stream().filter(p -> !p.didTakeTurn()).count();
        if (remainingPlayers == 0) {
            System.out.println("It's a tie!");
            resetTurns();
            return;
        } else if (remainingPlayers == 1) {
            System.out.println("You won!");
            end();
            return;
        } else {
            List<String> choice = players.values().stream().filter(p -> p.didTakeTurn()).map(p -> p.getChoice).collect(Collectors.toList());
            boolean hasDuplicates = choices.stream().distinct().count() < choices.size();

            if(hasDuplicates) {
                System.out.println("Eliminating players with duplicate choices");
            }
            players.values().stream().filter(p -> !p.didTakeTurn()).forEach(p -> p.setChoice(null));
            startChoicePhase();
        }
        int rand = (int)(Math.random() * 3);
        String playerMove = "";
        if (rand == 0) {
            playerMove = "rock";
        } else if (rand == 1) {
            playerMove = "paper";
        } else {
            playerMove = "scissors";
        }
        if (player.equals(playerMove)) {
            System.out.println("You tied!");
        } else if ((player.equals("rock") && playerMove.equals("scissors")) || 
                   (player.equals("scissors") && playerMove.equals("paper")) || 
                   (player.equals("paper") && playerMove.equals("rock"))) {
            System.out.println("You won!");
        } else {
            System.out.println("You lost!");
        }
    }
    //zb64 4/9/24

    private void startChoicePhase() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startChoicePhase'");
    }

    private void changePhase(Phase incomingChange) {
        if (currentPhase != incomingChange) {
            currentPhase = incomingChange;
            syncCurrentPhase();
        }
    }

    private void start() {
        if (currentPhase != Phase.READY) {
            System.err.println("Invalid phase called during start()");
            return;
        }
        canEndSession = false;
        changePhase(Phase.TURN);
        numActivePlayers = players.values().stream().filter(ServerPlayer::isReady).count();
setupTurns();
        startTurnTimer();
    startPlayerDecisionTimer();
    }

    private void startPlayerDecisionTimer() {
        if (playerDecisionTimer != null) {
            playerDecisionTimer.cancel();
            playerDecisionTimer = null;
        }
        if (playerDecisionTimer == null) {
            playerDecisionTimer = new TimedEvent(15, this::handleEndOfDecisionTime);
            playerDecisionTimer.setTickCallback(this::checkEarlyEndTurn);
            sendMessage(ServerConstants.FROM_ROOM, "Make a decision");
        }
    }
    //zb64 4/3/24

    //private void makeChoices() {
        //int c [] = {};
        //for (int i = 0; i < c.length; i++) {
       // }
    //}
    //zb64 4/8/24

    static void recordPlayerChoice(ServerThread player, String choice) {
        long playerId = player.getClientId();
        if (players.containsKey(playerId)) {
            ServerPlayer sp = players.get(playerId);
            sp.setChoice(choice);
            player.sendMessage(Constants.DEFAULT_CLIENT_ID, "Your choice " + choice + " has been recorded.");
            System.out.println(TextFX.colorize("Player " + player.getClientName() + " recorded choice: " + choice, Color.PURPLE));
        }
    }
    //zb64 4/8/24


    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        if (turnTimer == null) {
            // turnTimer = new TimedEvent(60, ()-> {handleEndOfTurn();});
            turnTimer = new TimedEvent(60, this::handleEndOfTurn);
            turnTimer.setTickCallback(this::checkEarlyEndTurn);
            sendMessage(ServerConstants.FROM_ROOM, "Pick /R, /P, or /S for rock paper scissor");
        }
    }


    


    private void checkEarlyEndTurn(int timeRemaining) {
        // implementation 1 zb64, 4/5/2024
        
          long numEnded =
          players.values().stream().filter(ServerPlayer::didTakeTurn).count();
          if (numEnded >= numActivePlayers) {
          // end turn early
          handleEndOfTurn();
          }
         


    }


    private void handleEndOfTurn() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        System.out.println(TextFX.colorize("Handling end of turn", Color.YELLOW));
        // option 1 - if they can only do a turn when ready
        List<ServerPlayer> playersToProcess = players.values().stream().filter(player -> player.didTakeTurn() && player.getChoice() != null).toList();
        // option 2 - double check they are ready and took a turn
        // List<ServerPlayer> playersToProcess =
        // players.values().stream().filter(sp->sp.isReady() &&
        // sp.didTakeTurn()).toList();
        playersToProcess.forEach(p -> {
            sendMessage(ServerConstants.FROM_ROOM, String.format("%s did something for the game", p.getClientName()));
        });

        // TODO end game logic
        //zb64 4/7/24
        



        if (new Random().nextInt(101) <= 30) {
            canEndSession = true;
            // simulate end game
            end();
        } else {
            resetTurns();
            //implementation 2
            //end of implementation 2
            startTurnTimer(); 
        }
    }

    private void resetTurns() {
        players.values().stream().forEach(p -> p.setTakenTurn(false));
        sendResetLocalTurns();
    }

    private void end() {
        System.out.println(TextFX.colorize("Doing game over", Color.YELLOW));
        turnOrder.clear();
        // mark everyone not ready
        players.values().forEach(p -> {
            // TODO fix/optimize, avoid nested loops if/when possible
            p.setReady(false);
            p.setTakenTurn(false);
            // reduce being wasteful
            // syncReadyState(p);
        });
        // depending if this is not called yet, we can clear this state here too
        sendResetLocalReadyState();
        sendResetLocalTurns();
        changePhase(Phase.READY);
        sendMessage(ServerConstants.FROM_ROOM, "Session over!");
        // TODO, eventually will be more optimal to just send that the session ended

    }

    // start send/sync methods



    private void sendResetLocalReadyState() {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendResetLocalReadyState();
        }
    }

    private void sendResetLocalTurns() {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendResetLocalTurns();
        }
    }

    private void syncUserTookTurn(ServerPlayer isp) {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendPlayerTurnStatus(isp.getClientId(), isp.didTakeTurn());
        }
    }
    private void syncCurrentPhase() {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendPhase(currentPhase);
        }
    }

    private void syncReadyState(ServerPlayer csp) {
        Iterator<ServerPlayer> iter = players.values().iterator();
        while (iter.hasNext()) {
            ServerPlayer sp = iter.next();
            sp.sendReadyState(csp.getClientId(), csp.isReady());
        }
    }
    // end send/sync methods
}
