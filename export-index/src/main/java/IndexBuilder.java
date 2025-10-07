import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Java implementation of the Maven Search Index Builder
 * Creates a binary index file compatible with the Go search implementation
 */
public class IndexBuilder {

    // Must match the KeySize constant in the Go implementation
    private static final int KEY_SIZE = 40;
    private static final int ENTRY_SIZE = KEY_SIZE + 8 + 8; // key + offset + length

    /**
     * Builds a binary search index from a text data file
     *
     * @param dataFilePath path to the input text file containing key-value pairs
     * @param indexFilePath path where the binary index will be written
     * @throws IOException if file operations fail
     */
    public static void buildIndex(String dataFilePath, String indexFilePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(dataFilePath));
             FileOutputStream fos = new FileOutputStream(indexFilePath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            String line;
            long offset = 0;

            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    offset += line.length() + 1; // +1 for newline
                    continue;
                }

                // Split line into key and value
                String[] parts = line.split(" ", 2);
                if (parts.length != 2) {
                    offset += line.length() + 1;
                    continue; // Skip malformed lines
                }

                String key = parts[0];
                long lineLength = line.length() + 1; // +1 for newline

                // Create binary entry
                byte[] entry = createIndexEntry(key, offset, lineLength);
                bos.write(entry);

                offset += lineLength;
            }
        }
    }

    /**
     * Creates a binary index entry matching the Go implementation format
     *
     * @param key the search key
     * @param offset byte offset of the line in the data file
     * @param length length of the line in the data file
     * @return binary representation of the index entry
     */
    private static byte[] createIndexEntry(String key, long offset, long length) {
        ByteBuffer buffer = ByteBuffer.allocate(ENTRY_SIZE);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Match Go's binary.LittleEndian

        // Write key (fixed size, null-padded)
        byte[] keyBytes = new byte[KEY_SIZE];
        byte[] sourceKeyBytes = key.getBytes();
        System.arraycopy(sourceKeyBytes, 0, keyBytes, 0,
                Math.min(sourceKeyBytes.length, KEY_SIZE));
        buffer.put(keyBytes);

        // Write offset and length as little-endian 64-bit integers
        buffer.putLong(offset);
        buffer.putLong(length);

        return buffer.array();
    }

    /**
     * Command-line interface for the index builder
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java IndexBuilder <data-file> <index-file>");
            System.err.println("  data-file:  Input text file with key-value pairs");
            System.err.println("  index-file: Output binary index file");
            System.exit(1);
        }

        String dataFile = args[0];
        String indexFile = args[1];

        try {
            System.out.printf("Building index from %s to %s...%n", dataFile, indexFile);
            long startTime = System.currentTimeMillis();

            buildIndex(dataFile, indexFile);

            long endTime = System.currentTimeMillis();
            System.out.printf("Index built successfully in %d ms!%n", endTime - startTime);

            // Print some statistics
            File index = new File(indexFile);
            long numEntries = index.length() / ENTRY_SIZE;
            System.out.printf("Created %d index entries (%d bytes)%n", numEntries, index.length());

        } catch (IOException e) {
            System.err.printf("Failed to build index: %s%n", e.getMessage());
            System.exit(1);
        }
    }
}

