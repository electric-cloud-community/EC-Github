
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
        // TODO altered shell
        shell = 'ec-groovy'
        shell = 'ec-groovy -cp $[/myJob/flowpdk_classpath]'

        resourceName = '$[/myJob/flowpdkResource]'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: b4b11ef5c5f4702a4d60fdb6249f3ae4 ===
}