// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Create Release', description: '''This procedure can create a new Github Release or update an existing one.''', {

    step 'Create Release', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.groovy").text
        // TODO altered shell
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: e1f39a29469ce11a5ea278b00706e4be ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}