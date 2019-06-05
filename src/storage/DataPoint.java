package storage;

public class DataPoint {
    private int songID;
    private int time;
    private double[] keyPts;
    private static final int FUZ_FACTOR = 2;

    public DataPoint (int songID, int time, double[] keyPts) {
        this.songID = songID;
        this.time = time;
        this.keyPts = keyPts;
    }
    public DataPoint (int time, double[] keyPts) { //songID undef not sure if to keep
        this.songID = -1;
        this.time = time;
        this.keyPts = keyPts;
    }

    public int getID() {
        return this.songID;
    }
    public int getTime() {
        return this.time;
    }

    @Override
    public int hashCode() {
        int ret = 0;
        for (int i = keyPts.length-1; i >= 0; i--)
            ret += (keyPts[i]-(keyPts[i]%FUZ_FACTOR)) * Math.pow(10, i*2);

        return ret;
    }
    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        else if(other.getClass() != this.getClass())
            return false;
        else {
            DataPoint comp = (DataPoint)other;
            return this.songID==comp.songID && this.time==comp.time;
        }
    }
}