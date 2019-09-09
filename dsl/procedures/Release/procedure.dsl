import java.io.File

def procName = 'Create Release'
procedure procName, description: 'Creates a release on Github', {

    step 'Create Release',
         command: new File(pluginDir, "dsl/procedures/Release/steps/release.groovy").text,
         errorHandling: 'failProcedure',
         exclusiveMode: 'none',
         releaseMode: 'none',
         shell: 'ec-groovy',
         timeLimitUnits: 'minutes'

}
