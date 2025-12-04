#!/usr/bin/perl
#
# usage: cat RELEASE_NOTES_A_LA*.md | perl split.pl

use warnings;
use strict;

my $fh;

while (<>) {
    if (/^## ([0-9.]+)/) {
        my $vers = $1;
        print stderr "vers: $vers\n";
        close $fh if $fh;
        open($fh, '>', "$vers.md") or die;
        print $fh $_;
    } elsif ($fh) {
        print $fh $_;
    }
}

close $fh if $fh;
