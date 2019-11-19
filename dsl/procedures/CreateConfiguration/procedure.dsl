
// DO NOT EDIT THIS BLOCK === configuration starts ===
procedure 'CreateConfiguration', description: 'Creates a plugin configuration', {

    step 'createConfiguration',
        command: new File(pluginDir, "dsl/procedures/CreateConfiguration/steps/createConfiguration.pl").text,
        errorHandling: 'abortProcedure',
        exclusiveMode: 'none',
        postProcessor: '$[/myProject/perl/postpLoader]',
        releaseMode: 'none',
        shell: 'ec-perl',
        timeLimitUnits: 'minutes'

    property 'ec_checkConnection', value: ''
// DO NOT EDIT THIS BLOCK === configuration ends, checksum: 3a9bcf9be26ddf71372e76023a02a335 ===
}