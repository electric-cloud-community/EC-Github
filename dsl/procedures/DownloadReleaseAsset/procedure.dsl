// This procedure.dsl was generated automatically
// DO NOT EDIT THIS BLOCK === procedure_autogen starts ===
procedure 'Download Release Asset', description: '''Downloads the specified release asset from Github.''', {

    step 'Download Release Asset', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DownloadReleaseAsset/steps/DownloadReleaseAsset.groovy").text
        // TODO altered shell
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// DO NOT EDIT THIS BLOCK === procedure_autogen ends, checksum: 186a88b00b674bdc1293a2d584c610a0 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}