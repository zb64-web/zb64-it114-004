package Project.Server;

import java.util.ArrayList;
//import java.util.Collections;
import java.util.Iterator;
import java.util.List;
//import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;

import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.TextFX;
import Project.Common.TimedEvent;
import Project.Common.TextFX.Color;

public class GameRoom extends Room {

    private static ConcurrentHashMap<Long, ServerPlayer> players = new ConcurrentHashMap<Long, ServerPlayer>();

    private TimedEvent readyCheckTimer = null;
    private TimedEvent turnTimer = null;
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
            System.out.println(TextFX.colorize(client.getClientName() + " became spectator " + getName(), Color.WHITE));
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
            System.out.println(TextFX.colorize(players.get(playerId).getClientName() + " marked themselves as ready ",Color.YELLOW));
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
            if (!sp.isReady()) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Sorry, you have been eliminated or are not ready");
                return;
            }
            if (sp.getPreviousChoice() == choice) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Sorry, you have already made a choice.");
                return;
            }
            if (sp.didTakeTurn()) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Your turn has already been completed. Please wait.");
                return;
            }
            //zb64 4/8/2024
            // player can only update their turn "actions once"
            System.out.println(choice);
            if(!sp.didTakeTurn()) {
                sp.setTakenTurn(true);
                sp.setChoice(choice);
                sp.sendChoice(choice);
                sp.setPreviousChoice(choice);
                sendMessage(ServerConstants.FROM_ROOM, String.format("%s completed their turn ", sp.getClientName()));
                syncUserTookTurn(sp);

                if(choice.equalsIgnoreCase("skip")) {
                    proceedToNextPlayerTurn();
                }
            } else {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Your turn has already been completed. Please wait.");
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
                //readyDecision();
            });
            readyCheckTimer.setTickCallback((time) -> System.out.println("Ready Countdown: " + time));
        }
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
        changePhase(Phase.TURN);
        canEndSession = false;
        numActivePlayers = players.values().stream().filter(player -> player.isReady() == true && player.getRemoved() == false).count();
        startTurnTimer();
    }

    private void startTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        if (turnTimer == null) {
            // turnTimer = new TimedEvent(60, ()-> {handleEndOfTurn();});
            turnTimer = new TimedEvent(15, this::handleEndOfTurn);
            turnTimer.setTickCallback(this::checkEarlyEndTurn);
            sendMessage(ServerConstants.FROM_ROOM, "Pick /R, /P, /S, or /skip for rock paper scissor or to skip turn");
        } //zb64 4/27/24
    }


    private void checkEarlyEndTurn(int timeRemaining) {
        long numEnded = players.values().stream().filter(ServerPlayer::didTakeTurn).count();
        if (numEnded >= numActivePlayers) {
            // end turn early
            handleEndOfTurn();
        }
    }

    public void proceedToNextPlayerTurn() {
        int currentIndex = turnOrder.indexOf(currentPlayer.getClientId());
        int nextIndex = currentIndex + 1;

        while (!players.get(turnOrder.get(nextIndex)).isReady() || players.get(turnOrder.get(nextIndex)).getRemoved()) {
            nextIndex = (nextIndex + 1) % turnOrder.size();
        }

        long nextPlayerId = turnOrder.get(nextIndex);
        currentPlayer = players.get(nextPlayerId);
        currentPlayer.sendCurrentPlayerTurn(nextPlayerId);
        startTurnTimer();
    }//zb64 4/29/24


    private void handleEndOfTurn() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
        System.out.println(TextFX.colorize("Handling end of turn", Color.YELLOW));
        // option 1 - if they can only do a turn when ready
        List<ServerPlayer> playersToProcess = players.values().stream().filter(player -> !player.getRemoved() && player.didTakeTurn() && player.getChoice() != null).toList();        
        // players.values().stream().filter(sp->sp.isReady() &&
        // sp.didTakeTurn()).toList();
        /*playersToProcess.forEach(p -> {
            sendMessage(ServerConstants.FROM_ROOM, String.format("%s did something for the game", p.getClientName()));
        });*/
        // TODO end game logic

        boolean hasSkip = playersToProcess.stream().anyMatch(player -> player.getChoice().equals("skip"));
        if (hasSkip) {
            proceedToNextPlayerTurn();
            sendMessage(ServerConstants.FROM_ROOM, "At least one player has chosen to skip their turn. Proceeding...");
        }
        //zb64 4/28/24

        for (int i = 0; i < playersToProcess.size(); i++) {
            ServerPlayer p1 = playersToProcess.get(i);
            int next = (i + 1) % playersToProcess.size();
            ServerPlayer p2 = playersToProcess.get(next);
            String p1Choice = p1.getChoice();
            String p2Choice = p2.getChoice();

            if ((p1Choice.equals("R") && p2Choice.equals("S"))) {
                p2.sendRemoved(hasSkip, i);
                p2.setRemoved(true);
                sendMessage(ServerConstants.FROM_ROOM, String.format(p1.getClientName() + " has chosen " + p1Choice + " and " + p2.getClientName() + " has chosen " + p2Choice+ " and lost"));
            } else if (p1Choice.equals("S") && p2Choice.equals("P")) {
                p2.sendRemoved(hasSkip, i);
                p2.setRemoved(true);
                sendMessage(ServerConstants.FROM_ROOM, String.format(p1.getClientName() + " has chosen " + p1Choice + " and " + p2.getClientName() + " has chosen " + p2Choice+ " and lost"));
            } else if (p1Choice.equals("P") && p2Choice.equals("R")) {
                p2.sendRemoved(hasSkip, i);
                p2.setRemoved(true);
               sendMessage(ServerConstants.FROM_ROOM, String.format(p1.getClientName() + " has chosen " + p1Choice + " and " + p2.getClientName() + " has chosen " + p2Choice+ " and lost"));
            }
        }


        List<ServerPlayer> remainingPlayers = players.values().stream().filter(player -> !player.getRemoved() && player.getChoice() == null).toList();
        sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(remainingPlayers.size() + " left", Color.YELLOW));
        if (remainingPlayers.size() == 1) {
            ServerPlayer winner = remainingPlayers.get(0);
            sendMessage(ServerConstants.FROM_ROOM, TextFX.colorize(winner.getClientName() + " won!", Color.BLUE));
            end();
            start();
        } else if (remainingPlayers.size() > 1) {
            sendMessage(ServerConstants.FROM_ROOM, "More than 1 player remains");
            end();
            start();
        } else {
            sendMessage(ServerConstants.FROM_ROOM, "It's a tie!");
            resetTurns();
            start();
        }
        //zb64 4/29/24


        players.values().forEach(player -> {
            player.setRemoved(false);
            player.setReady(false);
            resetTurns();
        });
    } //zb64 4/7/24

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
