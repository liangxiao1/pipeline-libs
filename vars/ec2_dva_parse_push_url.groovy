// identify target from task url
def call() {
    sh '''
     #!/bin/bash -x
    if [[ -e $WORKSPACE/job_env.txt ]]; then
        source $WORKSPACE/job_env.txt
    fi
    task_body=$(curl $JOB_TASK_URL|grep aws)
    if [[ $task_body =~ "aws-china" ]]; then
        target=aws-china
    elif [[ $task_body =~ "aws-us-gov" ]]; then
        target=aws-us-gov
    else
        target=aws
    fi
    echo """
JOB_TARGET: $target
""">> $WORKSPACE/job_env.yaml
    '''
}