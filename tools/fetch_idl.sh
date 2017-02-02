#!/bin/sh
root=http://bark.cwmars.org
# GlobalConfigs.java
IDL_FILE_FROM_ROOT="/reports/fm_IDL.xml?class=ac&class=acn&class=acp&class=ahr&class=ahtc&class=aou&class=au&class=aua&class=auact&class=bmp&class=cbreb&class=cbrebi&class=cbrebin&class=cbrebn&class=ccs&class=circ&class=cuat&class=ex&class=mbt&class=mbts&class=mous&class=mra&class=mus&class=mvr&class=perm_ex";
IDL_FILE=/reports/fm_IDL.xml

curl -o fm_IDL_trimmed.xml "$root$IDL_FILE_FROM_ROOT"
curl -o fm_IDL_orig.xml "$root$IDL_FILE"
