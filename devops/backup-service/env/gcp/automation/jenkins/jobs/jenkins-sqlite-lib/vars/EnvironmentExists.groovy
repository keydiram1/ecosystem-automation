import com.ecosys.SQLiteEnvManager
@NonCPS
def call(Map config) {
    return SQLiteEnvManager.EnvironmentExists(config)
}
