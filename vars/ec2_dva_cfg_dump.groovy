// get image id and generate configuration file for dva run
def call() {
    def job_env = readYaml file: "job_env.yaml"
    sh("""
        cd ${WORKSPACE}
        cd ..
        if [[ -e p3_venv/bin/activate ]]; then
            source p3_venv/bin/activate
        else
            virtualenv -p python3 p3_venv
            source p3_venv/bin/activate
        fi
        source p3_venv/bin/activate
        pip install -U boto3
        pip install -U PyYAML
        python ${WORKSPACE}/mini_utils/dva_dump_images.py --task_url ${job_env.JOB_TASK_URL} --dir ${WORKSPACE} -d
        python ${WORKSPACE}/mini_utils/dva_config_generate.py --pubkeyfile ${WORKSPACE}/data/virtqe_s1.pub --sshkeyfile ${WORKSPACE}/data/virtqe_s1.pem --target ${job_env.JOB_TARGET} --tokenfile /etc/dva_keys.yaml --output ${job_env.DVA_CFG}
        python ${WORKSPACE}/mini_utils/amis_status_check.py --task_url ${job_env.JOB_TASK_URL}
        deactivate
        """
    )
}