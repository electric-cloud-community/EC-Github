//
// EFPlugin  plugin = new EFPlugin()
// println plugin.getParameters()


GithubPlugin plugin = new GithubPlugin("imago-storm", System.getenv('GITHUB_TOKEN'))
// plugin.createRepository("imago-storm", "test1", [addLicense: true, licenseFile: '/tmp/LICENSE'])
plugin.createRelease('imago-storm/test',
                     ["hello": new File("/tmp/hello")],
                     GithubPlugin.UpdateAction.RECREATE, '1.0.0', [:])