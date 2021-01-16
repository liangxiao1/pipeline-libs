// Get compose information from ci message or compose url
// Below env's are required
// params.ARCH='x86_64|aarch64'

def call() {
    sh '''
    #!/bin/bash -x
    if [[ ${BUILD_CAUSE} =~ 'XTRIGGERCAUSE' ]]; then
        echo "This is url trigger!"
        build_owner="URL_TRIGGER"
        url_trigger_log=${JOB_URL}/urltriggerPollLog/
        url_out=`curl $url_trigger_log`
        trigger_url=`echo $url_out|grep COMPOSE |sed  's/.*\\(http.*latest-RHEL-[0-9]\\).*/\\1/'`
    elif ! [ -z ${CI_MESSAGE} ]; then
        echo "This is triggerd by CI message."
        build_owner="UMB_TRIGGER"
        echo "CI Message: ${CI_MESSAGE}"
        python /home/ec2/mini_utils/json_parser.py -c "${CI_MESSAGE}" --dir "${WORKSPACE}" -d
        source ${WORKSPACE}/job_env.txt
        baseurl=$JOB_LOCATION
        baseurl=${baseurl//'"'}
        baseurl=${baseurl%/compose/}
        baseurl=${baseurl%/compose}
        COMPOSEID_URL="${baseurl}/COMPOSE_ID"
        export trigger_url=${COMPOSEID_URL}
    elif ! [ -z ${COMPOSEID_URL} ]; then
        echo "This is triggerd by manually."
        build_owner="MANUAL_TRIGGER"
        export trigger_url=${COMPOSEID_URL}
    fi
    if [ -z ${trigger_url} ]; then
        echo "compose url not found, do not update image"
        compose_id="UNSPECIFIED"
        baseurl="UNSPECIFIED"
        COMPOSEID_URL="UNSPECIFIED"
        repo_url="UNSPECIFIED"
    else
        compose_id=`curl ${trigger_url}`
        baseurl=${trigger_url%COMPOSE_ID}
        if [[ "${compose_id}" =~ "RHEL-8" ]];then
            echo "This is RHEL-8 compose build"
            repo_url="${baseurl}/compose/BaseOS/${ARCH}/os/,${baseurl}/compose/AppStream/${ARCH}/os/"
        elif [[ "$compose_id" =~ "RHEL-7" ]];then
            echo "This is RHEL-7 compose build"
            repo_url="$baseurl/compose/Server/${ARCH}/os/"
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