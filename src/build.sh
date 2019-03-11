#!/bin/bash

mvn clean install -DskipTests -Dmaven.test.skip=true 
cp emc-metalnx-web/target/emc-metalnx-web.war ../packaging/docker
cd ../packaging/docker
docker build .
