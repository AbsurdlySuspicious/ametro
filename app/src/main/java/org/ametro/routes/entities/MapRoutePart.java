package org.ametro.routes.entities;

public class MapRoutePart {

    private final int from;
    private final int to;
    private final long delay;
    private final boolean transfer;

    public MapRoutePart(int from, int to, long delay, boolean transfer) {
        this.from = from;
        this.to = to;
        this.delay = delay;
        this.transfer = transfer;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public long getDelay() {
        return delay;
    }

    public boolean isTransfer() {return transfer; }
}

