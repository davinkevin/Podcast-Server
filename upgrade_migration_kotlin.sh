#!/usr/bin/env bash

set -euo pipefail

LAST_JAVA_COMMIT=4b123dfe
# Number of Java Lines
#git co $LAST_JAVA_COMMIT 
#NUMBER_OF_JAVA_START=`find backend/src -name '*.java' | xargs wc -l | tail -n 1 | sed -e 's@[[:blank:]]\([0-9]*\).*@\1@g'`
NUMBER_OF_JAVA_START=22569

#git co wip-migration-kotlin
NUMBER_OF_JAVA_END=`find backend/src -name '*.java' | xargs wc -l | tail -n 1 | sed -e 's@[[:blank:]]\([0-9]*\).*@\1@g'`

STAGE_JAVA=`echo "($NUMBER_OF_JAVA_END * 100) / $NUMBER_OF_JAVA_START" | bc`
STAGE_KOTLIN=`echo "100 - $STAGE_JAVA" | bc`

MIGRATION_LINE="Migration : ![Java stage](https://badgen.net/badge/Java/$STAGE_JAVA%25/orange) ![Kotlin stage](https://badgen.net/badge/Kotlin/$STAGE_KOTLIN%25/purple)"

sed -i -e "s@Migration.*@$MIGRATION_LINE@g" README.md