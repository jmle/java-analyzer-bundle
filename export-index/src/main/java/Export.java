import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeSet;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Export {

    // TODO: move to an arg
    private static String REPOSITORY_URL = "https://repo1.maven.org/maven2";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        
        if ("export".equals(command)) {
            if (args.length < 2) {
                System.err.println("Error: export command requires lucene-index-path argument");
                printUsage();
                System.exit(1);
            }
            exportData(args[1]);
        } else if ("index".equals(command)) {
            if (args.length < 3) {
                System.err.println("Error: index command requires data-file and index-file arguments");
                printUsage();
                System.exit(1);
            }
            IndexBuilder.buildIndex(args[1], args[2]);
            System.out.println("Index built successfully!");
        } else {
            System.err.println("Error: Unknown command '" + command + "'");
            printUsage();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar export-index-1.0-SNAPSHOT.jar export <lucene-index-path>");
        System.err.println("    - Creates maven.default.index from Lucene index");
        System.err.println("  java -jar export-index-1.0-SNAPSHOT.jar index <data-file> <index-file>");
        System.err.println("    - Creates binary search index from data file");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java -jar export-index-1.0-SNAPSHOT.jar export /path/to/lucene/index");
        System.err.println("  java -jar export-index-1.0-SNAPSHOT.jar index maven.default.index maven.default.idx");
    }

    private static void exportData(String luceneIndexPath) throws Exception {
        Path path = Paths.get(luceneIndexPath);
        Directory index = FSDirectory.open(path);
        IndexReader reader = DirectoryReader.open(index);
        StoredFields storedFields = reader.storedFields();

        TreeSet<String> artifacts = new TreeSet<String>();

        int num = reader.numDocs();
        for (int i = 0; i < num; i++) {
            Document d = storedFields.document(i);
            Artifact artifact = Artifact.fromDocument(d);
            if (artifact.shouldBeIndexed()) {
                if (artifact.shouldHaveSha()) {
                    try {
                        artifact.setSha(getJarSha1(REPOSITORY_URL, artifact));
                    } catch (IOException e) {
                        System.out.println("Could not get sha for " + artifact);
                        continue;
                    }
                }
                artifacts.add(artifact.toString());
//                System.out.println("Added artifact: " + artifact);
            }
        }

        BufferedWriter out = new BufferedWriter(new FileWriter("maven.default.index"));
        for (String artifact : artifacts) {
            out.write(artifact);
            out.newLine();
        }

        out.close();
        reader.close();
        System.out.println("Data exported successfully to maven.default.index");
    }

    /**
     * Retrieve SHA1 for artifacts that don't have them
     *
     * @param repositoryUrl
     * @param artifact
     * @return
     * @throws IOException
    {}*/
    public static String getJarSha1(String repositoryUrl, Artifact artifact) throws IOException {
        // e.g. https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-web/2.3.2.RELEASE/spring-boot-starter-web-2.3.2.RELEASE-javadoc.jar.sha1
        final String sha1FileUrl = new StringBuilder(repositoryUrl)
                .append("/").append(artifact.getGroupId().replace('.', '/'))
                .append("/").append(artifact.getArtifactId())
                .append("/").append(artifact.getVersion())
                .append("/").append(artifact.getArtifactId()).append("-").append(artifact.getVersion()).append(".jar.sha1").toString();
        final URL url = new URL(sha1FileUrl);
        InputStreamReader in1 = new InputStreamReader(url.openStream());
        final BufferedReader in = new BufferedReader(in1);
        // the hash sha1 file should be always a 1 line text file
        final String sha1 = in.readLine();
        in.close();
        // check the hash has the expected length
        if (!(sha1 != null && sha1.length() == 40)) {
            throw new IOException("Could not retrieve SHA1 for " + artifact);
        }
        System.out.println("Retrieved SHA1 for " + artifact);
        return sha1;
    }
}
