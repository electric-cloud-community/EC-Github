import java.io.File

def procName = 'Delete Tag'
procedure procName, description: 'Deletes a tag and an attached release from Github', {

    step 'Delete Tag',
         command: new File(pluginDir, "dsl/procedures/DeleteRelease/steps/step.groovy").text,
         errorHandling: 'failProcedure',
         exclusiveMode: 'none',
         releaseMode: 'none',
         shell: 'ec-groovy',
         timeLimitUnits: 'minutes'

}
