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

my $logcat = `adb logcat -d -v threadtime`;
my @lines = split(/\r\n/, $logcat);
foreach my $line (@lines) {
    print "line: $line\n" if $debug;
    if ($line =~ /GatewayRequest: ([^:]+): (.+)/) {
        my($key,$val) = ($1,$2);
        print "kcxxx: key={$key}=\n";
        if ($key eq 'result') {
            my $obj;
            eval '$obj = decode_json($val);';
            if ($@) {
                $obj = '*** '.$val;
            }
            print "$key -> ", Dumper($obj);
        } else {
            print "${key}:${val}\n";
        }
    }
}
