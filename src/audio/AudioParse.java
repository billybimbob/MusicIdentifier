package audio;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;
import java.io.*;
import fourier.*;
import storage.*;

public class AudioParse {
    private static final int CHUNK_SIZE = 4096;
    private static final int[] BOUNDS = {40, 80, 120, 180, 300};

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
    private DataPoint[] keyPoints(final Complex[][] freqs, int songID, BufferedWriter writing) 
        throws IOException {
        final int LOWER_LIMIT = BOUNDS[0];
        final int UPPER_LIMIT = BOUNDS[BOUNDS.length-1];
        DataPoint[] points = new DataPoint[freqs.length];

        for (int i = 0; i < freqs.length; i++) {
            double[] highMags = new double[BOUNDS.length];
            double[] highFreqs = new double[BOUNDS.length];

            for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT; freq++) {
                int range = getRange(freq);
                double mag = Math.log(freqs[i][freq].abs() + 1);

                if (mag > highMags[range]) {
                    highMags[range] = mag;
                    highFreqs[range] = freq;
                }
            }
            for (double highFreq: highFreqs)
                writing.write(highFreq + "\t");
            writing.write("\n");

            points[i] = new DataPoint(songID, i, highFreqs);
        }
        return points;
    }

    public static void parseSong (SongMatches songSto, String path) {
        AudioParse audParse = new AudioParse();
        final int songID = songSto.getNextId();

        AudioInputStream stream = null;
        BufferedWriter bw = null;
        String fileName = null;
        try {
            if (path == null)
                stream = audParse.listen();
            else {
                File file = new File(path);
                stream = audParse.read(file);
                fileName = file.getName();
            }
            
            byte[] audio = audParse.toBytes(stream);
            //for (byte val: audio)
                //System.out.println(val);
            System.out.println("bytes " + audio.length);

            Complex[][] freqs = audParse.freqTrans(audio);
            System.out.println("length " + freqs.length);
            
            bw = new BufferedWriter(new FileWriter("data.txt"));
            final DataPoint[] pts = audParse.keyPoints(freqs, songID, bw);

            if (fileName == null) {
                int match = songSto.findMatch(pts);
                System.out.println("Best match is:" + match);
            } else {
                songSto.addPoints(pts, fileName);
                System.out.println("Successfully added data");
            }
            

        } catch (IOException e) {
            System.err.println("I/O problem: " + e);
            System.exit(-1);

        } catch (LineUnavailableException e) {
            System.err.println("No audio: " + e);
            System.exit(-1);

        } catch (NoSuchFieldException e) {
            System.err.println("No match found");

        } catch (UnsupportedAudioFileException e) {
            System.err.println("Error reading audio file ");
            e.printStackTrace();
            
        } finally {
            System.out.println("closing streams");
            closeStreams(stream, bw);
        }
        
    }

    private static void closeStreams(Closeable... streams) {
        for (Closeable stream: streams) {
            try {
                if (stream != null) stream.close();
            } catch(IOException e) {
                System.err.println("Issue closing " + e);
            }
        }
    }

    public static void main(String[] args) {
        //determine what to parse
        //parseSong(0, true);

        SongMatches matches = new SongMatches();
        parseSong(matches, args[0]);
        System.out.println("Stored info\n" + matches.toString());
    }
}