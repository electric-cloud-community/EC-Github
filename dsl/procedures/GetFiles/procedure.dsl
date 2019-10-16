// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Get Files', description: '''This procedure fetches the content of the specified files and stores it in
the filesystem or in the provided property
''', {

    step 'Get Files', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/GetFiles/steps/GetFiles.groovy").text
        // TODO altered shell
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: 42579be15e121efa323e64ac87abf04a ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}