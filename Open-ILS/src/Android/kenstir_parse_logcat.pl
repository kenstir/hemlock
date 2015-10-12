#!/usr/bin/perl
#
# debug helper for evergreen app
#
# get output from logcat and parse the OSRF JSON output and print it

use strict;
use warnings;
use Data::Dumper;
use JSON;

my $logcat = `adb logcat -d`;
my @lines = split(/\n/, $logcat);
foreach my $line (@lines) {
    if ($line =~ /org.opensrf.net.http.GatewayRequest.*service:(\S+) method:(\S+) result:(.*)/) {
        my($svc,$method,$json) = ($1,$2,$3);
        my $obj = decode_json($json);
        print "$method $svc -> ", Dumper($obj);
    }
}
