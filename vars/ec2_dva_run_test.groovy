// run dva test
def call() {
    sh '''
    #!/bin/bash -x
    #mv -f ${WORKSPACE}/dva.yaml /etc/
    if [[ -e $WORKSPACE/job_env.txt ]]; then
        source $WORKSPACE/job_env.txt
    fi
    cd ${WORKSPACE}
    cd ..
    source dva_venv/bin/activate
    mkdir ${WORKSPACE}/dva_result
    result_dir=${WORKSPACE}/dva_result
    cd ${WORKSPACE}
    image_files=$(ls task_*.yaml)
    for image_file in $image_files;do
        echo $image_file
        dva -c $DVA_CFG validate -i $image_file -o dva_result/${image_file%.yaml}_fulllog.yaml -T testcase_99_package_version_check testcase_63_sriov testcase_08_memory testcase_11_package_set testcase_13_resize2fs
        dva summary -i dva_result/${image_file%.yaml}_fulllog.yaml > dva_result/${image_file%.yaml}_sum.yaml
        cat dva_result/${image_file%.yaml}_sum.yaml
        dva result -i dva_result/${image_file%.yaml}_fulllog.yaml > dva_result/${image_file%.yaml}_result.yaml || true
        cat dva_result/${image_file%.yaml}_result.yaml
    done
    deactivate

    '''
}