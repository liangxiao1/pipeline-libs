// Run avocado-cloud ec2 test
// Rememeber to keep them up to date.
// Below env's are required
// KEY_NAME=''(keypair name on aws)
// KEYFILE=''(ssh keyfile realpath)
// Below params are required
// params.EC2_REGION=''
// params.EC2_PROFILE=""
// params.EC2_SUBNET=""
// params.EC2_SG_GROUP=""
// params.PROXY_URL=""
// params.ARCH='x86_64|aarch64'
// params.RUN_CASES=''

def call() {
    sh '''
    #!/bin/bash
    source $WORKSPACE/job_env.txt
    log_dir="/home/ec2/logs/${BUILDNAME}_${ARCH}"
    mkdir -p $log_dir
    source /home/ec2/p3_venv/bin/activate
    cd /home/ec2/mini_utils/
    echo "Generate instance types yaml file in /tmp"
    instance_num=1
    if [[ ${INSTANCE_NUM} == '' ]];then
        if [[ $COMPOSE_ID =~ '.n.' ]] || [[ $COMPOSE_ID =~ '.t.' ]] || [[ $COMPOSE_ID =~ "update" ]] || [[ $COMPOSE_ID =~ "RHEL-9" ]]||[[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
            instance_num=1
        else
            instance_num=1
        fi
    else
        instance_num=${INSTANCE_NUM}
    fi
    instances_yaml=$WORKSPACE/compose_${ARCH}.yaml
    instances_sum_yaml=$WORKSPACE/sum_compose_${ARCH}.yaml
    echo "get instance type spec"
    if [[ -z $JOB_INSTANCE_TYPES ]]; then
        if [[ $COMPOSE_ID =~ 'RHEL-6' ]]|[[ $COMPOSE_ID =~ 'RHEL-7' ]]|[[ $COMPOSE_ID =~ 'RHEL-8.0' ]]|[[ $COMPOSE_ID =~ 'RHEL-8.1' ]]; then
            echo "Skip a1.metal instance as not support prior RHEL8.2"
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r --skip_instance a1.metal \
            -f ${instances_yaml} --num_instances $instance_num --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
        elif [[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r \
            -f ${instances_yaml} --num_instances $instance_num --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c --max_mem 16
        elif ! [ -z $JOB_INFO_BUILD_ID ]; then
            # virt-what test t2.small,t3.small,z1d.metal,t4g.small,m6g.metal instances
            if [[ ${JOB_INFO_PACKAGE_NAME} =~ 'virt-what' ]]; then
                instances='t2.small,t3.small,t4g.small,c6g.medium,m6g.metal,z1d.metal'
                RUN_CASES='virtwhat'
            elif [[ ${JOB_INFO_PACKAGE_NAME} =~ 'kernel' ]]; then
                # c4.large for kernel 2184745
                instances='c4.large,t3.small,t4g.small,c6g.medium'
                RUN_CASES=${DEFALUT_RUN_CASES}
            else
                instances='t3.small,t4g.small,c6g.medium'
                RUN_CASES=${DEFALUT_RUN_CASES}
            fi
            if [[ -z $RUN_CASES ]]; then
                RUN_CASES=${JOB_INFO_PACKAGE_NAME/'-'/'_'}
            fi
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -t $instances \
                 -f ${instances_yaml} --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
                 ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c -${ARCH}
        else
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r \
            -f ${instances_yaml} --num_instances $instance_num --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
        fi
    else
        echo "use specified $JOB_INSTANCE_TYPES"
        # python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -t $JOB_INSTANCE_TYPES \
        # -f ${instances_yaml} --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
        # ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
    fi

    if [[ -z $JOB_INSTANCE_TYPES ]]; then
        job_instance_types=$(cat ${instances_sum_yaml})
        echo """\
JOB_INSTANCE_TYPES: $job_instance_types""" >> $WORKSPACE/job_env.yaml
    JOB_INSTANCE_TYPES=$job_instance_types
    fi
    ssh_user='ec2-user'
    # From CentOS-Stream-9, its default user is changed to ec2-user
    if [[ $COMPOSE_ID =~ 'CentOS-Stream-8' ]]; then
        ssh_user='centos'
        echo "$ssh_user"
    fi
    #cat ${instances_yaml}
    volume_size=10
    if ! [[ -z ${CERT_PRODUCT_ID} ]]; then
        volume_size=20
    fi
    test_date=$(date +%Y%m%d)
    for instance in ${JOB_INSTANCE_TYPES//,/ }; do
        echo """\
Cloud:
    provider : aws
profile_name : ${EC2_PROFILE}
ami_id : ${IMAGE}
additionalinfo : ${EC2_ADDITIONALINFO}
region : ${EC2_REGION}
availability_zone : "${EC2_REGION}a"
subnet_id_ipv6 : ${EC2_SUBNET} 
subnet_id_ipv4 : ${EC2_SUBNET} 
security_group_ids : ${EC2_SG_GROUP}
ssh_key_name : ${KEY_NAME}
ec2_tagname : virtqe_auto_cloud
instance_type: ${instance}
volume_size: ${volume_size}
        """ >  $WORKSPACE/aws_${instance}.yaml
        if ! [[ -z $CPUS ]] && ! [[ $CPUS =~ 'null' ]]; then
            echo "cpus: ${CPUS}" >> $WORKSPACE/aws_${instance}.yaml
        fi
        if ! [[ -z $MEMORY ]] && ! [[ $MEMORY =~ 'null' ]]; then
            echo "memory: ${MEMORY}" >> $WORKSPACE/aws_${instance}.yaml
        fi
        if ! [[ -z $DISKS ]] && ! [[ $DISKS =~ 'null' ]]; then
            echo "disks: ${DISKS}" >> $WORKSPACE/aws_${instance}.yaml
        fi
        if ! [[ -z $NET_BANDWIDTH ]] && ! [[ $NET_BANDWIDTH =~ 'null' ]]; then
            echo "net_bandwidth: ${NET_BANDWIDTH}" >> $WORKSPACE/aws_${instance}.yaml
        fi
        if ! [[ -z ${CERT_PRODUCT_ID} ]]; then
            echo "placement_group_name : xiliang_place" >> $WORKSPACE/aws_${instance}.yaml
        fi
        cmd_options="--user $ssh_user --keyfile ${KEYFILE} --platform_profile $WORKSPACE/aws_${instance}.yaml --result $WORKSPACE/os_tests_result_${instance} -p ${RUN_CASES}"
        if ! [[ -z $SKIP_CASES ]] && ! [[ $SKIP_CASES =~ 'null' ]]; then
            cmd_options="${cmd_options} -s ${SKIP_CASES}"
        fi
        if ! [[ -z $OS_TESTS_EXTRA_OPTIONS ]] && ! [[ $OS_TESTS_EXTRA_OPTIONS =~ 'null' ]]; then
            cmd_options="${cmd_options} ${OS_TESTS_EXTRA_OPTIONS}"
        fi
        os-tests ${cmd_options}
        
        cat $WORKSPACE/os_tests_result_${instance}/results/sum.log >> $WORKSPACE/total_sum.log
        if ! [[ -z $NFS_SERVER ]]; then
            echo "Save log to remote ${NFS_SERVER}"
            if ! [[ -d ${NFS_MOUNT_POINT} ]]; then
                echo "${NFS_MOUNT_POINT} not found, create it"
                mkdir ${NFS_MOUNT_POINT}
            fi
            if ! mount|grep ${NFS_MOUNT_POINT}; then
                echo "nfs not mount, mount it"
                sudo mount  ${NFS_SERVER}:${NFS_PATH_DIR} ${NFS_MOUNT_POINT}
            fi
            mkdir -p ${NFS_MOUNT_POINT}/os_tests/$test_date/$WORKSPACE
            cp -r $WORKSPACE/os_tests_result_${instance} ${NFS_MOUNT_POINT}/os_tests/$test_date/$WORKSPACE   
       fi
    done
    if ! [[ -z $NFS_SERVER ]]; then
        echo "HTMLURL: http://${NFS_SERVER}/results/iscsi/os_tests/$test_date/${WORKSPACE}" >> $WORKSPACE/job_env.yaml
    else
        echo "HTMLURL: NotSetNFS_SEVER" >> $WORKSPACE/job_env.yaml
    fi
    if ! [[ -z ${CERT_PRODUCT_ID} ]]; then
        echo "get all certification xml files"
        cert_logs=$(find $WORKSPACE/os_tests_result_${instance}/attachments -name *rhcert*xml)
        sed -i "/CERT_CERT_ATTACHMENT/d" $WORKSPACE/job_env.yaml
        sed -i "/CERT_CERT_ATTACHMENT/d" $WORKSPACE/job_env.txt
        echo """\
CERT_CERT_ATTACHMENT: '${cert_logs}'""" >> $WORKSPACE/job_env.yaml
echo """\
CERT_CERT_ATTACHMENT='${cert_logs}'""" >> $WORKSPACE/job_env.txt
    fi
    deactivate
    testresult=''
    umb_testresult="passed"
    if ${UPLOAD_REPORTPORTAL}; then
        launchids=''
        echo "upload test result to reportportal"
        source /home/ec2/rp_preproc_venv/bin/activate
        for instance in ${JOB_INSTANCE_TYPES//,/ }; do
            
            cp /home/ec2/mini_utils/data/reportportal.json $WORKSPACE
            sed -i "s/RP_TOKEN/${RP_TOKEN}/g" $WORKSPACE/reportportal.json
            sed -i "s/PROJECT/aws/g" $WORKSPACE/reportportal.json
            sed -i "s/RELEASE/${COMPOSE_ID}/g" $WORKSPACE/reportportal.json
            sed -i "s/INSTANCE/${instance}/g" $WORKSPACE/reportportal.json
            sed -i "s/ARCH/${ARCH}/g" $WORKSPACE/reportportal.json
            sed -i "s/IS_NEW/${IS_NEW_INSTANCE}/g" $WORKSPACE/reportportal.json
            debuglogurl="http://${NFS_SERVER}/results/iscsi/os_tests/$test_date/${WORKSPACE}"
            sed -i "s|HTMLURL|${debuglogurl}|g" $WORKSPACE/reportportal.json
            rp_preproc -c $WORKSPACE/reportportal.json -d $WORKSPACE/os_tests_result_${instance} --debug > $WORKSPACE/${instance}.json 2>&1
            launchid=$(cat  $WORKSPACE/${instance}.json |jq .reportportal.launches[0])
            launchids="$launchid $launchids"
            curl  -X POST -H "Authorization: bearer ${RP_TOKEN}" ${LOG_SERVER}/api/v1/aws/launch/analyze --header 'Content-type: application/json' -d '{"analyzeItemsMode": [  "TO_INVESTIGATE"],"analyzerMode": "ALL","analyzerTypeName": "autoAnalyzer","launchId": '$launchid'}'
        done
        sleep 120
        if ${ENABLE_TFA}; then
            cd $WORKSPACE
            for launchid in $launchids;do
                # tfacon
                /root/xen-ci/utils/tfacon.sh ${RP_TOKEN} aws $launchid
            done
            sleep 120
        fi
        curl  -o  $WORKSPACE/defects_type.json -X GET -H "Authorization: bearer ${RP_TOKEN}" --header 'Accept: application/json'  ${LOG_SERVER}/api/v1/aws/settings
        for launchid in $launchids;do
            curl  -o  $WORKSPACE/${launchid}.json -X GET -H "Authorization: bearer ${RP_TOKEN}" --header 'Accept: application/json'  ${LOG_SERVER}/api/v1/aws/launch/$launchid
            instance=$(cat $WORKSPACE/${launchid}.json|jq '.attributes[]|select(.key|match("^instance")).value')
            total=$(cat $WORKSPACE/${launchid}.json|jq .statistics.executions.total)
            passed=$(cat $WORKSPACE/${launchid}.json|jq .statistics.executions.passed)
            failed=$(cat $WORKSPACE/${launchid}.json|jq .statistics.executions.failed)
            skipped=$(cat $WORKSPACE/${launchid}.json|jq .statistics.executions.skipped)
            to_investigate=$(cat $WORKSPACE/${launchid}.json|jq .statistics.defects.to_investigate.total)
            if ! [[ $to_investigate == null ]]; then
                umb_testresult="failed"
            else
                to_investigate=0
            fi
            if [[ $failed == null ]]; then
                failed=0
            fi
            if [[ $skipped == null ]]; then
                skipped=0
            fi
            if [[ $passed == null ]]; then
                passed=0
            fi
            testresult=$(cat <<-END
    InstanceType:${instance}
    Total:${total}
    Passed:${passed:=0}
    Failed:${failed:=0}
    Skipped:${skipped:=0}
    To_investigate:${to_investigate:=0}
    ReportPortal:${LOG_SERVER}/ui/#aws/launches/all/${launchid}
    ${testresult}
END
)
        done
        #cat $WORKSPACE/defects_type.json |jq '.subTypes.SYSTEM_ISSUE[]|select(.locator|match("si_1iexrfknerm92")).longName'
        deactivate
    fi
    pass=$(grep PASS $WORKSPACE/total_sum.log|wc -l)
    failures=$(grep FAIL $WORKSPACE/total_sum.log|wc -l)
    errors=$(grep ERROR $WORKSPACE/total_sum.log|wc -l)
    total=$((pass+errors+failures))
    echo """
TESTRESULT: > 
$testresult
PASS: $pass
FAILURES: $failures
ERRORS: $errors
TOTAL: $total
UMB_TESTRESULT: "$umb_testresult"
TESTSUITE: https://github.com/virt-s1/os-tests
""">> $WORKSPACE/job_env.yaml
    
    '''
}