import com.ecosys.ArtifactSplitter

def call(Map params) {
    new ArtifactSplitter(this).split(params)
}
