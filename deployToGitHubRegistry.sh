#!/bin/bash
echo "<settings><servers><server>\
<id>github</id>\
<username>AAFC-BICoE</username>\
<password>${GITHUB_TOKEN}</password>\
</server></servers></settings>" > ~/.m2/settings.xml
mvn deploy