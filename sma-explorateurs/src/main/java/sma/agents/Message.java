package sma.agents;

import sma.environnement.Case;

/**
 * Message envoyé par un agent communicant vers un agent cognitif.
 */
public class Message {

    public enum TypeMessage {
        TRESOR_TROUVE,
        ANIMAL_DETECTE,
        ZONE_EXPLOREE,
        AUCUN_TRESOR_ZONE
    }

    private final int expediteurId;
    private final TypeMessage type;
    private final Case position;      // Où se trouve l'info
    private final int zoneId;         // Zone concernée
    private final long timestamp;

    public Message(int expediteurId, TypeMessage type, Case position, int zoneId) {
        this.expediteurId = expediteurId;
        this.type = type;
        this.position = position;
        this.zoneId = zoneId;
        this.timestamp = System.currentTimeMillis();
    }

    public int getExpediteurId() {
        return expediteurId;
    }

    public TypeMessage getType() {
        return type;
    }

    public Case getPosition() {
        return position;
    }

    public int getZoneId() {
        return zoneId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Message[de=%d, type=%s, zone=%d]", expediteurId, type, zoneId);
    }
}
