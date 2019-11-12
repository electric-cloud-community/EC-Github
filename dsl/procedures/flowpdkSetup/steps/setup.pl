use strict;
use warnings;
use JSON;
use ElectricCommander;
use Data::Dumper;
use File::Spec;
use File::Path qw(mkpath);
use Digest::MD5;
use MIME::Base64 qw(decode_base64);
use Archive::Zip;
use Time::HiRes qw(gettimeofday tv_interval);
use subs qw(logInfo);

my $ec = ElectricCommander->new;
my $resName = '$[/myResource/name]';
$ec->setProperty({propertyName => '/myJob/flowpdkResource', value => $resName});
print "Acquired resource $resName\n";

my $generateClasspathFromFolders = $ec->getPropertyValue('generateClasspathFromFolders');
deliverDependencies();

if ($generateClasspathFromFolders) {
    print "generateClasspathFromFolders: $generateClasspathFromFolders\n";
    # Folders are relative to agent/ folder
    my @jars = ();

    for my $folder (split /\,\s*/ => $generateClasspathFromFolders) {
        my $path = File::Spec->catfile($ENV{COMMANDER_PLUGINS}, '@PLUGIN_NAME@/agent/' . $folder);
        if (-d $path) {
            if ($path !~ /\/$/) {
                $path .= '/';
            }
            $path .= '*';
            push @jars, $path;
        }
        # print "Looking for jars in folder $path\n";
        # opendir my $dh, $path or die "Cannot open directory $path: $!";
        # for my $file (readdir $dh) {
        #     # One level
        #     next if $file =~ /^\./;
        #     if ($file =~ /\.jar$/) {
        #         push @jars, File::Spec->catfile($path, $file);
        #     }
        # }
    }

    my $os = $^O;
    my $separator = ':';
    if ($os =~ /win/i) {
        $separator = ";";
    }
    my $classpath = join($separator, @jars);
    $ec->setProperty({propertyName => '/myJob/flowpdk_classpath', value => $classpath});
    print "Classpath: $classpath\n";
}

# Auto-generated method for the procedure DeliverDependencies/DeliverDependencies
# Add your code into this method and it will be called when step runs
sub deliverDependencies {
    my $start = [gettimeofday];

    my $source = $ec->getPropertyValue('/server/settings/pluginsDirectory') .'/@PLUGIN_NAME@/agent';

    my $processingFile = 0;
    my $dest = File::Spec->catfile($ENV{COMMANDER_PLUGINS}, '@PLUGIN_NAME@/agent');
    $ec->setProperty({
        propertyName => '/myJob/flowpdk_agentFolderPath',
        value => $dest,
    });

    # Cache check
    my $cacheValid = 1;
    my $meta = {};
    eval {
        $meta = readMeta();
        1;
    } or do {
        $cacheValid = 0;
        print "[WARNING] Failed to read meta. Caches won't be used\n";
    };

    my @files = keys %$meta;
    for my $file (@files) {
        my $fullFilename = File::Spec->catfile($dest, $file);
        unless(-f $fullFilename) {
            logInfo "File $fullFilename does not exist, cache is invalid";
            $cacheValid = 0;
            last;
        }
        my $digest = Digest::MD5->new;
        open(my $fh, $fullFilename);
        if (!$fh) {
            logInfo "File $fullFilename cannot be opened, cache is invalid";
            $cacheValid = 0;
            last;
        }
        binmode $fh;
        $digest->addfile($fh);
        if ($digest->hexdigest ne $meta->{$file}) {
            $cacheValid = 0;
            logInfo "Checksums do not match for file $file, cache is invalid";
            last;
        }
    }
    if ($cacheValid) {
        logInfo "Local cache is valid, no further action required";
        exit 0;
    }

    # TODO error
    my $dsl = $ec->getPropertyValue('/myProcedure/flowpdk-dsl');

    my $hasMore = 1;
    my $offset = 0;
    mkpath($dest);
    my $dependencies = File::Spec->catfile($dest, ".cbDependenciesTarget.zip");
    open my $fh, ">$dependencies" or die "Cannot open $dependencies: $!";
    binmode $fh;
    while($hasMore) {
        my $args = {
            offset => $offset,
            source => $source,
            chunkSize => 1024 * 1024 * 4
        };
        print "Calling evalDSl with arguments " . Dumper ($args) . "\n";
        my $xpath = $ec->evalDsl({
            dsl => $dsl,
            parameters => encode_json($args),
        });
        my $result = $xpath->findvalue('//value')->string_value;
        my $chunks = decode_json($result);
        my $chunk = $chunks->{chunk};
        my $bytes = decode_base64($chunk);
        my $remaining = $chunks->{remaining};
        print "Got bytes: " . length($bytes) . "\n";
        print "Bytes remaining: " . $remaining . "\n";
        my $readBytes = $chunks->{readBytes};
        $offset += $readBytes;
        print "Read bytes: $readBytes\n";
        if ($readBytes > 0) {
            print $fh $bytes;
        }
        if ($remaining == 0 || $readBytes <= 0) {
            $hasMore = 0;
        }
    }
    close $fh;

    my $t0 = [gettimeofday];

    my $zip = Archive::Zip->new();
    unless($zip->read($dependencies) == Archive::Zip::AZ_OK()) {
      die "Cannot read .zip dependencies: $!";
    }
    $zip->extractTree("", $dest . '/');

    # TODO write meta
}

sub readMeta {
    my $ec = ElectricCommander->new;
    my $json = $ec->getPropertyValue('/myProject/flowpdk_binaryDependencies');
    unless($json) {
        # TODO error
        die 'Failed to retrive dependencies meta from property /myProject/flowpdk_binaryDependencies';
    }
    my $meta = decode_json($json);
    return $meta;
}


sub logInfo {
    my $message = shift;
    print "[INFO] $message\n";
}
