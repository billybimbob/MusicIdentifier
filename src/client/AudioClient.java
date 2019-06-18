package client;

import java.io.*;
import java.util.*;
import java.net.URL;
import audio.*;
import audio.storage.*;

class AudioClient {

    public static void parseSong(AudioParse parse, String path) {
        try {
            if (path.contains("http")) {
                URL url = new URL(path);
                parse.addSong(url);
            } else {
                File file = new File(path);
                parse.addSong(file);
            }
            System.out.println("Successfully added song");

        } catch (IOException e) {
            System.err.println("I/O issue " + e);
        }
    }
    public static void parseSong(AudioParse parse) {
        try {
            int match = parse.findSong();
            System.out.println("Best match: " + parse.getSongs().getName(match));

        } catch (NoSuchFieldException e) {
            System.out.println("No match found");

        } catch (IOException e) {
            System.err.println("I/O issue " + e);
        }
    }

    public static SongMatches setMatches() {
        try {
            return new SongMatches(new File("data.txt"));

        } catch (FileNotFoundException e) {
            System.err.println("File not found ");

        } catch (IOException e) {
            System.err.println("I/O issue " + e);
        }
        return new SongMatches();
    }

    public static void writeMatches(String info) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data.txt", false))) {
            System.err.println("Writing to data file");
            writer.write(info);

        } catch (IOException e) {
            System.err.println("Issue writing file " +e);
        }
    }

    public static void main(String[] args) {
        //determine what to parse

        AudioParse audio = new AudioParse(setMatches());

        //List<Thread> threads = new ArrayList<>();
        for (String path: args) {
            /*Thread thread = new Thread(new Runnable(){ //not sure
                @Override
                public void run() {
                    parseSong(audio, path);
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread: threads)
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("Error joining");
            }
        */
            parseSong(audio, path);
        }
        String info = audio.getSongs().toString();
        //System.out.println("Stored info\n" + info);
        writeMatches(info);

    }
}

