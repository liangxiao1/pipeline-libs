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
    if ! [ -z $BRANCH_NAME ]; then
        branch_name=$BRANCH_NAME
    else
        branch_name='UNSPECIFIED'
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
        umb_testresult="passed"
        testcomment="PASS"
    else
        testresult="failed"
        umb_testresult="failed"
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
UMB_TESTRESULT: "$umb_testresult"
TESTSUITE: avocado-cloud
AUTOCHECK: \\"$autocheck\\"
""">> $WORKSPACE/job_env.yaml
    '''
}