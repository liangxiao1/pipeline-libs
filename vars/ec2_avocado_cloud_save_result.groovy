// Below env's are required
// NFS_SERVER=xxx
// Below params are required
// params.ARCH='x86_64|aarch64'
// Save avocado-cloud test result to remote server
// copy test result to ${NFS_SERVER}:/var/www/html/results/iscsi/ec2_xen/ci_log
// test result parser: get PASS,ERRORS,FAILURES
def call() {
    sh '''
    #!/bin/bash -x
    source $WORKSPACE/job_env.txt
    if [[ "${COMPOSE_ID}" =~ "RHEL-8.6" ]];then
        branch_name='RHEL8.6'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-8.7" ]];then
        branch_name='RHEL8.7'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-8.2" ]];then
        branch_name='RHEL8.2'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-8.3" ]];then
        branch_name='RHEL8.3'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-8.4" ]];then
        branch_name='RHEL8.4'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-8.5" ]];then
        branch_name='RHEL8.5'
    elif [[ "${COMPOSE_ID}" =~ "CentOS-Stream-8" ]];then
        branch_name='CentOS-Stream-8'
    elif [[ "${COMPOSE_ID}" =~ "CentOS-Stream-9" ]];then
        branch_name='CentOS-Stream-9'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-9.0" ]];then
        branch_name='RHEL9.0'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-9.1" ]];then
        branch_name='RHEL9.1'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-9.2" ]];then
        branch_name='RHEL9.2'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-7.8" ]];then
        branch_name='RHEL7.8'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-7.9" ]];then
        branch_name='RHEL7.9'
    elif [[ "${COMPOSE_ID}" =~ "RHEL-7.10" ]];then
        branch_name='RHEL7.10'
    elif [[ "${COMPOSE_ID}" =~ "UNSPECIFIED" ]];then
        branch_name='RHEL8'
    fi

    # Copy result to remote disk
    LOGDIR="/home/ec2/logs/${BUILDNAME}_${ARCH}/"
    remote_folder_name="$LOGDIR"
    remote_folder="/var/www/html/results/iscsi/ec2_xen/ci_log/${remote_folder_name}"
    remote_ip=${NFS_SERVER}
    ssh root@${remote_ip} "mkdir -p ${remote_folder}"
    result_dir=$(realpath $LOGDIR/latest|awk -F'/' '{print $NF}')
    cd $LOGDIR/
    tar -zcf ${result_dir}.tgz $result_dir
    scp  ${result_dir}.tgz root@${remote_ip}:${remote_folder}
    ssh  root@${remote_ip} "cd ${remote_folder};tar -zxf ${result_dir}.tgz;rm -f ${result_dir}.tgz"
    echo "HTMLURL: http://${remote_ip}/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir}/results.html" >> $WORKSPACE/job_env.yaml
    auto_analyze=$(ssh root@${remote_ip} "python /home/xiliang/mini_utils/ec2_test.py --db_file /var/www/report_viewer/app.db --dir /var/www/html/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir}" 2>&1)
    autocheck=$(ssh root@${remote_ip} "cat /var/www/html/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir}/autocheck.log")
    # Analyze test result
    result_json="$LOGDIR/latest/results.json"
    pass=`cat ${result_json}|jq .pass`
    failures=`cat ${result_json}|jq .failures`
    errors=`cat ${result_json}|jq .errors`
    total=$((pass+errors+failures))
    if [[ $pass == $total ]];then
        testresult="PASS"
        testcomment="PASS"
    else
        testresult="failed"
        testcomment="FAILED"
        echo $auto_analyze
        if [[ "$auto_analyze" =~ "No similar failure" ]];then
            testresult="FAIL"
            testcomment="FAIL"
        else
            if [[ "$auto_analyze" =~ "Product blockerbug" ]];then
                testresult="Partial FAIL as Know Blocker Bug Found"
                testcomment="PartialFAILasKnowBlockerBugFound"
            elif [[ "$auto_analyze" =~ "Product bug" ]];then
                testresult="Conditional PASS as Know Product Bug Found"
                testcomment="ConditionalPASSasKnowProductBugFound"
            else
                testresult="PASS as No New Exception Found"
                testcomment="PASSasNoNewExceptionFound"
            fi
        fi
    fi
    testrun="${BUILD_ID}_${BUILDNAME}x86_64"
    if ! [ -z ${INSTANCE_DATE} ]; then
        instance_date=${INSTANCE_DATE}
        ssh  root@${remote_ip} "python3 /var/www/report_viewer/utils/ec2_report_write.py --dir /var/www/html/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir} --report_url http://${remote_ip}/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir}/results.html --branch_name $branch_name --compose-id $COMPOSE_ID --comments "acceptance,cijob,$testcomment" --db_file /var/www/report_viewer/app.db --testrun ${testrun} --instance_available_date ${instance_date}"
    else
        ssh  root@${remote_ip} "python3 /var/www/report_viewer/utils/ec2_report_write.py --dir /var/www/html/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir} --report_url http://${remote_ip}/results/iscsi/ec2_xen/ci_log/${remote_folder_name}/${result_dir}/results.html --branch_name $branch_name --compose-id $COMPOSE_ID --comments "acceptance,cijob,$testcomment" --db_file /var/www/report_viewer/app.db --testrun ${testrun}"
    fi
    echo """
PASS: $pass
FAILURES: $failures
ERRORS: $errors
TOTAL: $total
TESTRESULT: "$testresult"
TESTSUITE: avocado-cloud
AUTOCHECK: '$autocheck'
""">> $WORKSPACE/job_env.yaml
    '''
}