@Grab(group='org.jsoup', module='jsoup', version='1.18.3')
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

String URL = "https://download.aerospike.com/artifacts/aerospike-server-enterprise/"

def fetchAsdbVersions() {
    try {
        Document doc = Jsoup.connect(URL).get()
        Elements versionLinks = doc.select("table tr td a")
        List<String> versions = versionLinks.collect { link ->
            String folderName = link.text().replaceAll("/", "")
            if (!folderName.equalsIgnoreCase("Parent Directory") && folderName >= "6.4.0.0") {
                return folderName
            }
            return null
        }.findAll { it != null } as List<String>
        return versions
    } catch (Exception e) {
        return ["Error fetching or processing data: ${e.message}"]
    }
}

return this