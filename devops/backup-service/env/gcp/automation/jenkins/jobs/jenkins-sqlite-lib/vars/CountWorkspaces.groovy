import com.ecosys.SQLiteWorkspaceManager
@NonCPS
def call(Map config) {
    return SQLiteWorkspaceManager.CountWorkspaces()
}
