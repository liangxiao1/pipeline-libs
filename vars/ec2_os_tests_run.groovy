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
    echo "get instance type spec"
    if [[ -z $JOB_INSTANCE_TYPES ]]; then
        if [[ $COMPOSE_ID =~ 'RHEL-6' ]]|[[ $COMPOSE_ID =~ 'RHEL-7' ]]|[[ $COMPOSE_ID =~ 'RHEL-8.0' ]]|[[ $COMPOSE_ID =~ 'RHEL-8.1' ]]; then
            echo "Skip a1.metal instance as not support prior RHEL8.2"
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r --skip_instance a1.metal \
            -f /tmp/compose_${ARCH}.yaml --num_instances $instance_num --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
        elif [[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r \
            -f /tmp/compose_${ARCH}.yaml --num_instances $instance_num --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c --max_mem 16
        elif ! [ -z $JOB_INFO_BUILD_ID ]; then
            RUN_CASES=${JOB_INFO_PACKAGE_NAME/'-'/'_'}
            # virt-what test t2.small,t3.small,z1d.metal,t4g.small,m6g.metal instances
            if [[ ${JOB_INFO_PACKAGE_NAME} =~ 'virt-what' ]]; then
                # skip t2.small for now as bz1986909
                instances='t3.small,t4g.small,c6g.medium,m6g.metal,z1d.metal'
            else
                instances='t3.small,t4g.small,c6g.medium'
            fi
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -t $instances \
                 -f /tmp/compose_${ARCH}.yaml --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
                 ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c -${ARCH}
        else
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r \
            -f /tmp/compose_${ARCH}.yaml --num_instances $instance_num --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
        fi
    else
        python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -t $JOB_INSTANCE_TYPES \
        -f /tmp/compose_${ARCH}.yaml --region ${EC2_REGION} --key_name ${KEY_NAME} --security_group_ids \
        ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
    fi

    if [[ -z $JOB_INSTANCE_TYPES ]]; then
        job_instance_types=$(cat /tmp/sum_compose_${ARCH}.yaml)
        echo """\
JOB_INSTANCE_TYPES: $job_instance_types""" >> $WORKSPACE/job_env.yaml
    JOB_INSTANCE_TYPES=$job_instance_types
    fi
    ssh_user='ec2-user'
    if [[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
        #ssh_user='centos'
        echo "$ssh_user"
    fi
    cat /tmp/compose_${ARCH}.yaml
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
        """ >  $WORKSPACE/aws_${instance}.yaml
        os-tests --user $ssh_user --keyfile ${KEYFILE} --platform_profile $WORKSPACE/aws_${instance}.yaml --result $WORKSPACE/os_tests_result_${instance} -p ${RUN_CASES}
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
    deactivate
    if ${UPLOAD_REPORTPORTAL}; then
        echo "upload test result to reportportal"
        for instance in ${JOB_INSTANCE_TYPES//,/ }; do
            source /home/ec2/rp_preproc_venv/bin/activate
            cp /home/ec2/mini_utils/data/reportportal.json $WORKSPACE
            sed -i "s/RP_TOKEN/${RP_TOKEN}/g" $WORKSPACE/reportportal.json
            sed -i "s/PROJECT/aws/g" $WORKSPACE/reportportal.json
            sed -i "s/RELEASE/${COMPOSE_ID}/g" $WORKSPACE/reportportal.json
            sed -i "s/INSTANCE/${instance}/g" $WORKSPACE/reportportal.json
            sed -i "s/ARCH/${ARCH}/g" $WORKSPACE/reportportal.json
            rp_preproc -c $WORKSPACE/reportportal.json -d $WORKSPACE/os_tests_result_${instance} --debug
            deactivate
        done
    fi
    pass=$(grep PASS $WORKSPACE/total_sum.log|wc -l)
    failures=$(grep FAIL $WORKSPACE/total_sum.log|wc -l)
    errors=$(grep ERROR $WORKSPACE/total_sum.log|wc -l)
    total=$((pass+errors+failures))
    echo """
PASS: $pass
FAILURES: $failures
ERRORS: $errors
TOTAL: $total
UMB_TESTRESULT: "$umb_testresult"
TESTSUITE: https://github.com/virt-s1/os-tests
""">> $WORKSPACE/job_env.yaml
    
    '''
}