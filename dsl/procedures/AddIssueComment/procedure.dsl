// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Add Issue Comment', description: '''This procedure can create comments in issues and pull requests.''', {

    // Handling binary dependencies
    step 'flowpdk-setup', {
        description = "This step handles binary dependencies delivery"
        subprocedure = 'flowpdk-setup'
        actualParameter = [
            generateClasspathFromFolders: 'deps/libs'
        ]
    }

    step 'Add Issue Comment', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/AddIssueComment/steps/AddIssueComment.groovy").text
        shell = 'ec-groovy'
        shell = 'ec-groovy -cp $[/myJob/flowpdk_classpath]'

        resourceName = '$[flowpdkResource]'

        postProcessor = '''$[/myProject/perl/postpLoader]'''
    }

    formalOutputParameter 'commentId',
        description: 'Id of the created comment'
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: 2c9b1fdc496c71805f5037750c444d1e ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}