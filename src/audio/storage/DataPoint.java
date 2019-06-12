package audio.storage;

public class DataPoint {
    private int songID;
    private int time;
    private int hash;
    private static final int FUZ_FACTOR = 2;

    public DataPoint (int songID, int time, double[] keyPts) {
        this.songID = songID;
        this.time = time;
        this.setHash(keyPts);
    }
    public DataPoint (int songID, int time, int hash) {
        this.songID = songID;
        this.time = time;
        this.hash = hash;
    }
    public DataPoint (int time, double[] keyPts) { //songID undef
        this.songID = -1;
        this.time = time;
        this.setHash(keyPts);
    }

    public int getID() {
        return this.songID;
    }
    public int getTime() {
        return this.time;
    }

    public void setID(int ID) {
        this.songID = ID;
    }
    private void setHash(double[] keyPts) {
        this.hash = 0;
        for (int i = keyPts.length-1; i >= 0; i--)
            this.hash += (keyPts[i]-(keyPts[i]%FUZ_FACTOR)) * Math.pow(10, i*2);
    }

    @Override
    public int hashCode() { //will always be the same
        return this.hash;
    }
    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        else if (other.getClass() != this.getClass())
            return false;
        else {
            DataPoint comp = (DataPoint)other;
            return this.time==comp.time && this.hashCode() == comp.hashCode(); //id might be wrong
        }
    }

    @Override
    public String toString() {
        return time + ": " + hash;
    }
}