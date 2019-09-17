$[/myProject/scripts/preamble]

import groovy.json.JsonSlurper
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHRepository



EFPlugin efPlugin = new EFPlugin()
Map parameters = efPlugin.getParameters()
def config = new EFPlugin().getConfiguration(parameters.config)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
String updateAction = parameters.updateAction
def assetsStr = parameters.assets
Map<String, File> assets = [:]

if (assetsStr) {
    Map assetsJson = new JsonSlurper().parseText(assetsStr) as Map
    for (String name in assetsJson.keySet()) {
        String fileName = assetsJson.get(name)
        File f = new File(fileName)
        if (!f.isAbsolute()) {
            f = new File(System.getProperty('user.dir'), fileName)
        }
        if (!f.exists()) {
            throw new RuntimeException("The asset $f.absolutePath does not exist")
        }
        assets.put(name, f)
    }
}
GithubPlugin.UpdateAction upd = GithubPlugin.UpdateAction.valueOf(updateAction.toUpperCase())
GHRelease release = plugin.createRelease(parameters.repoName as String, assets, upd, parameters.tagName as String, parameters)
efPlugin.setProperty_1("/myJob/report-urls/Release $release.tagName", release.htmlUrl.toString())
efPlugin.setPipelineSummaryLink("GitHub Release ${release.owner.fullName.replaceAll('/', '-')}: ${release.tagName}" , "Release ${release.tagName}", release.htmlUrl.toString())
