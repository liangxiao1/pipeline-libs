// Call rhcert_manager.py to create/update certification ticket
def call() {
    sh '''
    #!/bin/bash
    cmd_options="cert"
    source /home/ec2/p3_venv/bin/activate
    cd /home/ec2/mini_utils
    python rhcert_manager.py token --init
    source $WORKSPACE/job_env.txt
    if ! [[ -z ${CERT_CERT_ID} ]]; then
       cmd_options="${cmd_options} --id ${CERT_CERT_ID}"
       echo "use exists certification id: ${CERT_CERT_ID}"
       python rhcert_manager.py ${cmd_options} --list
    fi
    if ! [[ -z ${CERT_CERT_CASENUMBER} ]]; then
       cmd_options="${cmd_options} --caseNumber ${CERT_CERT_CASENUMBER}"
       echo "use exists certification case number: ${CERT_CERT_CASENUMBER}"
       python rhcert_manager.py ${cmd_options} --list
       if (( $? != 0 )); then
            exit 1
       fi
    fi
    if ! [[ -z ${CERT_CERT_ID} ]] || ! [[ -z ${CERT_CERT_CASENUMBER} ]]; then
        echo 'missing CERT_CERT_ID or CERT_CERT_CASENUMBER'
        exit 0
    fi

    echo "create new cert ticket since CERT_ID or  CERT_CASENUMBER not provided"
    if [[ ${ARCH} =~ 'x86' ]]; then
        platformId=1
    else
        platformId=7
    fi
    if ! [[ -z ${CERT_CERT_VERSIONID} ]]; then
        content="{\\"versionId\\":\\"${CERT_CERT_VERSIONID}\\",\\"platformId\\":\\"$platformId\\"}"
    else
        content="{\\"platformId\\":\\"$platformId\\"}"
    fi
    out=$(python rhcert_manager.py cert --classificationId ${CERT_CERT_CLASSIFICATIONID} --partnerProductId ${CERT_PRODUCT_ID}  --certificationTypeId ${CERT_CERT_CERTIFICATIONTYPEID} --content $content --new)
    if (( $? != 0 )); then
        echo $out
        exit 1
    fi
    id=$(echo -e $out|jq ".id")
    casenum=$(echo -e $out|jq ".caseNumber")
    python rhcert_manager.py cert --id $id --list
    sed -i "/CERT_CERT_ID/d" $WORKSPACE/job_env.yaml
    sed -i "/CERT_CERT_ID/d" $WORKSPACE/job_env.txt
    sed -i "/CERT_CERT_CASENUMBER/d" $WORKSPACE/job_env.yaml
    sed -i "/CERT_CERT_CASENUMBER/d" $WORKSPACE/job_env.txt
    echo """\
CERT_CERT_ID: $id
CERT_CERT_CASENUMBER: $casenum""" >> $WORKSPACE/job_env.yaml
echo """\
CERT_CERT_ID=$id
CERT_CERT_CASENUMBER=$casenum""" >> $WORKSPACE/job_env.txt
    '''
}