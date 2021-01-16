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
            string(name: 'ARCH', defaultValue: pipelineParams.DEFAULT_ARCH, description: 'x86_64|aarch64')
            string(name: 'PROXY_URL', defaultValue: pipelineParams.DEFAULT_PROXY_URL, description: 'proxy ip:port to access internal ip')
            string(name: 'INSTANCE_TYPES', defaultValue: pipelineParams.DEFAULT_INSTANCE_TYPES, description: 'option, specify instance types you want to test, seperate by comma')
            string(name: 'INSTANCE_NUM', defaultValue: '', description: 'option, how many instance to you want to test, default nightly compose is 1, production compose is 8')
            string(name: 'INSTANCE_DATE', defaultValue: '', description: 'option, only for new instance types available date')
            string(name: 'PKG_URL', defaultValue: '', description: 'option, Specify pkgs url you want to install')
            string(name: 'BRANCH_NAME', defaultValue: '', description: 'option, Specify branch name, eg. CentOS-Stream-8, RHEL-8.3')
            string(name: 'RUN_CASES', defaultValue: pipelineParams.DEFALUT_RUN_CASES, description: 'case tags, eg. acceptance, cloudinit, kernel_tier1 or one casename')
            choice(name: 'EC2_PROFILE', choices: ['default', pipelineParams.DEFAULT_EC2_PROFILE1, pipelineParams.DEFAULT_EC2_PROFILE2], description: 'account used for testing!')
            choice(name: 'EC2_REGION', choices: ['us-west-2', 'us-west-1', 'us-east-1','us-east-2'], description: 'which region the test run in?')
            string(name: 'EC2_SUBNET', defaultValue: pipelineParams.DEFAULT_EC2_SUBNET, description: 'which subnet the test run in?')
            string(name: 'EC2_SG_GROUP', defaultValue: pipelineParams.DEFAULT_EC2_SG_GROUP, description: 'which security group the test run in?')
            string(name: 'EC2_ADDITIONALINFO', defaultValue: '', description: 'please split it by ";" if have more items')
            text(name: 'PREVIEW_INSTANCE_TYPES', defaultValue: 'instance_types: !mux\n    xxxx.2xlarge:\n        instance_type: xxxx.2xlarge\n        cpu: 8\n        memory: 64\n        disks: 2\n        net_perf : 10\n        ipv6: True\n', description: 'Cannot get preview instances spec, so please input it manually!')
        }
        options {
            buildDiscarder(logRotator(numToKeepStr: '20'))
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
            DEFAULT_EC2_SG_GROUP="${pipelineParams.DEFAULT_EC2_SG_GROUP}"
            DEFAULT_PROXY_URL="${pipelineParams.DEFAULT_PROXY_URL}"
            DEFAULT_JSLAVE="${pipelineParams.DEFAULT_JSLAVE}"
            DEFALUT_RUN_CASES="${pipelineParams.DEFALUT_RUN_CASES}"
            DEFAULT_INSTANCE_TYPES="${pipelineParams.DEFAULT_INSTANCE_TYPES}"
            DEFAULT_ARCH="${pipelineParams.DEFAULT_ARCH}"
            KEY_NAME="${pipelineParams.KEY_NAME}"
            KEYFILE="${pipelineParams.KEYFILE}"
            DEFAULT_MAIL_SENDER="${pipelineParams.DEFAULT_MAIL_SENDER}"
            DEFAULT_MAIL_RECEIVER_SUCCESS="${pipelineParams.DEFAULT_MAIL_RECEIVER_SUCCESS}"
            DEFAULT_MAIL_RECEIVER_FAIL="${pipelineParams.DEFAULT_MAIL_RECEIVER_FAIL}"
            UMB_NAMESPACE="${pipelineParams.UMB_NAMESPACE}"
            UMB_TOPIC="${pipelineParams.UMB_TOPIC}"
            NFS_SERVER="${pipelineParams.NFS_SERVER}"
            LOG_SERVER="${pipelineParams.LOG_SERVER}"
            TESTOWNER="${pipelineParams.TESTOWNER}"
        }
        stages {
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
                    }
                }
            }
            stage("Prepare image") {
                steps {
                    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'origin/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'avocado-cloud']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://code.engineering.redhat.com/gerrit/avocado-cloud']]]
                    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'origin/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'xen-ci']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://code.engineering.redhat.com/gerrit/xen-ci']]]
                    ec2_prepare_test_ami()
                }
            }
            stage ("Run Test") {
                steps {
                    ec2_avocado_cloud_run()
                }
            }
            stage ("Save Result to Remote Server") {
                steps {
                    ec2_avocado_cloud_save_result()
                    script {
                        config = readYaml file: "job_env.yaml"
                        HTMLURL = config.HTMLURL
                    }
                }
            }
        }
        post {
            always {
                archiveArtifacts 'job_env.yaml'
            }
            success {
                ec2_mail_notify_result(MAILSENDER: DEFAULT_MAIL_SENDER, MAILRECEIVER: DEFAULT_MAIL_RECEIVER_SUCCESS)
                ec2_umb_notify_result()
                cleanWs()
            }
            failure {
                mail_notify_simple_params(MAILSENDER: DEFAULT_MAIL_SENDER, MAILRECEIVER: DEFAULT_MAIL_RECEIVER_FAIL, ATTACHLOG: false)
                cleanWs()
             }
        }
    }
}