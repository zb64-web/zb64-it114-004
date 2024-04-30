package Project.Server;

import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.Player;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

public class ServerPlayer extends Player {
    private ServerThread client;

    public ServerPlayer(ServerThread t) {
        client = t;
        System.out.println(TextFX.colorize("Wrapped ServerThread " + t.getClientName(), Color.CYAN));
    }

    public long getClientId() {
        return client != null ? client.getClientId() : Constants.DEFAULT_CLIENT_ID;
    }

    public String getClientName() {
        return client != null ? client.getClientName() : "";
    }

    public void sendPhase(Phase phase) {
        if (client != null) {
            client.sendPhase(phase.name());
        }
    }

    public void sendReadyState(long clientId, boolean isReady) {
        if (client != null) {
            client.sendReadyState(clientId, isReady);
        }
    }

    public void sendPlayerTurnStatus(long clientId, boolean didTakeTurn) {
        if (client != null) {
            client.sendPlayerTurnStatus(clientId, didTakeTurn);
        }
    }

    public void sendResetLocalTurns() {
        if (client != null) {
            client.sendResetLocalTurns();
        }
    }

    public void sendResetLocalReadyState() {
        if (client != null) {
            client.sendResetLocalReadyState();
        }
    }

    public void sendCurrentPlayerTurn(long clientId) {
        if (client != null) {
            client.sendCurrentPlayerTurn(clientId);
        }
    }

    public void sendChoice(String clientChoice) {
        if (client != null) {
            client.sendChoice(clientChoice);
        }
    }

    public void sendRemoved(boolean isRemoved, long clientId) {
        if (client != null) {
            client.sendRemoved(isRemoved, clientId);
        }
    }
}
