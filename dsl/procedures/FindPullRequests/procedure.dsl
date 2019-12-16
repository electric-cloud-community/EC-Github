// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Find Pull Requests', description: '''This procedure will return all open PRs. If a branch is given, only PR with head at branch will be returned.''', {

    // Handling binary dependencies
    step 'flowpdk-setup', {
        description = "This step handles binary dependencies delivery"
        subprocedure = 'flowpdk-setup'
        actualParameter = [
            generateClasspathFromFolders: 'deps/libs'
        ]
    }

    step 'Find Pull Requests', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/FindPullRequests/steps/FindPullRequests.groovy").text
        shell = 'ec-groovy'
        shell = 'ec-groovy -cp $[/myJob/flowpdk_classpath]'

        resourceName = '$[flowpdkResource]'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'prNum',
        description: 'Numbers of the found pull request(s).'
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: af521965cc291cfc0b7b29bfa556db85 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}