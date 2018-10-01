/*
 *   Copyright (C) 2002-2006  Felipe Rivera <liebremx at users.sourceforge.net>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *
 *   Coefficient stuff
 *
 *   $Id: iir_cfs.c,v 1.2 2006/01/15 00:17:46 liebremx Exp $
 */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

static const double band_f031[] =
{ 20,25,31.5,40,50,63,80,100,125,160,200,250,315,400,500,630,800,
  1000,1250,1600,2000,2500,3150,4000,5000,6300,8000,10000,12500,16000,20000
};

#define GAIN_F0 1.0
#define GAIN_F1 GAIN_F0 / M_SQRT2

#define SAMPLING_FREQ 44100.0
#define TETA(f) (2*M_PI*(double)f/sample_frequency)
#define TWOPOWER(value) (value * value)

#define BETA2(tf0, tf) \
(TWOPOWER(GAIN_F1)*TWOPOWER(cos(tf0)) \
 - 2.0 * TWOPOWER(GAIN_F1) * cos(tf) * cos(tf0) \
 + TWOPOWER(GAIN_F1) \
 - TWOPOWER(GAIN_F0) * TWOPOWER(sin(tf)))
#define BETA1(tf0, tf) \
    (2.0 * TWOPOWER(GAIN_F1) * TWOPOWER(cos(tf)) \
     + TWOPOWER(GAIN_F1) * TWOPOWER(cos(tf0)) \
     - 2.0 * TWOPOWER(GAIN_F1) * cos(tf) * cos(tf0) \
     - TWOPOWER(GAIN_F1) + TWOPOWER(GAIN_F0) * TWOPOWER(sin(tf)))
#define BETA0(tf0, tf) \
    (0.25 * TWOPOWER(GAIN_F1) * TWOPOWER(cos(tf0)) \
     - 0.5 * TWOPOWER(GAIN_F1) * cos(tf) * cos(tf0) \
     + 0.25 * TWOPOWER(GAIN_F1) \
     - 0.25 * TWOPOWER(GAIN_F0) * TWOPOWER(sin(tf)))

#define GAMMA(beta, tf0) ((0.5 + beta) * cos(tf0))
#define ALPHA(beta) ((0.5 - beta)/2.0)

/*************
 * Functions *
 *************/

/* Get the band_f031 at both sides of F0. These will be cut at -3dB */
static void find_f1_and_f2(double f0, double octave_percent, double *f1, double *f2)
{
    double octave_factor = pow(2.0, octave_percent/2.0);
    *f1 = f0/octave_factor;
    *f2 = f0*octave_factor;
}

/* Find the quadratic root
 * Always return the smallest root */
static int find_root(double a, double b, double c, double *x0) {
  double k = c-((b*b)/(4.*a));
  double h = -(b/(2.*a));
  double x1 = 0.;
  if (-(k/a) < 0.)
    return -1;
  *x0 = h - sqrt(-(k/a));
  x1 = h + sqrt(-(k/a));
  if (x1 < *x0)
    *x0 = x1;
  return 0;
}

void calc_coeffs(double sample_frequency)
{
  int i, n;
  double f1, f2;
  double x0;

    printf("    public final static IIRCoefficients iir_cf31_%d[] = {\n", (int)sample_frequency);
    for (i = 0; i < 31; i++) {

      /* Find -3dB frequencies for the center freq */
      find_f1_and_f2(band_f031[i], 1.0/3.0, &f1, &f2);
      /* Find Beta */
      if ( find_root(
            BETA2(TETA(band_f031[i]), TETA(f1)),
            BETA1(TETA(band_f031[i]), TETA(f1)),
            BETA0(TETA(band_f031[i]), TETA(f1)),
            &x0) == 0)
      {
        /* Got a solution, now calculate the rest of the factors */
        /* Take the smallest root always (find_root returns the smallest one)
         *
         * NOTE: The IIR equation is
         *  y[n] = 2 * (alpha*(x[n]-x[n-2]) + gamma*y[n-1] - beta*y[n-2])
         *  Now the 2 factor has been distributed in the coefficients
         */
        /* Now store the coefficients */
        printf("        /* %.1f Hz */\n", band_f031[i]);
        printf("        new IIRCoefficients(%.10e, %010e, %.10e),\n",
            (double)(2.0 * x0),
            (double)(2.0 * ALPHA(x0)),
            (double)(2.0 * GAMMA(x0, TETA(band_f031[i])))
        );
      } else {
        printf("  **** Where are the roots?\n");
      }
    }// for i
    printf("    };\n");
}

int main(int argc, char **argv) {
    if (argc != 2) {
        printf("Usage: iircoeff <sample frequency>\n");
        return -1;
    }

    double f = strtod(argv[1], NULL);
    calc_coeffs(f);
}
