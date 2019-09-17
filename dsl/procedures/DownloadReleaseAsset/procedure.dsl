// This procedure.dsl was generated automatically
// === procedure_autogen starts ===
procedure 'Download Release Asset', description: 'Downloads the specified release asset from Github.', {

    step 'Download Release Asset', {
        description = ''
        command = new File(pluginDir, "dsl/procedures/DownloadReleaseAsset/steps/DownloadReleaseAsset.groovy").text
        shell = 'ec-groovy'

        postProcessor = ''''''
    }
// === procedure_autogen ends, checksum: ef595f86961464d70f2ced0e3197da86 ===
// Do not update the code above the line
// procedure properties declaration can be placed in here, like
// property 'property name', value: "value"
}