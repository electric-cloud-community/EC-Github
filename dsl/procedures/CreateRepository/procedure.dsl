
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Create Repository', description: '''Creates a GitHub Repository''', {

    // Handling binary dependencies
    step 'flowpdk-setup', {
        description = "This step handles binary dependencies delivery"
        subprocedure = 'flowpdk-setup'
        actualParameter = [
            generateClasspathFromFolders: 'deps/libs'
        ]
    }

    step 'Create Repository', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRepository/steps/CreateRepository.groovy").text
        shell = 'ec-groovy'
        shell = 'ec-groovy -cp $[/myJob/flowpdk_classpath]'

        resourceName = '$[flowpdkResource]'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: 732b06af9a37a80377df3a0fa4d6a995 ===
}