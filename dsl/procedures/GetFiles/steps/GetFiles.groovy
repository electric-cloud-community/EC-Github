$[/myProject/scripts/preamble]

EFPlugin efPlugin = new EFPlugin()
Map parameters = efPlugin.getParameters()
def config = new EFPlugin().getConfiguration(parameters.config)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
List<String> files = parameters.files.split(/\n+/)
plugin.downloadFiles(
    parameters.ownerName,
    parameters.repoName,
    files,
    parameters.ref,
    parameters.destinationFolder
)