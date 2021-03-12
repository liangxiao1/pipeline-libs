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
    #!/bin/bash -x
    source $WORKSPACE/job_env.txt
    log_dir="/home/ec2/logs/${BUILDNAME}_${ARCH}"
    mkdir -p $log_dir
    source /home/ec2/ec2_venv/bin/activate
    cd /home/ec2/mini_utils/
    echo "Generate instance types yaml file in /tmp"
    instance_num=1
    if [[ ${INSTANCE_NUM} == '' ]];then
        if [[ $COMPOSE_ID =~ '.n.' ]] || [[ $COMPOSE_ID =~ '.t.' ]] || [[ $COMPOSE_ID =~ "update" ]] || [[ $COMPOSE_ID =~ "RHEL-9" ]]; then
            instance_num=1
        else
            instance_num=5
        fi
    else
        instance_num=${INSTANCE_NUM}
    fi
    echo "get instance type spec"
    if ! [[ ${PREVIEW_INSTANCE_TYPES} =~ 'xxxx' ]]; then
        echo "manually instance spec specified, use it"
        echo -e ${PREVIEW_INSTANCE_TYPES} > /tmp/compose_${ARCH}.yaml
    else
        if [[ -z $JOB_INSTANCE_TYPES ]]; then
            if [[ $COMPOSE_ID =~ 'RHEL-6' ]]|[[ $COMPOSE_ID =~ 'RHEL-7' ]]|[[ $COMPOSE_ID =~ 'RHEL-8.0' ]]|[[ $COMPOSE_ID =~ 'RHEL-8.1' ]]; then
                echo "Skip a1.metal instance as not support prior RHEL8.2"
                python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r --skip_instance a1.metal \
                -f /tmp/compose_${ARCH}.yaml --num_instances $instance_num --region ${EC2_REGION} --key_name {KEY_NAME} --security_group_ids \
                ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
            else
                python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -${ARCH} -r \
                -f /tmp/compose_${ARCH}.yaml --num_instances $instance_num --region ${EC2_REGION} --key_name {KEY_NAME} --security_group_ids \
                ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
            fi
        else
            python ec2_instance_select.py --profile ${EC2_PROFILE} --ami-id $IMAGE -t $JOB_INSTANCE_TYPES \
            -f /tmp/compose_${ARCH}.yaml --region ${EC2_REGION} --key_name {KEY_NAME} --security_group_ids \
            ${EC2_SG_GROUP} --subnet_id ${EC2_SUBNET} --zone ${EC2_REGION}a -c
        fi
    fi
    if [[ -z $JOB_INSTANCE_TYPES ]]; then
        job_instance_types=$(cat /tmp/sum_compose_${ARCH}.yaml)
        echo """\
JOB_INSTANCE_TYPES: $job_instance_types""" >> $WORKSPACE/job_env.yaml
    fi
    ssh_user='ec2-user'
    if [[ $COMPOSE_ID =~ 'CentOS-Stream' ]]; then
        ssh_user='centos'
    fi
    if ! [[ ${PREVIEW_INSTANCE_TYPES} =~ 'xxxx' ]]; then
        echo "manually instance spec specified, use it"
        echo -e ${PREVIEW_INSTANCE_TYPES} > /tmp/compose_${ARCH}.yaml
    fi
    cat /tmp/compose_${ARCH}.yaml
    if [[ -z ${EC2_ADDITIONALINFO} ]]; then
        python ec2_test_run.py --profile ${EC2_PROFILE} --ami-id $IMAGE --key_name ${KEY_NAME} --security_group_ids ${EC2_SG_GROUP} --ssh_user $ssh_user\
        --subnet_id ${EC2_SUBNET} --region ${EC2_REGION} --zone "${EC2_REGION}a" --casetag ${RUN_CASES} --result_dir $log_dir \
        --instance_yaml /tmp/compose_${ARCH}.yaml --timeout 1152000
    else
        python ec2_test_run.py --profile ${EC2_PROFILE} --ami-id $IMAGE --key_name ${KEY_NAME} --security_group_ids ${EC2_SG_GROUP} --ssh_user $ssh_user\
        --subnet_id ${EC2_SUBNET} --region ${EC2_REGION} --zone "${EC2_REGION}a" --casetag ${RUN_CASES} --result_dir $log_dir \
        --instance_yaml /tmp/compose_${ARCH}.yaml --timeout 1152000 --additionalinfo ${EC2_ADDITIONALINFO}
    fi
    deactivate
    rm -rf /tmp/compose_${ARCH}.yaml
    rm -rf /tmp/sum_compose_${ARCH}.yaml
    '''
}