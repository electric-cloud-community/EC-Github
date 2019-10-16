
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Create Repository', description: '''Creates a GitHub Repository''', {

    step 'Create Repository', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/CreateRepository/steps/CreateRepository.groovy").text
        // TODO altered shell
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: 4f13e44746feee914ee6c05288698eef ===
}