#!/bin/bash
~/Downloads/osmosis-latest/bin/osmosis --rb file=brazil-latest.osm.pbf \
   --bounding-box left=-49.337540 top=-26.056166 right=-48.539658 bottom=-26.536323 \
completeWays=true --used-node --wx allroads.osm
