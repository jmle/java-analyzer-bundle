import org.apache.lucene.document.Document;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Artifact {

    private static final Set<String> SKIPPED_CLASSIFIERS =
            new HashSet<>(Arrays.asList(("javadoc javadocs docs groovydoc site"
                    + " source sources src source-release project-src gf-project-src"
                    + " test tests test-sources tests-sources test-javadoc tests-javadoc"
                    + " maven-archetype maven-plugin"
                    + " bin app bundle image dist distribution assembly resources scripts"
                    + " module kubernetes openshift helm").split(" ")));

    private final Set<String> SKIPPED_PACKAGINGS  = new HashSet<>(Arrays.asList(
            ("png eclipse-repository xhtml ${packaging.type} ${lifecycle} ${packaging}"
                    + " jbi-service-unit eclipse-test-plugin atlassian-plugin sh cfg list tree jszip"
                    + " pdf eclipse-feature eclipse-plugin swf jangaroo swc html xsd txt apk jdocbook nexus-plugin"
                    + " sonar-plugin nbm yml maven-archetype maven-plugin").split(" ")));

    private final Set<String> SKIP_VERSIONS_CONTAINING  = new HashSet<>(Arrays.asList(
            "rc alpha beta snap [-,\\.]cr [-,\\.]pre".split(" ")));

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
        return groupId != null
                && !SKIPPED_CLASSIFIERS.contains(classifier)
                && !SKIPPED_PACKAGINGS.contains(packaging)
                && SKIP_VERSIONS_CONTAINING.stream().noneMatch(ver -> version.matches(String.format("(?i).*%s.*", ver)));
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
