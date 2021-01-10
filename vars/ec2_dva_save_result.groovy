// copy test result to remote
// test result parser: get PASS,ERRORS,FAILURES
// NFS_SERVER=xxx
// NFS_PATH_DIR=xxx
// NFS_MOUNT_POINT=xxx
def call() {
    sh '''
    #!/bin/bash -x
    if [[ -e $WORKSPACE/job_env.txt ]]; then
        source $WORKSPACE/job_env.txt
    fi

    # Copy result to remote nfs
    LOGDIR="${WORKSPACE}/dva_result"

    mkdir -p ${NFS_MOUNT_POINT}/ami_val/$WORKSPACE
    cp -r $LOGDIR ${NFS_MOUNT_POINT}/ami_val/$WORKSPACE
    remote_ip=${NFS_SERVER}
    echo "HTMLURL: http://${remote_ip}/results/iscsi/ami_val/${LOGDIR}/" >> $WORKSPACE/job_env.yaml

    # Analyze test result
    cd $LOGDIR
    total=$(grep stage[0-9] *_result.yaml|wc -l)
    pass=$(grep stage *_result.yaml|grep passed|wc -l)
    failures=$(grep stage *_result.yaml|grep failed|wc -l)
    skip=$(grep stage *_result.yaml|grep skip|wc -l)
    if [[ $pass == $total ]];then
        testresult="passed"
    else
        testresult="failed"
    fi
    echo """
PASS: $pass
FAILURES: $failures
SKIP: $skip
TOTAL: $total
TESTRESULT: $testresult
TESTSUITE: dva
""">> $WORKSPACE/job_env.yaml
    '''
}