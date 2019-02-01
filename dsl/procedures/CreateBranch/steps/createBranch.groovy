$[/myProject/scripts/preamble]

String configName = '$[config]'
String owner = '$[ownerName]'
String repoName = '$[repoName]'
String branchName = '$[branchName]'
String from = '$[sourceBranch]'

def config = new EFPlugin().getConfiguration(configName)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
plugin.createBranch(owner, repoName, branchName, from)