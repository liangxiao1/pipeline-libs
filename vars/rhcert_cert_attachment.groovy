// Call rhcert_manager.py to upload/download attachment to cert ticket
def call() {
    sh '''
    #!/bin/bash
    cmd_options="cert"
    source /home/p3_venv/bin/activate
    rhcert_manager token --refresh
    source $WORKSPACE/job_env.txt 
    if ! [[ -z ${CERT_CERT_ID} ]]; then
       cmd_options="${cmd_options} --id ${CERT_CERT_ID}"
       echo "use exists certification id: ${CERT_CERT_ID}"
       rhcert_manager ${cmd_options} --list
    fi
    if ! [[ -z ${CERT_CERT_CASENUMBER} ]]; then
       cmd_options="${cmd_options} --caseNumber ${CERT_CERT_CASENUMBER}"
       echo "use exists certification case number: ${CERT_CERT_CASENUMBER}"
       rhcert_manager ${cmd_options} --list
    fi
    if [[ -z ${CERT_CERT_ID} ]] && [[ -z ${CERT_CERT_CASENUMBER} ]]; then
        echo 'missing CERT_CERT_ID or CERT_CERT_CASENUMBER'
        exit 0
    fi
    if [[ -z ${CERT_CERT_ATTACHMENT} ]]; then
        echo 'missing CERT_CERT_ATTACHMENT'
        exit 0
    fi

    echo "upload new attachment ${CERT_CERT_ATTACHMENT}"
    rhcert_manager $cmd_options --attachment "${CERT_CERT_ATTACHMENT}" --attachment_desc "Auto uploaded." --attachment_upload
    rhcert_manager $cmd_options --attachments_list
    '''
}