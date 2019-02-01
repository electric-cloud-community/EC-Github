$[/myProject/scripts/preamble]

String configName = '$[config]'
String owner = '$[owner]'
String repoName = '$[repoName]'
String description = '''
$[repoDescription]
'''
String homepage = '$[repoHomepage]'
boolean isPublic = '$[repoPublic]' == 'true'

def config = new EFPlugin().getConfiguration(configName)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
plugin.createRepository(owner, repoName, description, homepage, isPublic)