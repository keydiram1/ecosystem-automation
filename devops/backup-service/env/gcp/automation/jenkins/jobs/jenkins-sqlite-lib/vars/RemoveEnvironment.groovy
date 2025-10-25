import com.ecosys.SQLiteEnvManager
@NonCPS
def call(Map config) {
    SQLiteEnvManager.RemoveEnvironment(config)
}
