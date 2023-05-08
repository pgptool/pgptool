#!/bin/bash
if [[ ! -d "$HOME/.m2/repository/com/akathist/maven/plugins/launch4j" ]]; then
	git clone https://github.com/orphan-oss/launch4j-maven-plugin TEMP-launch4j-mp
	cd TEMP-launch4j-mp
	#VERSION=$(grep '<version>' pom.xml | grep SNAPSHOT | grep -Po '[0-9\.]+-SNAPSHOT')
	mvn install
	cd ..
	rm -rf TEMP-launch4j-mp
fi

VERSION=`ls $HOME/.m2/repository/com/akathist/maven/plugins/launch4j/launch4j-maven-plugin/|grep SNAPSHOT|tee|tail -n 1`
cd pgptool-gui
sed -E ':a;N;$!ba;s@(<artifactId>launch4j-maven-plugin<\/artifactId>[[:space:]]*\n[[:space:]]*<version>)([0-9.]+(-SNAPSHOT)?)</version>@\1'"$VERSION"'<\/version>@m' -i pom.xml
mvn package
