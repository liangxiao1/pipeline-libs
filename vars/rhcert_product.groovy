// Call rhcert_manager.py to create/update product
def call() {
    sh '''
    #!/bin/bash
    cmd_options="product"
    source /home/p3_venv/bin/activate
    rhcert_manager token --refresh
    source $WORKSPACE/job_env.txt

    if ! [[ -z ${CERT_PRODUCT_ID} ]]; then
       cmd_options="${cmd_options} --id ${CERT_PRODUCT_ID}"
       echo "use exists product id: ${CERT_PRODUCT_ID}"
       rhcert_manager ${cmd_options} --list
       exit 0
    fi

    echo "create new product since PRODUCT_ID not provided"
    out=$(rhcert_manager product --partnerId ${CERT_PRODUCT_PARTNERID} --category "${CERT_PRODUCT_CATEGORY}" --name "${CERT_PRODUCT_NAME}" --make "${CERT_PRODUCT_MAKE}" --model "${CERT_PRODUCT_MODEL}" --description "${CERT_PRODUCT_DESCRIPTION}" --productUrl "${CERT_PRODUCT_PRODUCTURL}" --specUrl "${CERT_PRODUCT_SPECURL}" --supportUrl "${CERT_PRODUCT_SUPPORTURL}" --new)
    if (( $? != 0 )); then
        echo $out
        exit 1
    fi
    echo $out
    id=$(echo -e $out|jq ".id")
    rhcert_manager product --id $id --list
    sed -i "/CERT_PRODUCT_ID/d" $WORKSPACE/job_env.yaml
    sed -i "/CERT_PRODUCT_ID/d" $WORKSPACE/job_env.txt
    echo """\
CERT_PRODUCT_ID: $id""" >> $WORKSPACE/job_env.yaml
echo """\
CERT_PRODUCT_ID=$id""" >> $WORKSPACE/job_env.txt
    '''
}