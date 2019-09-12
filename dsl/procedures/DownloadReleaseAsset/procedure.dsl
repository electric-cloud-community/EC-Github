import java.io.File

def procName = 'Download Release Asset'
procedure procName, description: 'Downloads the specified asset from the release', {

    step procName,
         command: new File(pluginDir, "dsl/procedures/DownloadReleaseAsset/steps/step.groovy").text,
         errorHandling: 'failProcedure',
         exclusiveMode: 'none',
         releaseMode: 'none',
         shell: 'ec-groovy',
         timeLimitUnits: 'minutes'

}
