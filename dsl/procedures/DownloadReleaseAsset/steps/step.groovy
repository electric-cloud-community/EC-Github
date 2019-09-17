$[/myProject/scripts/preamble]

import groovy.json.JsonSlurper
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHRepository



EFPlugin efPlugin = new EFPlugin()
Map parameters = efPlugin.getParameters()
def config = new EFPlugin().getConfiguration(parameters.config)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
plugin.downloadReleaseAsset(parameters.repoName, parameters.tagName, )

