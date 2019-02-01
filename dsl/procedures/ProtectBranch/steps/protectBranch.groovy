$[/myProject/scripts/preamble]

String configName = '$[config]'
String owner = '$[ownerName]'
String repoName = '$[repoName]'
String branchName = '$[branchName]'
int requiredReviewers = '$[requiredReviewers]' as int

def config = new EFPlugin().getConfiguration(configName)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
plugin.protectBranch(owner, repoName, branchName, requiredReviewers)