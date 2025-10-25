package com.ecosys
import groovy.json.JsonOutput

class SQLiteAsdbRecord {
    private String version
    private String distros
    private String archs

    public SQLiteAsdbRecord(String version, String[] distros, String[] archs) {
        this.version = version
        this.distros = JsonOutput.toJson(distros)
        this.archs = JsonOutput.toJson(archs)
    }

    @NonCPS
    public String getVersion() {
        return version
    }

    @NonCPS
    public String getDistros() {
        return distros
    }

    @NonCPS
    public String getArchs() {
        return archs
    }

    @NonCPS
    @Override
    String toString() {
        return "SQLiteAsdbRecord(version='" + version + "', distros=" + distros + ", archs=" + archs + ")"
    }
}
