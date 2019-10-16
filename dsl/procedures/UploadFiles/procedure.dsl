// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Upload Files', description: '''This procedure uploads the provided files into the provided repository
''', {

    step 'Upload Files', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/UploadFiles/steps/UploadFiles.groovy").text
        // TODO altered shell
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: d206ef81766dce6d29ebc416ba2a9ee6 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}