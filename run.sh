
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}
    LPFilePath="in/strengthedModelAfterRootRelaxation.lp"
    initMembershipFilePath="out/$modifiedName/membership0.txt"
    echo $filename
    echo "in/""$modifiedName"
    mkdir -p "out/""$modifiedName"

    ant -v -buildfile build.xml -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DmaxNbEdit=3 -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=50000 run

    #ant -v -buildfile build.xml -DformulationType="edge" -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DmaxNbEdit=3 -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="" -DJAR_filepath_EnumCC="RNS.jar" -DnbThread=6 -Dtilim=-1 -DsolLim=50000 run

done
