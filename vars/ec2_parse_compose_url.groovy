// Get compose information from ci message or compose url
// Below env's are required
// params.ARCH='x86_64|aarch64'

def call() {
    sh '''
    #!/bin/bash -x
    if ! [ -z ${CI_MESSAGE} ]; then
        echo "This is triggerd by CI message."
        build_owner="UMB_TRIGGER"
        echo "CI Message: ${CI_MESSAGE}"
        python /home/ec2/mini_utils/json_parser.py -c "${CI_MESSAGE}" --dir "${WORKSPACE}" -d
        source ${WORKSPACE}/job_env.txt
        if ! [ -z $JOB_COMPOSE_COMPOSE_INFO_PAYLOAD_COMPOSE_ID ]; then
            #baseurl="${COMPOSE_LOCATION}/${JOB_COMPOSE_COMPOSE_INFO_PAYLOAD_COMPOSE_ID}"
            baseurl="${JOB_COMPOSE_COMPOSE_URL}"
        elif ! [ -z $JOB_INFO_BUILD_ID ]; then
            echo "This is brew build pkg test"
            if ! [[ ${JOB_INFO_PACKAGE_NAME} =~ 'kernel' ]]; then
            python /home/ec2/mini_utils/html_parser.py --url "${BREW_BUILD_URL}${JOB_INFO_BUILD_ID}" --dir "${WORKSPACE}" --keyword "${JOB_INFO_PACKAGE_NAME}-\\d*.*(noarch|${DEFAULT_ARCH})" --excludekeys "src"
            else
            python /home/ec2/mini_utils/html_parser.py --url "${BREW_BUILD_URL}${JOB_INFO_BUILD_ID}" --dir "${WORKSPACE}" --keyword "${JOB_INFO_PACKAGE_NAME}-" --excludekeys "src,devel,internal,debuginfo,extra,headers,cross,debug,tools" --andkeys "x86" --element a --field href     
            fi
            python /home/ec2/mini_utils/html_parser.py --url "${BREW_BUILD_URL}${JOB_INFO_BUILD_ID}" --dir "${WORKSPACE}" --keyword "Tags" --element tr --field text --name BREWTAG -r
            source ${WORKSPACE}/job_env.txt
            PKG_URL=${JOB_PKGURL}
            compose_id=${JOB_INFO_NVR}
        else
            baseurl=$JOB_LOCATION
        fi
        if [ -z $JOB_INFO_BUILD_ID ]; then
            baseurl=${baseurl//'"'}
            baseurl=${baseurl%/compose/}
            baseurl=${baseurl%/compose}
            COMPOSEID_URL="${baseurl}/COMPOSE_ID"
            export trigger_url=${COMPOSEID_URL}
        fi
    elif ! [ -z ${COMPOSEID_URL} ]; then
        echo "This is triggerd by manually."
        build_owner="MANUAL_TRIGGER"
        export trigger_url=${COMPOSEID_URL}
    fi
    if [ -z ${trigger_url} ]; then
        echo "compose url not found, do not update image"
        if [ -z ${compose_id} ]; then
            compose_id="UNSPECIFIED"
        fi
        baseurl="UNSPECIFIED"
        COMPOSEID_URL="UNSPECIFIED"
        repo_url="UNSPECIFIED"
    else
        compose_id=`curl ${trigger_url}`
        baseurl=${trigger_url%COMPOSE_ID}
        if [[ "$compose_id" =~ "RHEL-7" ]];then
            echo "This is RHEL-7 compose build"
            repo_url="$baseurl/compose/Server/${ARCH}/os/"
        elif [[ $compose_id =~ 'CentOS-Stream' ]]; then
            echo "This is $compose_id compose build, use BaseOS and AppStream repos"
            repo_url="${baseurl}/BaseOS/${ARCH}/os/,${baseurl}/AppStream/${ARCH}/os/"
        else
            echo "This is $compose_id compose build, use BaseOS and AppStream repos"
            repo_url="${baseurl}/compose/BaseOS/${ARCH}/os/,${baseurl}/compose/AppStream/${ARCH}/os/"
        fi
        echo "Repo url is $repo_url"
    fi

    if [[ $compose_id =~ 'UNSPECIFIED' ]]; then
        if ! [ -z ${BRANCH_NAME} ]; then
            compose_id=${BRANCH_NAME}
        fi
    fi

    compose_parse_file="$WORKSPACE/job_env.yaml"
    echo """\
COMPOSE_ID: ${compose_id}
COMPOSE_URL: ${baseurl}
COMPOSEID_URL: ${COMPOSEID_URL}
REPO_URL: ${repo_url}
PKG_URL: ${PKG_URL}
BRANCH_NAME: ${BRANCH_NAME}
ARCH: ${ARCH}
OWNER: ${build_owner}
SCRATCH: false
EMAIL: xen-qe-list@redhat.com
NAME: AWS-auto-CI""" >> ${compose_parse_file}

env_file="$WORKSPACE/job_env.txt"
    echo """\
COMPOSE_ID=${compose_id}
COMPOSE_URL=${baseurl}
COMPOSEID_URL=${COMPOSEID_URL}
REPO_URL=${repo_url}
PKG_URL=${PKG_URL}
BRANCH_NAME=${BRANCH_NAME}
OWNER=${build_owner}
SCRATCH=false""" >> ${env_file}
if ! [ -z ${BASE_AMI} ]; then
        echo """\
JOB_BASE_AMI: ${BASE_AMI}""" >> ${compose_parse_file}
        echo """\
JOB_BASE_AMI=${BASE_AMI}""" >> $WORKSPACE/job_env.txt
    fi
if ! [ -z ${INSTANCE_TYPES} ]; then
        echo """\
JOB_INSTANCE_TYPES: ${INSTANCE_TYPES}""" >> ${compose_parse_file}
        echo """\
JOB_INSTANCE_TYPES=${INSTANCE_TYPES}""" >> $WORKSPACE/job_env.txt
    fi
'''
}