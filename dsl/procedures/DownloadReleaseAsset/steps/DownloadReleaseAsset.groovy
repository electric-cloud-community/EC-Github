$[/myProject/scripts/preamble]


EFPlugin efPlugin = new EFPlugin()
Map parameters = efPlugin.getParameters()
def config = new EFPlugin().getConfiguration(parameters.config)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
plugin.downloadReleaseAsset(parameters.repoName, parameters.tagName, parameters.assetName, parameters.assetPath)

