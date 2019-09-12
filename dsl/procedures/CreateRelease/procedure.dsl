// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'Create Release', description: 'This procedure can create a new Github Release or update an existing one.', {

    step 'Create Release', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRelease/steps/CreateRelease.groovy").text
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// === procedure_autogen ends, checksum: e75ae2fc97e8d0b7207a3e3fb41bc80d ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}