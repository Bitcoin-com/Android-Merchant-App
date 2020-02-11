#!/bin/bash
dirs=("values-ar-rSA" "values-de-rDE" "values-en-rUS" "values-es-rES" "values-fr-rFR" "values-it-rIT" "values-ja-rJP" "values-pt-rPT" "values-ru-rRU")
prefix=app/src/main/res
mv $prefix/values-en/strings.xml $prefix/values
rm -d $prefix/values-en
for dir in ${dirs[*]}
  do
    sourceDir=$prefix/$dir
    targetDir=$prefix/${dir:0:9}
    echo mv $sourceDir/strings.xml $targetDir
    mv $sourceDir/strings.xml $targetDir
    rm -d $sourceDir
  done
