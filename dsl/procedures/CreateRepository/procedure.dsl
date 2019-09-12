
// === procedure_autogen starts ===
procedure 'Create Repository', description: 'Creates a GitHub Repository', {

    step 'Create Repository', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRepository/steps/CreateRepository.groovy").text
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// === procedure_autogen ends, checksum: 9e8e485c7e1c0c68e76405aa1ee22414 ===
}