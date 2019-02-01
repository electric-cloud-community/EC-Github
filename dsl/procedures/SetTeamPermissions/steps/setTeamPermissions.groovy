$[/myProject/scripts/preamble]

String configName = '$[config]'
String orgName = '$[orgName]'
String repoName = '$[repoName]'
String teamName = '$[teamName]'
String permissionName = '$[permissionName]'

def config = new EFPlugin().getConfiguration(configName)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
plugin.grantTeamPermission(orgName, repoName, teamName, permissionName)
