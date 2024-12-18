// publish UMB messages to ci dashboard
def call() {
    ci = readYaml file: "job_env.yaml"
    String date = sh(script: 'date -uIs', returnStdout: true).trim()
    echo "${date}"
    def thread_id = sh(script: "echo ${ci.COMPOSE_ID}${ci.ARCH} | md5sum | awk '{print \$1}'", returnStdout: true).trim()
    def scratch = ''
    if ( "${ci.SCRATCH}" == 'true' ) {
        scratch = true
    } else {
        scratch = false
    }
    def message_content = """
{
  "contact": {
    "name": "${ci.NAME}",
    "team": "VirtCloud",
    "slack": "team-virt-cloud-qe",
    "url": "${env.JENKINS_URL}",
    "email": "${ci.EMAIL}",
    "docs": "https://github.com/liangxiao1/pipeline-libs"
  },
  "run": {
    "url": "${env.BUILD_URL}", 
    "log": "${env.BUILD_URL}console",
    "debug": "${ci.HTMLURL}",
    "rebuild": "${env.BUILD_URL}rebuild"
  },
  "artifact": {
    "type": "brew-build",
    "id": ${ci.JOB_INFO_TASK_ID},
    "issuer": "${ci.JOB_INFO_OWNER_NAME}",
    "component": "${ci.JOB_INFO_PACKAGE_NAME}",
    "nvr": "${ci.JOB_INFO_NVR}",
    "scratch": ${scratch}
  },
  "system": [
    {
    "os": "${ci.JOB_INFO_VOLUME_NAME}",
    "provider": "os-tests",
    "architecture": "${ci.ARCH}"
    }
  ],
  "test": {
    "type": "tier1",
    "category": "functional",
    "result": "${ci.UMB_TESTRESULT}",
    "namespace": "${UMB_NAMESPACE}",
    "docs": "https://github.com/virt-s1/os-tests"
  },
  "pipeline": {
      "id": "${thread_id}",
      "name": "tier1-gating"
  },
  "generated_at": "${date}",
  "version": "1.1.14"
}"""
    echo "${message_content}"
    return sendCIMessage (
        providerName: 'Red Hat UMB',
        overrides: [topic: "${UMB_TOPIC}"],
        messageContent: message_content,
        messageType: 'Custom',
        failOnError: true
    )
}