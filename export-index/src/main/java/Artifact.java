import org.apache.lucene.document.Document;

public class Artifact {
    private String sha;
    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String packaging;

    public Artifact(String sha, String groupId, String artifactId, String version, String classifier, String packaging) {
        this.sha = sha;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.packaging = packaging;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getSha() {
        return sha;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackaging() {
        return packaging;
    }

    public String toString() {
        return sha + " " + groupId + ":" + artifactId + ":" + version + ":" + packaging;
    }

    public boolean shouldHaveSha() {
        return (sha == null || sha.isEmpty()) && (packaging.equals("jar") || packaging.equals("pom"));
    }

    public boolean shouldBeIndexed() {
        // All that we don't need can be filtered out here
        return groupId != null && !classifier.equals("sources") && !classifier.equals("javadocs") && !classifier.equals("tests");
    }

    public static Artifact fromDocument(Document d) {
        String u = d.get("u");
        if (u != null) {
            String[] artifact = u.split("\\|");
            String groupId = artifact[0];
            String artifactId = artifact[1];
            String version = artifact[2];
            String classifier = artifact[3];
            String pkg = artifact[4];
            String sha = d.get("1");
            return new Artifact(sha, groupId, artifactId, version, classifier, pkg);
        }
        return new Artifact(null, null, null, null, null, null);
    }

}
