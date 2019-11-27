use strict;
use warnings;
use ElectricCommander::Util;

# Have to rename "cofngi_new" into "config_new_basic_credential"

# If promoted from < 2.2.0
# die $otherPluginName, $otherVersion, $thisVersion;
# Determine which version is newer.  $versionCmp is < 0 if this
# version is newer than the other version.
# my $versionCmp = compareExact($otherVersion, $thisVersion);


if (compareExact($otherVersion, "2.2.0.0") <= 0) {
    my $xpath = $commander->getProperties({ path => "/plugins/$pluginName/project/ec_plugin_cfgs/" });
    for my $configProp ($xpath->findnodes('//property')) {
        my $configName = $configProp->findvalue('propertyName')->string_value;
        my $oldCredName = $commander->getPropertyValue("/plugins/$pluginName/project/ec_plugin_cfgs/$configName/credential");
        if ($oldCredName) {

            my $newCredName = "${configName}_basic_credential";
            $commander->setProperty("/plugins/$pluginName/project/ec_plugin_cfgs/$configName/basic_credential", $newCredName);
            $commander->setProperty("/plugins/$pluginName/project/ec_plugin_cfgs/$configName/bearer_credential", "${configName}_bearer_credential");
            $commander->deleteProperty("/plugins/$pluginName/project/ec_plugin_cfgs/$configName/credential");
            $commander->setProperty("/plugins/$pluginName/project/ec_plugin_cfgs/$configName/authScheme", "basic");
            $commander->setProperty("/plugins/$pluginName/project/ec_plugin_cfgs/$configName/endpoint", "https://api.github.com");

            eval {
                $commander->modifyCredential({
                    credentialName => $oldCredName,
                    projectName    => "/plugins/$pluginName/project",
                    newName        => $newCredName,
                });
                warn "Renamed credential $oldCredName to $newCredName";
                $commander->createCredential({
                    credentialName => "${configName}_bearer_credential",
                    projectName    => "/plugins/$pluginName/project",
                    userName       => "",
                    password       => ""
                });
                1;
            }
                or do {
                warn "Failed to rename credential $oldCredName: $@";
            };
        }
    }
}
# /plugins/$otherPluginName/project/$configName
