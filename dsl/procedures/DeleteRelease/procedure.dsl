// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'Delete Release', description: 'Deletes a tag and an attached release from Github', {

    step 'Delete Release', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DeleteRelease/steps/DeleteRelease.groovy").text
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// === procedure_autogen ends, checksum: 420b71563fb3e061e50d6725cd7a8022 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}