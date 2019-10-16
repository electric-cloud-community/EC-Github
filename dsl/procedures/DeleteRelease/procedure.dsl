// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Delete Release', description: '''Deletes a tag and an attached release from Github''', {

    step 'Delete Release', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DeleteRelease/steps/DeleteRelease.groovy").text
        // TODO altered shell
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: 082f96095acf81c8caa3e5f42acd0304 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}