import com.ecosys.SQLiteWorkspaceManager
@NonCPS
def call(String workspace = '') {
    SQLiteWorkspaceManager.RemoveWorkspace(workspace)
}
