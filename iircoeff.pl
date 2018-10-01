#!/usr/bin/perl

use Math::Trig;

my $fs = 48000;
my $q = 1.414;

printCo(20, $fs, $q);
printCo(25, $fs, $q);
printCo(31.5, $fs, $q);
printCo(40, $fs, $q);
printCo(50, $fs, $q);
printCo(63, $fs, $q);
printCo(80, $fs, $q);
printCo(100, $fs, $q);
printCo(125, $fs, $q);
printCo(160, $fs, $q);
printCo(200, $fs, $q);
printCo(250, $fs, $q);
printCo(315, $fs, $q);
printCo(400, $fs, $q);
printCo(500, $fs, $q);
printCo(630, $fs, $q);
printCo(800, $fs, $q);
printCo(1000, $fs, $q);
printCo(1250, $fs, $q);
printCo(1600, $fs, $q);
printCo(2000, $fs, $q);
printCo(2500, $fs, $q);
printCo(3150, $fs, $q);
printCo(4000, $fs, $q);
printCo(5000, $fs, $q);
printCo(6300, $fs, $q);
printCo(8000, $fs, $q);
printCo(10000, $fs, $q);
printCo(12500, $fs, $q);
printCo(16000, $fs, $q);
printCo(20000, $fs, $q);

sub printCo($$$$) {
    my $f0 = shift;
    my $fs = shift;
    my $q = shift;


    @coeff = coefficient($f0, $fs, $q);
    print "/* $f0 Hz */\n";
    printf("new IIRCoefficients(%.10e, %.10e, %.10e),\n" , $coeff[1] * 2, $coeff[0] * 2, $coeff[2] * 2);
}


sub coefficient($$$$) {
    my $f0 = shift;
    my $fs = shift;
    my $q = shift;

    my $q2 = $q * $q;

    my $f1 = $f0 * (sqrt(1 + (1 / (4 * $q2))) - (1 / (2 * $q)));
    my $f2 = $f0 * (sqrt(1 + (1 / (4 * $q2))) + (1 / (2 * $q)));

    my $pi = 3.141592653;

    my $theta0 = 2 * $pi * ($f0 / $fs);

    my $thetaOverTwoQ = $theta0 / (2 * $q);

    my $beta = 0.5 * ((1 - tan($thetaOverTwoQ)) / (1 + tan($thetaOverTwoQ)));

    my $gamma = (0.5 + $beta) * cos($theta0);
    
    my $alpha = (0.5 - $beta) / 2;

    return ($alpha, $beta, $gamma);
}

