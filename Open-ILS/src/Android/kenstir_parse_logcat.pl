#!/usr/bin/perl
#
# debug helper for evergreen app
#
# get output from logcat and parse the OSRF JSON output and print it

use strict;
use warnings;
use Data::Dumper;
use Getopt::Long;
use JSON;

my $debug = 0;
GetOptions("-d=i" => \$debug) or die;

my $logcat = `adb logcat -d`;
my @lines = split(/\r\n/, $logcat);
foreach my $line (@lines) {
    print "line: $line\n" if $debug;
    if ($line =~ /org.opensrf.net.http.GatewayRequest\( *\d+\): ?([^:]+):(.+)/) {
        my($key,$val) = ($1,$2);
        if ($key eq 'result') {
            my $obj = decode_json($val);
            print "$key -> ", Dumper($obj);
        } else {
            print "${key}:${val}\n";
        }
    }
}
