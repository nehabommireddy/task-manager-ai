#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See NOTICE for additional information.
# SPDX-License-Identifier: Apache-2.0
# ----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ]; then
  javaExecutable="$(which java)"
  if [ -n "$javaExecutable" ] && ! [ "$(expr "$javaExecutable" : '\([^ ]*\)')" = "no" ]; then
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$javaExecutable")")")"
    export JAVA_HOME
  fi
fi

exec "$JAVA_HOME/bin/java" \
  -classpath ".mvn/wrapper/maven-wrapper.jar" \
  "-Dmaven.multiModuleProjectDirectory=$(pwd)" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"
