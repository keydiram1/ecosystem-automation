import com.ecosys.SQLiteWorkspaceManager
@NonCPS
def call(Map config) {
    SQLiteWorkspaceManager.AddWorkspace(config)
}
