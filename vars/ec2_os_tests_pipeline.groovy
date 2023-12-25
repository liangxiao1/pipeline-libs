def call(Map pipelineParams) {
    pipeline {
        agent {
            node {
                label "${pipelineParams.DEFAULT_JSLAVE}"
                customWorkspace "workspace/${env.JOB_NAME}-${env.BUILD_ID}"
            }
        }
        parameters {
            string(name: 'COMPOSEID_URL', defaultValue: '', description: 'this job can be triggerd by COMPOSEID_URL, CI_MESSAGE or BASE_AMI')
            string(name: 'CI_MESSAGE', defaultValue: '', description: 'this job can be triggerd by COMPOSEID_URL, CI_MESSAGE or BASE_AMI')
            string(name: 'BASE_AMI', defaultValue: '', description: 'this job can be triggerd by COMPOSEID_URL, CI_MESSAGE or BASE_AMI')
            choice(name: 'UPDATE_BASEAMI', choices: ['true', 'false'], description: 'do you want to upgrade BASE_AMI to the latest? if not, will use BASE_AMI in testing without any change.')
            choice(name: 'UPLOAD_REPORTPORTAL', choices: ['true', 'false'], description: 'Upload test result to reportportal or not.')
            choice(name: 'ENABLE_TFA', choices: ['false', 'true'], description: 'Call tfa to analyze test result in reportportal')
            choice(name: 'IS_NEW_INSTANCE', choices: ['false', 'true'], description: 'Set it to true if it is new instance(only for the first run)')
            string(name: 'POST_CMDS', defaultValue: 'uname', description: 'option, extra command executed when update baseami')
            string(name: 'ARCH', defaultValue: pipelineParams.DEFAULT_ARCH, description: 'x86_64|aarch64')
            string(name: 'PROXY_URL', defaultValue: pipelineParams.DEFAULT_PROXY_URL, description: 'proxy ip:port to access internal ip')
            string(name: 'INSTANCE_TYPES', defaultValue: pipelineParams.DEFAULT_INSTANCE_TYPES, description: 'option, specify instance types you want to test, seperate by comma')
            text (name: 'MORE_OS_TESTS_SETTING', defaultValue: '', description: 'Append more yaml settings to os-tests run')
            string(name: 'INSTANCE_NUM', defaultValue: '', description: 'option, how many instance to you want to test, default nightly compose is 1, production compose is 2')
            string(name: 'PKG_URL', defaultValue: '', description: 'option, specify pkgs url you want to install, for pkgs not in the repo')
            string(name: 'PKGS', defaultValue: pipelineParams.PKGS, description: 'option, specify pkg names you want to add or upgrade, for pkgs in the repo')
            choice(name: 'ENABLE_CERTREPO', choices: ['false', 'true'], description: 'enable cert repos or not')
            choice(name: 'IS_INSTALL_PKG_LIST', choices: ['true', 'false'], description: 'install extra pkgs required in run from default repo?')
            string(name: 'BRANCH_NAME', defaultValue: '', description: 'option, Specify branch name, eg. CentOS-Stream-8, RHEL-8.3')
            string(name: 'RUN_CASES', defaultValue: pipelineParams.DEFALUT_RUN_CASES, description: 'case tags, eg. acceptance, cloudinit, kernel_tier1 or one casename')
            string(name: 'SKIP_CASES', defaultValue: pipelineParams.DEFALUT_SKIP_CASES, description: 'not run case tags, eg. acceptance, cloudinit, kernel_tier1 or one casename')
            string(name: 'OS_TESTS_EXTRA_OPTIONS', defaultValue: '', description: 'append extra options to os-tests command line, https://github.com/virt-s1/os-tests/blob/master/os_tests/docs/os-tests_advanced_tips.md')
            choice(name: 'EC2_PROFILE', choices: ['default', pipelineParams.DEFAULT_EC2_PROFILE1, pipelineParams.DEFAULT_EC2_PROFILE2], description: 'account used for testing!')
            string(name: 'EC2_REGION', defaultValue: 'us-west-2', description: 'which region the test run in?')
            string(name: 'EC2_SUBNET', defaultValue: pipelineParams.DEFAULT_EC2_SUBNET, description: 'which subnet the test run in?')
            string(name: 'EC2_SUBNET_IPV6ONLY', defaultValue: pipelineParams.DEFAULT_EC2_SUBNET_IPV6ONLY, description: 'which ipv6-pnly subnet the test run in?')
            string(name: 'EC2_SG_GROUP', defaultValue: pipelineParams.DEFAULT_EC2_SG_GROUP, description: 'which security group the test run in?')
            string(name: 'EC2_ADDITIONALINFO', defaultValue: '', description: 'please split it by ";" if have more items')
            string(name: 'NFS_SERVER', defaultValue: pipelineParams.NFS_SERVER, description: 'nfs server to save log')
            string(name: 'NFS_PATH_DIR', defaultValue: pipelineParams.NFS_PATH_DIR, description: 'export path from nfs server')
            string(name: 'NFS_MOUNT_POINT', defaultValue: pipelineParams.NFS_MOUNT_POINT, description: 'mount point in local')
            text (defaultValue: pipelineParams.CERT_PARAMS, description: 'Only for CCSP test for certification.', name: 'CERT_PARAMS')
        }
        options {
            buildDiscarder(logRotator(numToKeepStr: '25'))
            timeout(time: 24, unit: 'HOURS') 
        }
        environment {
            PYTHONPATH="${env.WORKSPACE}"
            BASENAME="ec2auto"
            VM_PREFIX="${BASENAME}c${env.BUILD_ID}r"
            SRC_STORAGE="${BASENAME}images"
            DST_STORAGE="${BASENAME}eastus"
            SRC_GROUP="${SRC_STORAGE}"
            DST_GROUP="${DST_STORAGE}"
            DEFAULT_EC2_PROFILE1="${pipelineParams.DEFAULT_EC2_PROFILE1}"
            DEFAULT_EC2_PROFILE2="${pipelineParams.DEFAULT_EC2_PROFILE2}"
            DEFAULT_EC2_SUBNET="${pipelineParams.DEFAULT_EC2_SUBNET}"
            DEFAULT_EC2_SUBNET_IPV6ONLY="${pipelineParams.DEFAULT_EC2_SUBNET_IPV6ONLY}"            
            DEFAULT_EC2_SG_GROUP="${pipelineParams.DEFAULT_EC2_SG_GROUP}"
            DEFAULT_PROXY_URL="${pipelineParams.DEFAULT_PROXY_URL}"
            DEFAULT_JSLAVE="${pipelineParams.DEFAULT_JSLAVE}"
            DEFALUT_RUN_CASES="${pipelineParams.DEFALUT_RUN_CASES}"
            DEFALUT_SKIP_CASES="${pipelineParams.DEFALUT_SKIP_CASES}"
            DEFAULT_INSTANCE_TYPES="${env.INSTANCE_TYPES}"
            DEFAULT_ARCH="${env.ARCH}"
            OS_TESTS_EXTRA_OPTIONS="${env.OS_TESTS_EXTRA_OPTIONS}"
            KEY_NAME="${pipelineParams.KEY_NAME}"
            KEYFILE="${pipelineParams.KEYFILE}"
            DEFAULT_MAIL_SENDER="${pipelineParams.DEFAULT_MAIL_SENDER}"
            DEFAULT_MAIL_RECEIVER_SUCCESS="${pipelineParams.DEFAULT_MAIL_RECEIVER_SUCCESS}"
            DEFAULT_MAIL_RECEIVER_FAIL="${pipelineParams.DEFAULT_MAIL_RECEIVER_FAIL}"
            UMB_NAMESPACE="${pipelineParams.UMB_NAMESPACE}"
            UMB_TOPIC="${pipelineParams.UMB_TOPIC}"
            NFS_SERVER="${env.NFS_SERVER}"
            NFS_PATH_DIR="${env.NFS_PATH_DIR}"
            NFS_MOUNT_POINT="${env.NFS_MOUNT_POINT}"
            LOG_SERVER="${pipelineParams.LOG_SERVER}"
            TESTOWNER="${pipelineParams.TESTOWNER}"
            IS_INSTALL_PKG_LIST="${pipelineParams.IS_INSTALL_PKG_LIST}"
            //COMPOSE_LOCATION="${pipelineParams.COMPOSE_LOCATION}"
            BREW_BUILD_URL="${pipelineParams.BREW_BUILD_URL}"
            UPLOAD_REPORTPORTAL="${pipelineParams.UPLOAD_REPORTPORTAL}"
            ENABLE_TFA="${env.ENABLE_TFA}"
            RP_TOKEN=credentials("rp_serv_token")
            CERT_PARAMS="${env.CERT_PARAMS}"
        }
        stages {
            stage('url trigger'){
                when {
                     triggeredBy 'URLTrigger'
                }
                steps{
                    echo 'url triggered'
                    script {
                        env.COMPOSEID_URL = "https://mirror.stream.centos.org/9-stream/COMPOSE_ID"
                    }
            }
                
            }
            stage('Parse COMPOSEID URL') {
                steps {
                    cleanWs()
                    script {
                        ec2_parse_compose_url()
                        def ci_env = readYaml file: "job_env.yaml"
                        currentBuild.displayName = "${env.BUILD_ID}_${ci_env.COMPOSE_ID}"
                        if( "${ci_env.SCRATCH}" == 'true') {
                            currentBuild.result = 'ABORTED'
                            error('Scratch build. Aborted.')
                        }
                        //COMPOSE_ENV_YAML = readFile file: "job_env.yaml".trim()
                        if (env.CERT_PARAMS){
                            rhcert_product()
                            rhcert_cert_ticket()
                        }
                    }
                }
            }
            stage("Prepare image") {
                steps {
                    //checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'origin/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'avocado-cloud']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://code.engineering.redhat.com/gerrit/avocado-cloud']]]
                    //checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'origin/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'xen-ci']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://code.engineering.redhat.com/gerrit/xen-ci']]]
                    ec2_prepare_test_ami()
                }
            }
            stage ("Run Test") {
                steps {
                    ec2_os_tests_run()
                }
            }
        }
        post {
            always {
                archiveArtifacts 'job_env.yaml'
            }
            success {
                ec2_os_tests_mail_report(MAILSENDER: DEFAULT_MAIL_SENDER, MAILRECEIVER: DEFAULT_MAIL_RECEIVER_SUCCESS)
                script{
                    if (env.UMB_TOPIC.contains('brew-build')){
                        ec2_umb_notify_result_brew()
                    }
                    else{
                        ec2_umb_notify_result()
                    }
                    if (env.CERT_PARAMS){
                        rhcert_cert_attachment()
                    }
                }
                cleanWs()
            }
            failure {
                mail_notify_simple_params(MAILSENDER: DEFAULT_MAIL_SENDER, MAILRECEIVER: DEFAULT_MAIL_RECEIVER_FAIL, ATTACHLOG: false)
             }
        }
    }
}