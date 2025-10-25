package com.ecosys

import java.io.Serializable

class ArtifactSplitter implements Serializable {
    private final Object steps

    ArtifactSplitter(Object steps) {
        this.steps = steps
    }

    void split(Map<String, Object> params) {
        List<String> artifacts = params["artifacts"] instanceof String
                ? [params["artifacts"] as String]
                : params["artifacts"] as List<String>

        List<String> images = new ArrayList<>()
        List<String> packages = new ArrayList<>()
        List<String> charts = new ArrayList<>()


        for (String item : artifacts) {
            if (item == "docker") {
                images.add(item)
            } else if (item == "helm") {
                charts.add(item)
            } else {
                packages.add(item)
            }
        }

        steps.env.IMAGES = images.join(' ')
        steps.env.PACKAGES = packages.join(' ')
        steps.env.CHARTS = charts.join(' ')
    }
}
