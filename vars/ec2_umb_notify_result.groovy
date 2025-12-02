// publish UMB messages to ci dashboard
def call() {
    def ci = readYaml file: "job_env.yaml"
    String date = sh(script: 'date -uIs', returnStdout: true).trim()
    echo "${date}"
    def thread_id = sh(script: "echo ${ci.COMPOSE_ID}${ci.ARCH} | md5sum | awk '{print \$1}'", returnStdout: true).trim()
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
    "team": "VirtCloud",
    "slack": "team-virt-cloud-qe",
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
    "type": "productmd-compose",
    "id": "${ci.COMPOSE_ID}",
    "compose_type" : "gate"
  },
  "system": [
    {
    "provider": "avocado-cloud",
    "architecture": "${ci.ARCH}",
    "variant" : "fast"
    }
  ],
  "label": [
            "${ci.ARCH}"
    ],
  "type": "acceptance",
  "category": "validation",
  "thread_id": "${thread_id}",
  "status": "${ci.TESTRESULT}",
  "namespace": "${UMB_NAMESPACE}",
  "generated_at": "${date}",
  "version": "0.1.0"
}"""
    echo "${message_content}"
    return sendCIMessage (
        providerName: 'Red Hat UMB',
        overrides: [topic: "${UMB_TOPIC}"],
        messageContent: message_content,
        messageType: 'Custom',
        failOnError: true,
        messageProperties: "type=productmd-compose\n compose_id=${ci.COMPOSE_ID}"

    )
}