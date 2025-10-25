package com.ecosys

@Grab(group='org.apache.maven', module='maven-artifact', version='3.3.3')
@Grab(group='org.jsoup', module='jsoup', version='1.18.3')

import org.jsoup.Jsoup
import groovy.json.JsonSlurper
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import com.ecosys.SQLiteAsdbRecord
import org.apache.maven.artifact.versioning.ComparableVersion

import groovy.transform.TypeChecked
import groovy.transform.CompileStatic


@CompileStatic
@TypeChecked
class SQLiteAsdbDataFetcher {

    private static final String ASDB_MIN_VERSION = "5.7.0.0"
    private static final String ASDB_DOWNLOAD_CENTER_URL = "https://download.aerospike.com/artifacts/aerospike-server-enterprise/"
    private static final String ASDB_DOCKERHUB_URL = "https://hub.docker.com/v2/repositories/aerospike/aerospike-server/tags?page_size=1024"

    // Retry configuration
    private static final int MAX_RETRIES = 3
    private static final int RETRY_DELAY_MS = 5000 // 5 seconds
    private static final int TIMEOUT_MS = 30000 // 30 seconds

    @NonCPS
    public static String[] fetchAvailableAsdbVersionsFromDownloadCenter() {
        Exception lastException = null

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                println "Attempting to connect to download center (attempt ${attempt}/${MAX_RETRIES})"

                Document doc = Jsoup.connect(ASDB_DOWNLOAD_CENTER_URL)
                        .timeout(TIMEOUT_MS)
                        .followRedirects(true)
                        .get()

                println "Successfully connected to download center on attempt ${attempt}"

                Elements versionLinks = doc.select("table tr td a")

                List<String> versions = versionLinks.collect { link ->
                    String folderName = link.text().replaceAll("/", "")
                    if (!folderName.equalsIgnoreCase("Parent Directory") &&
                            !folderName.equalsIgnoreCase("latest") &&
                            new ComparableVersion(folderName) >= new ComparableVersion(ASDB_MIN_VERSION)) {
                        return folderName
                    }
                    return null
                }.findAll { it != null }

                versions.sort { String a, String b -> new ComparableVersion(b) <=> new ComparableVersion(a) }
                println "Successfully fetched ${versions.size()} versions from download center"
                return versions as String[]

            } catch (Exception e) {
                lastException = e
                println "Download center connection attempt ${attempt} failed: ${e.getMessage()}"

                if (attempt < MAX_RETRIES) {
                    println "Retrying in ${RETRY_DELAY_MS / 1000} seconds..."
                    try {
                        Thread.sleep(RETRY_DELAY_MS)
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt()
                        throw new RuntimeException("Interrupted during retry delay", ie)
                    }
                }
            }
        }

        println "All retry attempts failed for download center"
        throw new RuntimeException("Failed to connect to download center after ${MAX_RETRIES} attempts", lastException)
    }

    @NonCPS
    private static int compareDistroPriority(String distro) {
        switch (distro) {
            case "ubuntu24.04": return 1
            case "ubuntu22.04": return 2
            case "ubuntu20.04": return 3
            case "debian12": return 4
            case "debian11": return 5
            case "el9": return 6
            case "el8": return 7
            case "amzn2023": return 8
            default: return 99
        }
    }

    @NonCPS
    public static SQLiteAsdbRecord fetchVersionDetails(String version) {
        Exception lastException = null
        String versionUrl = ASDB_DOWNLOAD_CENTER_URL + version + "/"

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                println "Fetching version details for: ${version} (attempt ${attempt}/${MAX_RETRIES})"

                Document doc = Jsoup.connect(versionUrl)
                        .timeout(TIMEOUT_MS)
                        .followRedirects(true)
                        .get()

                println "Successfully connected to version URL: ${versionUrl} on attempt ${attempt}"

                Elements links = doc.select("table tr td a")

                Set<String> distros = new HashSet<>()
                Set<String> architectures = new HashSet<>()

                links.each { link ->
                    String fileName = link.text()
                    if (!fileName.equalsIgnoreCase("Parent Directory") && fileName.endsWith(".tgz")) {
                        String[] parts = fileName.split("_")
                        if (parts.length > 3) {
                            distros.add(parts[3])
                        }
                        if (fileName.contains("_aarch64")) {
                            architectures.add("arm64")
                        } else if (fileName.contains("_x86_64")) {
                            architectures.add("amd64")
                        }
                    }
                }

                String[] sortedDistros = distros.sort { String distro -> compareDistroPriority(distro) }
                println "Successfully fetched details for version ${version}: ${distros.size()} distros, ${architectures.size()} architectures"

                return new SQLiteAsdbRecord(version, sortedDistros, architectures.toArray(new String[0]))

            } catch (Exception e) {
                lastException = e
                println "Version details fetch attempt ${attempt} failed for ${version}: ${e.getMessage()}"

                if (attempt < MAX_RETRIES) {
                    println "Retrying in ${RETRY_DELAY_MS / 1000} seconds..."
                    try {
                        Thread.sleep(RETRY_DELAY_MS)
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt()
                        throw new RuntimeException("Interrupted during retry delay", ie)
                    }
                }
            }
        }

        println "All retry attempts failed for version details: ${version}"
        throw new RuntimeException("Failed to fetch version details for ${version} after ${MAX_RETRIES} attempts", lastException)
    }

    @NonCPS
    public static String[] fetchAvailableAsdbVersionsFromDockerHub() {
        Exception lastException = null

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                println "Attempting to fetch from DockerHub (attempt ${attempt}/${MAX_RETRIES})"

                String responseText = new URL(ASDB_DOCKERHUB_URL).text

                println "Successfully fetched from DockerHub on attempt ${attempt}"

                Map<String, Object> json = (Map<String, Object>) new JsonSlurper().parseText(responseText)

                List<Map<String, Object>> results = (List<Map<String, Object>>) json.get("results")

                List<String> tags = results.collect { it.get("name")?.toString() }

                List<String> filteredTags = tags.findAll { String tag ->
                    tag != "latest" && new ComparableVersion(tag.toString()) >= new ComparableVersion(ASDB_MIN_VERSION)
                }

                List<String> sortedTags = filteredTags.sort { String a, String b ->
                    new ComparableVersion(b) <=> new ComparableVersion(a)
                }

                println "Successfully fetched ${sortedTags.size()} versions from DockerHub"
                return sortedTags as String[]

            } catch (Exception e) {
                lastException = e
                println "DockerHub fetch attempt ${attempt} failed: ${e.getMessage()}"

                if (attempt < MAX_RETRIES) {
                    println "Retrying in ${RETRY_DELAY_MS / 1000} seconds..."
                    try {
                        Thread.sleep(RETRY_DELAY_MS)
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt()
                        throw new RuntimeException("Interrupted during retry delay", ie)
                    }
                }
            }
        }

        println "All retry attempts failed for DockerHub"
        throw new RuntimeException("Failed to fetch from DockerHub after ${MAX_RETRIES} attempts", lastException)
    }
}