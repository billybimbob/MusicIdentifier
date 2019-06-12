package audio;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;
import java.io.*;
import fourier.*;
import storage.*;

public class AudioParse {
    private static final int CHUNK_SIZE = 4096;
    private static final int[] BOUNDS = {40, 80, 120, 180, 300};
    private SongMatches songs;
    private int match;

    public AudioParse() {
        this.match = -1;
    }
    public AudioParse(SongMatches songs) {
        this.match = -1;
        this.songs = songs;
    }

    public SongMatches getSongs() { //might want to copy
        return songs;
    }
    public int getMatch() {
        return match;
    }


    /*
     * private helper functions
     */

    private AudioFormat defaultFormat() {
        float sampleRate = 8000.0f;
        int sampleSize = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }
    private AudioFormat decodeFormat(AudioFormat copy) {
        Encoding encode = Encoding.PCM_SIGNED;
        float sampleRate = copy.getSampleRate();
        int sampleSize = 16;
        int channels = copy.getChannels();
        int frameSize = copy.getChannels()*2;
        float frameRate = copy.getSampleRate();
        boolean bigEndian = false;
        return new AudioFormat(encode, sampleRate, sampleSize, channels, frameSize, frameRate, bigEndian);
    }

    private AudioInputStream listen() throws LineUnavailableException {
        final AudioFormat format = defaultFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        return new AudioInputStream(line);
    }

    private AudioInputStream read(File file) 
        throws UnsupportedAudioFileException, IOException {

        System.out.println("File ob " + file.getName() + " " + file.getPath());
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioFormat decodeForm = decodeFormat(in.getFormat());
        AudioInputStream decodeIn = AudioSystem.getAudioInputStream(decodeForm, in);
        return decodeIn;
        //PCMtoPCMCodec convert = new PCMtoPCMCodec(); //not sure if needed
        //return convert.getAudioInputStream(defaultFormat(), decodeIn);
    }

    private byte[] toBytes(AudioInputStream line) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buffer = new byte[(int)1024];
        boolean running = true;
        int loops = 0;
        while(running && loops < 1000) { //can change condition, like for mic
            int count = line.read(buffer);
            if (count > 0) {
                out.write(buffer, 0, count);
                loops++;
            } else
                running = false;
        }
        out.close();
        System.out.println("Loops "+ loops);
        return out.toByteArray();
        //return line.readAllBytes();
    }

    private Complex[][] freqTrans(final byte[] data) { //trans time domain to freq
        final int numChunks = data.length/CHUNK_SIZE;
        Complex [][] freqs = new Complex[numChunks][];
        
        for(int chunk = 0; chunk < numChunks; chunk++) {
            Complex[] set = new Complex[CHUNK_SIZE];
            for(int offset = 0; offset < CHUNK_SIZE; offset++)
                set[offset] = new Complex(data[chunk*CHUNK_SIZE + offset], 0);
            
            freqs[chunk] = FourierTrans.fft(set);
        }

        return freqs;
    }

    private int getRange(final int freq) { //could change to binary search
        int idx = 0;
        while(idx < BOUNDS.length && BOUNDS[idx] > freq)
            idx++;
        
        return idx;
    }

    //writes to input writer file
    private DataPoint[] keyPoints(final Complex[][] freqs) throws IOException {
        final int LOWER_LIMIT = BOUNDS[0];
        final int UPPER_LIMIT = BOUNDS[BOUNDS.length-1];
        DataPoint[] points = new DataPoint[freqs.length];

        for (int i = 0; i < freqs.length; i++) {
            double[] highMags = new double[BOUNDS.length];
            double[] highFreqs = new double[BOUNDS.length];
            final int time = i;

            for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT; freq++) {
                int range = getRange(freq);
                double mag = Math.log(freqs[i][freq].abs() + 1);

                if (mag > highMags[range]) {
                    highMags[range] = mag;
                    highFreqs[range] = freq;
                }
            }
            points[i] = new DataPoint(time, highFreqs);
        }
        return points;
    }

    private DataPoint[] parseAudio (AudioInputStream stream) throws IOException {
        byte[] audio = toBytes(stream);
        stream.close();
        System.out.println("bytes " + audio.length);

        Complex[][] freqs = freqTrans(audio);
        System.out.println("length " + freqs.length);
        
        return keyPoints(freqs);

    }


    /*
     * methods to find and add songs
     */

    public int findSong () throws IOException, NoSuchFieldException {
        try {
            AudioInputStream stream = listen();
            DataPoint[] pts = parseAudio(stream);
            return songs.findMatch(pts);
        } catch (LineUnavailableException e) {
            throw new IOException("Line Unavailable");
        }
    }
    public void addSong (String path) throws IOException { //need to look at structure of this metho
        try {
            final int songID = songs.getNextId();
            File file = new File(path);
            AudioInputStream stream = read(file);
            String fileName = file.getName();
            
            DataPoint[] pts = parseAudio(stream);
            for(DataPoint pt: pts)
                pt.setID(songID);

            songs.addPoints(pts, fileName);
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Cannot Read File");
        }
    }


    /*
    private static void closeStreams(Closeable... streams) {
        for (Closeable stream: streams) {
            try {
                if (stream != null) stream.close();
            } catch(IOException e) {
                System.err.println("Issue closing " + e);
            }
        }
    }*/

    public static void parseSong(boolean adding, String path) {
        //call addSong or findSong
    }

    public static SongMatches setMatches() {
        try {
            return new SongMatches(new File("data.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("File not found ");
        } catch (IOException e) {
            System.out.println("I/O issue " + e);
        }
        return new SongMatches();
    }
    public static void writeMatches(String info) {
        try (BufferedWriter write = new BufferedWriter(new FileWriter("data.txt", false))) {
            System.out.println("Writing to data file");
            write.write(info);

        } catch (IOException e) {
            System.out.println("Issue writing file " +e);
        }
    }

    public static void main(String[] args) {
        //determine what to parse

        AudioParse audio = new AudioParse(setMatches());
        //audio.parseSong(args[0]);
        String info = audio.getSongs().toString();
        //System.out.println("Stored info\n" + info);
        writeMatches(info);

    }
}