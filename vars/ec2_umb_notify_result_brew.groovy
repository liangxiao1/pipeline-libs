// publish UMB messages to ci dashboard
def call() {
    ci = readYaml file: "job_env.yaml"
    String date = sh(script: 'date -uIs', returnStdout: true).trim()
    echo "${date}"
    def thread_id = sh(script: "echo ${ci.COMPOSE_ID} | md5sum | awk '{print \$1}'", returnStdout: true).trim()
    thread_id = thread_id + "${ci.JOB_ARCH}"
    def scratch = ''
    if ( "${ci.SCRATCH}" == 'true' ) {
        scratch = 'true'
    } else {
        scratch = 'false'
    }
    def message_content = """\
{
  "ci": {
    "name": "${ci.NAME}",
    "team": "VirtQE-S1-AWS",
    "irc": "#AzureQE", 
    "url": "${env.JENKINS_URL}",
    "email": "${ci.EMAIL}"
  },
  "run": {
    "url": "${env.BUILD_URL}", 
    "log": "${env.BUILD_URL}console",
    "debug": "${ci.HTMLURL}",
    "rebuild": "${env.BUILD_URL}rebuild"
  },
  "artifact": {
    "type": "brew-build",
    "id": "${ci.JOB_INFO_TASK_ID}",
    "issuer": "${ci.JOB_INFO_OWNER_NAME}",
    "component": "${ci.JOB_INFO_PACKAGE_NAME}",
    "nvr": "${ci.JOB_INFO_NVR}",
    "scratch": "${scratch}"
  },
  "system": {
    "os": "${ci.JOB_INFO_VOLUME_NAME}",
    "provider": "avocado-cloud",
    "architecture": "${ci.ARCH}"
  },
  "label": [
            "${ci.ARCH}"
    ],
  "type": "tier1",
  "category": "functional",
  "thread_id": "${thread_id}",
  "status": "${ci.TESTRESULT}",
  "namespace": "${ci.NAMESPACE}.brew-build",
  "generated_at": "${date}",
  "version": "0.1.0"
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